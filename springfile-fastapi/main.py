import io
import asyncio # Import asyncio for sleep
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import StreamingResponse
from docx import Document
from docx.shared import Pt

app = FastAPI()

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

if __name__ == "__main__":
    import uvicorn
    # Run with: uvicorn main:app --reload --port 8001
    # (Using a different port like 8001 to avoid conflict with potential Spring Boot default 8080)
    uvicorn.run(app, host="0.0.0.0", port=8001)
