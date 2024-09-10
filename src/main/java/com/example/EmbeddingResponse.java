package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)  // This will ignore the `statistics` field
public class EmbeddingResponse {
    private List<EmbeddingPrediction> predictions;

    // Getters and setters
    public List<EmbeddingPrediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<EmbeddingPrediction> predictions) {
        this.predictions = predictions;
    }
}
