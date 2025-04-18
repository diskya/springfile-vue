import io
import asyncio # Import asyncio for sleep
import io
import asyncio # Import asyncio for sleep
import os
from typing import List # For type hinting

from fastapi import FastAPI, File, UploadFile, HTTPException, status # Added status
from fastapi.responses import StreamingResponse, JSONResponse # Added JSONResponse
from pydantic import BaseModel # For request body validation
from docx import Document
from docx.shared import Pt

# Langchain specific imports
from langchain_community.document_loaders import UnstructuredWordDocumentLoader, UnstructuredPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.embeddings import HuggingFaceBgeEmbeddings
import chromadb # Import ChromaDB client

app = FastAPI()

# --- Configuration ---
ALLOWED_EXTENSIONS = {".docx", ".pdf"}
UPLOADS_DIR = "../springfile-backend/uploads/" # Relative path to Spring Boot uploads
MODEL_NAME = "BAAI/bge-small-zh-v1.5"
MODEL_KWARGS = {'device': 'cpu'} # Use CPU. Change to 'cuda' for GPU if available and configured.
ENCODE_KWARGS = {'normalize_embeddings': True} # Or False, depending on use case
CHROMA_DB_PATH = "./chroma_db" # Directory to store ChromaDB data
COLLECTION_NAME = "document_embeddings"

# --- Initialize Embedding Model (globally) ---
# This might take time on first run as the model downloads
try:
    print(f"Initializing BGE embedding model: {MODEL_NAME}...")
    embedding_model = HuggingFaceBgeEmbeddings(
        model_name=MODEL_NAME,
        model_kwargs=MODEL_KWARGS,
        encode_kwargs=ENCODE_KWARGS
    )
    print("BGE embedding model initialized successfully.")
except Exception as e:
    print(f"FATAL: Failed to initialize embedding model: {e}")
    embedding_model = None # Indicate failure

# --- Initialize ChromaDB Client and Collection ---
try:
    print(f"Initializing ChromaDB client at path: {CHROMA_DB_PATH}...")
    chroma_client = chromadb.PersistentClient(path=CHROMA_DB_PATH)
    # Get or create the collection
    collection = chroma_client.get_or_create_collection(
        name=COLLECTION_NAME,
        # Optional: Specify embedding function if you want Chroma to handle it,
        # but we are providing embeddings manually here.
        # metadata={"hnsw:space": "cosine"} # Example metadata, adjust as needed
    )
    print(f"ChromaDB collection '{COLLECTION_NAME}' ready.")
except Exception as e:
    print(f"FATAL: Failed to initialize ChromaDB: {e}")
    chroma_client = None
    collection = None

# --- Initialize Text Splitter ---
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=50,
)

# --- Pydantic Models for Input ---
class FilePathInput(BaseModel):
    file_path: str # e.g., "some_uuid.docx" or "some_uuid.pdf"

class SearchQueryInput(BaseModel):
    query: str # The text query for similarity search
    n_results: int = 5 # Number of results to return, default 5

@app.post("/preprocess/docx/")
async def preprocess_docx(file: UploadFile = File(...)):
    """
    Accepts a DOCX file, adds "hello world" to the beginning,
    and returns the modified DOCX file.
    """
    if not file.filename.endswith('.docx'):
        raise HTTPException(status_code=400, detail="Invalid file type. Only .docx files are accepted.")

    try:
        # --- Add 3-second delay for testing ---
        print("Processing started, waiting 3 seconds...")
        await asyncio.sleep(3)
        print("Delay finished, continuing processing.")
        # --------------------------------------

        # Read the uploaded file content into memory
        content = await file.read()
        file_stream = io.BytesIO(content)

        # Load the document using python-docx
        document = Document(file_stream)

        # Create a new paragraph with "hello world"
        # Insert it at the very beginning of the document body
        paragraph = document.add_paragraph()
        run = paragraph.add_run("hello world")
        run.font.size = Pt(12) # Optional: set font size

        # Move the new paragraph to the beginning
        # The Document object's body element contains paragraphs and tables.
        # We access the underlying XML element (body) and insert the new paragraph's XML element (p) at the start.
        body = document.element.body
        body.insert(0, paragraph._p) # _p gives the underlying lxml element

        # Save the modified document to a BytesIO stream
        output_stream = io.BytesIO()
        document.save(output_stream)
        output_stream.seek(0) # Reset stream position to the beginning

        # Return the modified file as a streaming response
        return StreamingResponse(
            output_stream,
            media_type='application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            headers={'Content-Disposition': f'attachment; filename="processed_{file.filename}"'}
        )

    except Exception as e:
        # Log the error for debugging
        print(f"Error processing file {file.filename}: {e}")
        raise HTTPException(status_code=500, detail=f"Could not process file: {e}")

