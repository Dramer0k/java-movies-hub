package ru.practicum.moviehub.http;

import java.net.URI;
import java.net.http.HttpRequest;

import static ru.practicum.moviehub.http.MoviesServer.MOVIES;

public abstract class BaseApiTestMethod {
    private static final String BASE = "http://localhost:8080";

    public HttpRequest postRequest_movies(String json, String contentType) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    public HttpRequest getRequest(String endpoint) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE + endpoint))
                .GET()
                .build();
    }

    public HttpRequest deleteRequest(String endpoint) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE + endpoint))
                .DELETE()
                .build();
    }
}
