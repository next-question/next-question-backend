package com.buildup.nextQuestion.dto.gpt;

import java.util.Map;

public class FunctionSpec {

    private String name;
    private String description;
    private Map<String, Object> parameters;
    private Map<String, String> function_call;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getFunction_call() {
        return function_call;
    }

    public void setFunction_call(Map<String, String> function_call) {
        this.function_call = function_call;
    }
}