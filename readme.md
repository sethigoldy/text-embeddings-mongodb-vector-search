# Text Embedding Processor

The **Text Embedding Processor** is a Java-based application that retrieves text embeddings using the Google Vertex AI API. It processes documents from MongoDB, generates embeddings for text fields (like `description`, `labels`, and `type`), and stores these embeddings back into the database. This app is designed for large-scale data processing and handling failures with retries.

## Preparing your Data

Dataset used in this embedding is wikidata (Download and import using the torrent) - <https://academictorrents.com/details/0852ef544a4694995fcbef7132477c688ded7d9a>

Unzip the data after its been downloaded on linux using pigz (fast compared to normal gunzip):

```
sudo apt-get install pigz
pigz -d wikidata-20240101-all.json.gz
```

Use mongoimport to import the data to MongoDB using the below command:

```
mongoimport --uri="<YOUR_MONGODB_SRV_URL_WITH_CREDENTIALS>/" --db="wikidata" --collection="text" --ssl --type=json --jsonArray --numInsertionWorkers=100 wikidata-20240101-all.json
```

## Table of Contents
- [Components](#components)
- [Features](#features)
- [Setup Instructions](#setup-instructions)
- [Running the Application](#running-the-application)
- [Sample Usage](#sample-usage)
- [Troubleshooting](#troubleshooting)

## Components

### 1. **TextEmbeddingProcessor**
   - **Description**: Main class that processes documents from MongoDB, generates embeddings using the Google Vertex AI API, and stores them back.
   - **Functions**:
     - Fetch documents from MongoDB.
     - Check if embeddings are already present. If not, generate embeddings.
     - Handle API failures with retries.

### 2. **Embedding**
   - **Description**: Represents the embedding response from the Google Vertex AI API. Contains the embedding values.
   - **Fields**:
     - `values`: A `List<Double>` representing the embedding vector.

### 3. **EmbeddingPrediction**
   - **Description**: Represents the prediction in the response from the Google Vertex AI API.
   - **Fields**:
     - `embeddings`: An `Embedding` object containing the actual embedding values.

### 4. **EmbeddingResponse**
   - **Description**: Represents the overall response from the Google Vertex AI API. Contains multiple `EmbeddingPrediction` objects.

### 5. **MongoDBConnector**
   - **Description**: Handles connecting to the MongoDB instance and fetching/updating documents.

## Features
- Connects to MongoDB and processes documents.
- Fetches embeddings for specific fields from the Google Vertex AI API.
- Combines values from fields like `description.en.value`, `labels.en.value`, and `type` and generates a dynamic string for embedding.
- Stores embeddings back into MongoDB.
- Automatic retries in case of API failure.
  
## Setup Instructions

### Prerequisites
- Java 11 or later.
- Maven for dependency management.
- Google Cloud project with Vertex AI API enabled.
- MongoDB instance with relevant documents.

### 1. Clone the Repository

```bash
git clone https://github.com/your-repo/text-embedding-processor.git
cd text-embedding-processor
```

### 2. Setup Google Cloud Credentials
Ensure that the Google Cloud SDK is installed, and you are authenticated to the project where the Vertex AI API is enabled.

```bash
gcloud auth login
gcloud config set project <your-google-cloud-project-id>
```

### 3. Configure MongoDB
In `application.properties`, set your MongoDB connection string and database details:

```properties
mongodb.uri=mongodb://<username>:<password>@<host>:<port>/<database>
mongodb.collection=<your-collection-name>
```

### 4. Install Dependencies

Navigate to the root directory of the project and run:

```bash
mvn clean install
```

### 5. Set Environment Variables
For accessing Google Cloud Vertex AI, export the following environment variable to set the path to your service account credentials file:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your-service-account-file.json"
```

## Running the Application

After setting up the project and configuration, you can run the application using Maven.

### Command to Run:
```bash
mvn exec:java -Dexec.mainClass="com.example.TextEmbeddingProcessor"
```

This command will:
- Fetch documents from the MongoDB collection.
- Generate embeddings for the required text fields.
- Store the generated embeddings back into MongoDB.

## Sample Usage

Here's how the system works:
1. The application will pull documents from MongoDB.
2. For each document:
   - The fields `description.en.value`, `labels.en.value`, and `type` are concatenated with a `|` separator to form the content for embedding.
   - The concatenated content is sent to the Google Vertex AI API to generate embeddings.
   - The generated embeddings are stored back into the MongoDB document.
3. If there are errors or failures, the system will retry the request.

### Example Document Fields

- **description.en.value**: "This is a product description."
- **labels.en.value**: "Product, Example"
- **type**: "Clothing"

This will generate the following content for embeddings:

```
"This is a product description. | Product, Example | Clothing"
```

## Troubleshooting

- **ClassNotFoundException**: Ensure that the correct class name is specified and that you've built the project with `mvn clean install`.
- **MongoDB connection errors**: Ensure your MongoDB URI is correct and that MongoDB is running.
- **Google API errors**: Make sure that the Google Vertex AI API is enabled and the authentication credentials are correct.
  
For further help, check the logs for detailed error messages or run the app in debug mode using:

```bash
mvn exec:java -Dexec.mainClass="com.example.TextEmbeddingProcessor" -X
```