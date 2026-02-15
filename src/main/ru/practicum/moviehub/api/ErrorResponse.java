package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorResponse {
    private String error;
    private List<String> details = new ArrayList<>();
    private Map<String, List<String>> errorMap = new HashMap<>();

    public void clearMap() {
        errorMap.clear();
        details.clear();
    }

    public void setError(String error) {
        this.error = error;
    }

    public void addDetails(String str) {
        details.add(str);
    }

    public List<String> getDetails() {
        return details;
    }

    public Map<String, List<String>> getErrorMap() {
        errorMap.put(error, details);
        return errorMap;
    }
}