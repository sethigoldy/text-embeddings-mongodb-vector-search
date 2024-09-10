package com.example;

public class EmbeddingInstance {
    private String task_type = "RETRIEVAL_QUERY";
    private String content;

    public EmbeddingInstance(String content) {
        this.content = content;
    }

    public String getTask_type() {
        return task_type;
    }

    public void setTask_type(String task_type) {
        this.task_type = task_type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}