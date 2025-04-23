# Use an official Python runtime as a parent image
FROM python:3.10-slim

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONUNBUFFERED 1

# Set the working directory in the container
WORKDIR /app

# Install system dependencies if needed (e.g., for certain ML libraries)
# RUN apt-get update && apt-get install -y --no-install-recommends some-package && rm -rf /var/lib/apt/lists/*

# Copy the requirements file into the container at /app
COPY requirements.txt .

# Install any needed packages specified in requirements.txt
# Using --no-cache-dir reduces image size
RUN pip install --no-cache-dir --upgrade pip
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application code into the container at /app
COPY . .

# Make port 8080 available to the world outside this container
# Cloud Run will set the PORT environment variable, which uvicorn will use.
EXPOSE 8080

# Define the command to run the application using Uvicorn.
# It will listen on the port specified by the PORT environment variable.
# Using $PORT ensures Cloud Run compatibility. Defaulting to 8001 locally if PORT isn't set.
# Note: Using shell form to allow environment variable substitution for PORT.
CMD uvicorn main:app --host 0.0.0.0 --port ${PORT:-8001}