@app.post("/embedding/")
async def embedding(payload: FilePathInput):
    """
    Accepts a relative file path (within the Spring Boot uploads dir),
    loads the corresponding DOCX or PDF file, splits text,
    generates embeddings using the configured BGE model,
    stores them in ChromaDB, overwriting existing entries for the same file,
    and returns a success message.
    """
    if embedding_model is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Embedding model is not available.")
    if collection is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Vector database (ChromaDB) is not available.")

    # Removed sleep for production readiness
    # await asyncio.sleep(3)

    relative_path = payload.file_path
    print(f"Received request to embed file: {relative_path}")

    # --- File Path Construction and Validation ---
    full_path = os.path.abspath(os.path.join(UPLOADS_DIR, relative_path))
    print(f"Constructed full path: {full_path}")

    # Security check: Ensure the path stays within the intended uploads directory
    if not full_path.startswith(os.path.abspath(UPLOADS_DIR)):
        print(f"Security Alert: Attempt to access path outside uploads directory: {relative_path}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid file path specified."
        )

    if not os.path.exists(full_path):
        print(f"File not found at path: {full_path}")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"File not found at specified path: {relative_path}"
        )

    _, file_extension = os.path.splitext(relative_path)
    file_extension = file_extension.lower()

    if file_extension not in ALLOWED_EXTENSIONS:
        print(f"Invalid file type for embedding: {file_extension}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid file type. Allowed types: {', '.join(ALLOWED_EXTENSIONS)}"
        )

    try:
        # --- Document Loading & Text Extraction ---
        all_text = ""
        print(f"Processing file extension: {file_extension}")
        if file_extension == ".docx":
            print(f"Loading content using python-docx from {full_path}")
            try:
                document = Document(full_path)
                all_text = "\n\n".join([para.text for para in document.paragraphs])
                print(f"Successfully extracted text from DOCX (length: {len(all_text)}).")
            except Exception as e:
                print(f"Error processing DOCX file content with python-docx: {e}")
                raise HTTPException(
                    status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                    detail=f"Error processing DOCX file content: {e}"
                )
        elif file_extension == ".pdf":
            loader = UnstructuredPDFLoader(full_path)
            print(f"Loading content using {type(loader).__name__} from {full_path}")
            try:
                # Note: Still using loader.load() here for PDF. If PDFs also hang,
                # this might need asyncio.to_thread or a different PDF library.
                documents = loader.load()
                all_text = "\n\n".join([doc.page_content for doc in documents])
                print(f"Successfully loaded {len(documents)} PDF document parts (total text length: {len(all_text)}).")
            except Exception as e:
                print(f"Error processing file content with Langchain PDF loader: {e}")
                raise HTTPException(
                    status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                    detail=f"Error processing PDF file content: {e}"
                )
        else:
             # Should not happen due to earlier check
             print(f"Internal error: File type processing failed for extension {file_extension}")
             raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Internal error: File type processing failed.")

        # --- Text Splitting ---
        if not all_text.strip():
             print("Warning: Loaded document content is empty or whitespace only.")
             return JSONResponse(content={"filename": relative_path, "embeddings": []})

        print(f"Splitting text (total length: {len(all_text)})...")
        chunks = text_splitter.split_text(all_text)
        print(f"Split text into {len(chunks)} chunks.")

        if not chunks:
             print("Warning: Text splitting resulted in zero chunks.")
             return JSONResponse(content={"filename": relative_path, "embeddings": []})

        # --- Embedding Generation ---
        print(f"Generating embeddings for {len(chunks)} chunks using {MODEL_NAME}...")
        try:
            embeddings: List[List[float]] = embedding_model.embed_documents(chunks)
            print(f"Successfully generated {len(embeddings)} embeddings (dimension: {len(embeddings[0]) if embeddings else 'N/A'}).")
        except Exception as e:
            print(f"Error generating embeddings: {e}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to generate embeddings: {e}"
            )

        # --- Store Embeddings in ChromaDB ---
        if embeddings:
            # Create unique IDs for each chunk based on filename and index
            ids = [f"{relative_path}_{i}" for i in range(len(chunks))]
            # Create metadata (optional, but useful)
            metadatas = [{"source": relative_path, "chunk_index": i} for i in range(len(chunks))]

            try:
                # 1. Delete existing embeddings for this file
                print(f"Deleting existing embeddings for file: {relative_path}...")
                # Query for existing IDs associated with this file
                existing_docs = collection.get(where={"source": relative_path}, include=[]) # Only need IDs
                if existing_docs and existing_docs['ids']:
                    print(f"Found {len(existing_docs['ids'])} existing embeddings to delete.")
                    collection.delete(ids=existing_docs['ids'])
                    print("Existing embeddings deleted.")
                else:
                    print("No existing embeddings found for this file.")

                # 2. Add new embeddings
                print(f"Adding {len(chunks)} new embeddings to ChromaDB collection '{COLLECTION_NAME}'...")
                collection.add(
                    embeddings=embeddings,
                    documents=chunks, # Store the text chunk itself
                    metadatas=metadatas,
                    ids=ids
                )
                print("New embeddings added successfully.")

            except Exception as e:
                print(f"Error interacting with ChromaDB: {e}")
                raise HTTPException(
                    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    detail=f"Failed to store embeddings in vector database: {e}"
                )
        else:
            print("No embeddings generated, skipping database storage.")


        # --- Response ---
        # Return success message instead of embeddings
        return JSONResponse(
            status_code=status.HTTP_200_OK,
            content={
                "message": f"Successfully processed and stored embeddings for file: {relative_path}",
                "filename": relative_path,
                "chunks_processed": len(chunks),
                "embeddings_stored": len(embeddings) if embeddings else 0
            }
        )

    except HTTPException as e:
        # Re-raise HTTPExceptions
        print(f"HTTPException occurred: {e.status_code} - {e.detail}")
        raise e
    except Exception as e:
        # Catch other unexpected errors
        print(f"An unexpected error occurred during embedding: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"An unexpected error occurred: {e}"
        )

