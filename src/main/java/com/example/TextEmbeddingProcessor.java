package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TextEmbeddingProcessor {

    private static final String CLUSTER_URL = "<YOUR_MONGODB_SRV_URL_WITH_CREDENTIALS>";
    private static final String DATABASE_NAME = "wikidata";
    private static final String COLLECTION_NAME = "text";
    private static final int THREAD_POOL_SIZE = 300;
    private static final int MAX_RETRIES = 3;
    private static final String GOOGLE_API_URL = "https://us-central1-aiplatform.googleapis.com/v1/projects/PROJECT_ID/locations/us-central1/publishers/google/models/text-embedding-004:predict";

    public static void main(String[] args) {
        MongoClient mongoClient = MongoClients.create(CLUSTER_URL);
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (MongoCursor<Document> cursor = collection.find(Filters.eq("embeddings", null)).iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                executor.submit(() -> processDocument(document, collection, 0));
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        mongoClient.close();
    }

    public static double[] convertEmbeddingPredictionsToDoubleArray(List<EmbeddingPrediction> predictions) {
        // Create a list to store all the values as a flat structure
        List<Double> flattenedValues = new ArrayList<>();

        // Loop through each prediction and extract its values
        for (EmbeddingPrediction prediction : predictions) {
            List<Double> embeddingValues = prediction.getEmbeddings().getValues();
            if (embeddingValues != null) {
                flattenedValues.addAll(embeddingValues);
            }
        }

        // Convert the List<Double> to double[]
        double[] result = new double[flattenedValues.size()];
        for (int i = 0; i < flattenedValues.size(); i++) {
            result[i] = flattenedValues.get(i);
        }

        return result;
    }

    private static void processDocument(Document document, MongoCollection<Document> collection, int retryCount) {
        // Extract "description.en.value", "labels.en.value", and "type" fields
        String description = extractField(document, "description.en.value");
        String labels = extractField(document, "labels.en.value");
        String type = extractField(document, "type");

        // Construct the dynamic content by concatenating the fields with "|"
        String dynamicValue = String.join("|", description, labels, type);

        if (!dynamicValue.isEmpty()) {
            try {
                List<EmbeddingPrediction> embeddings = generateEmbeddingsFromGoogleAPI(dynamicValue);
                double[] embeddingsArray = convertEmbeddingPredictionsToDoubleArray(embeddings);

                Bson filter = Filters.and(
                        Filters.eq("_id", document.getObjectId("_id")),
                        Filters.eq("embeddings", null)
                );
                Bson update = Updates.set("embeddings", embeddingsArray);
                collection.updateOne(filter, update);

                System.out.println("Updated document with _id: " + document.getObjectId("_id"));

            } catch (MongoException e) {
                if (retryCount < MAX_RETRIES) {
                    System.err.println("Error updating document with _id: " + document.getObjectId("_id") + ". Retrying... Attempt " + (retryCount + 1));
                    processDocument(document, collection, retryCount + 1);
                } else {
                    System.err.println("Failed to update document with _id: " + document.getObjectId("_id") + " after " + MAX_RETRIES + " attempts.");
                }
            } catch (Exception e) {
                System.err.println("General error while processing document with _id: " + document.getObjectId("_id"));
                e.printStackTrace();
            }
        }
    }

    private static String extractField(Document document, String fieldPath) {
        Object value = document;
        for (String field : fieldPath.split("\\.")) {
            if (value instanceof Document) {
                value = ((Document) value).get(field);
            } else {
                return "";  // If any field in the path is missing, return empty string
            }
        }
        return value != null ? value.toString() : "";
    }

    private static List<EmbeddingPrediction> generateEmbeddingsFromGoogleAPI(String dynamicValue) throws IOException {
        String apiUrl = GOOGLE_API_URL.replace("PROJECT_ID", "<YOUR_GOOGLE_PROJECT_ID>");

        URL url = new URL(apiUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);

        // Step 1: Create request object with dynamic content
        EmbeddingRequest request = new EmbeddingRequest(new EmbeddingInstance(dynamicValue));

        // Step 2: Serialize request to JSON and send
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(request);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Step 3: Get response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            // Step 4: Deserialize response into a Java object
            EmbeddingResponse embeddingResponse = objectMapper.readValue(response.toString(), EmbeddingResponse.class);

            // Return the embeddings
            return embeddingResponse.getPredictions();
        }
    }

    private static String getAccessToken() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("gcloud", "auth", "print-access-token");
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.readLine();
        }
    }
}