package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingPrediction {
    private Embedding embeddings;

    // Getters and setters
    public Embedding getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(Embedding embeddings) {
        this.embeddings = embeddings;
    }
}