@app.post("/search/")
async def search_embeddings(payload: SearchQueryInput):
    """
    Accepts a text query, generates its embedding, and searches the ChromaDB
    collection for the most similar document chunks.
    Returns the top N results (default 5).
    """
    if embedding_model is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Embedding model is not available.")
    if collection is None:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Vector database (ChromaDB) is not available.")

    query_text = payload.query
    n_results = payload.n_results
    print(f"Received search query: '{query_text}', requesting {n_results} results.")

    if not query_text:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Query text cannot be empty.")
    if not 1 <= n_results <= 50: # Add reasonable limit
         raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Number of results must be between 1 and 50.")

    try:
        # --- Generate Query Embedding ---
        print(f"Generating embedding for query: '{query_text}'...")
        query_embedding = embedding_model.embed_query(query_text)
        print("Query embedding generated successfully.")

        # --- Query ChromaDB ---
        print(f"Querying ChromaDB collection '{COLLECTION_NAME}' for {n_results} similar documents...")
        results = collection.query(
            query_embeddings=[query_embedding], # Note: query expects a list of embeddings
            n_results=n_results,
            include=['documents', 'metadatas', 'distances'] # Include relevant info
        )
        print(f"ChromaDB query completed. Found results: {results}")

        # --- Format and Return Results ---
        # Results structure example:
        # {'ids': [['id1', 'id2']], 'distances': [[0.1, 0.2]], 'metadatas': [[{'source': 'file1'}, {'source': 'file2'}]], 'embeddings': None, 'documents': [['chunk1 text', 'chunk2 text']]}
        # We need to extract the relevant parts for our response.
        # Since we query with one embedding, we access the first element of each list.
        formatted_results = []
        if results and results.get('ids') and results['ids'][0]:
            ids = results['ids'][0]
            distances = results['distances'][0]
            metadatas = results['metadatas'][0]
            documents = results['documents'][0]

            for i in range(len(ids)):
                formatted_results.append({
                    "id": ids[i],
                    "distance": distances[i],
                    "metadata": metadatas[i],
                    "document": documents[i]
                })

        return JSONResponse(
            status_code=status.HTTP_200_OK,
            content={
                "query": query_text,
                "results": formatted_results
            }
        )

    except HTTPException as e:
        # Re-raise HTTPExceptions
        print(f"HTTPException occurred during search: {e.status_code} - {e.detail}")
        raise e
    except Exception as e:
        # Catch other unexpected errors
        print(f"An unexpected error occurred during search: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"An unexpected error occurred during search: {e}"
        )


if __name__ == "__main__":
    import uvicorn
    # Run with: uvicorn main:app --reload --port 8001
    # Includes endpoints: /preprocess/docx/, /embedding/, /search/
    print("Starting Uvicorn server...")
    uvicorn.run(app, host="0.0.0.0", port=8001)
