package com.example;

import java.util.Collections;
import java.util.List;

public class EmbeddingRequest {
    private List<EmbeddingInstance> instances;
    private EmbeddingParameters parameters;

    public EmbeddingRequest(EmbeddingInstance instance) {
        this.instances = Collections.singletonList(instance);
        this.parameters = new EmbeddingParameters(256);  // Setting outputDimensionality to 256
    }

    public List<EmbeddingInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<EmbeddingInstance> instances) {
        this.instances = instances;
    }

    public EmbeddingParameters getParameters() {
        return parameters;
    }

    public void setParameters(EmbeddingParameters parameters) {
        this.parameters = parameters;
    }
}