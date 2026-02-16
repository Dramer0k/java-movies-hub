package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    private static final int MIN_YEAR = 1888;
    private static final int MAX_YEAR = LocalDate.now().getYear() + 1;
    private static final int MAX_MOVIE_TITLE_LENGTH = 100;
    private final MoviesStore moviesStore;
    private List<Movie> movieList;
    private final ErrorResponse error = new ErrorResponse();
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        movieList = new ArrayList<>(moviesStore.getMoviesMap().values());
        String body = gson.toJson(movieList);
        String method = ex.getRequestMethod();

        switch (method) {
            case "GET" -> handleGetRequest(ex, body);
            case "POST" -> handlePostRequest(ex);
            case "DELETE" -> handleDeleteRequest(ex);
            default -> handleDefaultRequest(ex);
        }
    }

    public void handleGetRequest(HttpExchange ex, String body) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();
        String[] array = path.split("/");

        try {
            if (array.length == 2) {
                if (query != null && query.contains("year=")) {
                    String yearStr = query.split("=")[1];

                    int year = Integer.parseInt(yearStr);
                    List<Movie> filteredMovies = movieList.stream()
                        .filter(movie -> movie.getYear() == year)
                        .toList();
                    String json = gson.toJson(filteredMovies);
                    sendJson(ex, 200, json);
                    return;
                }
                sendJson(ex, 200, body);
            } else if (array.length == 3) {
                int id = Integer.parseInt(array[2]);
                if (moviesStore.getMoviesMap().size() > id) {
                    String json = gson.toJson(moviesStore.getMovie(id));
                    sendJson(ex, 200, json);
                } else {
                    error.addDetails("Фильм не найден");
                    String json = gson.toJson(error.getDetails());
                    error.clearMap();
                    sendJson(ex, 404, json);
                }
            }
        } catch (NumberFormatException e) {
            if (query != null && query.contains("year=")) {
                error.addDetails("Некорректный параметр запроса — 'year'");
            } else {
                error.addDetails("Некорректный ID");
            }
            String json = gson.toJson(error.getDetails());
            error.clearMap();
            sendJson(ex, 400, json);
        }
    }

    public void handlePostRequest(HttpExchange ex) throws IOException {
        Movie movie;
        try (InputStreamReader isr = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8)) {
            movie = gson.fromJson(isr, Movie.class);
        } catch (IOException e) {
            error.addDetails("Что-то пошло не так");
            String errorMessage = gson.toJson(error.getDetails());
            sendJson(ex, 422, errorMessage);
            return;
        } catch (JsonSyntaxException e) {
            throw new RuntimeException(e);
        }

        if (!checkCT(ex)) {
            error.addDetails("Неправильное значение заголовка Content-Type");
            String json = gson.toJson(error.getDetails());
            error.clearMap();
            sendJson(ex, 415, json);
            return;
       }

        if (!validateMovie(movie)) {
            String errorMessage = gson.toJson(error.getErrorMap());
            error.clearMap();
            sendJson(ex, 422, errorMessage);
            return;
        }

        int id = moviesStore.getMoviesMap().size();
        if (moviesStore.checkMovie(movie.getTitle())) {
            id = moviesStore.getMovieId(movie.getTitle());
        }

        HashMap<Integer, Movie> map = new HashMap<>();
        map.put(id, movie);

        moviesStore.addMovie(movie);
        String jsonMovie = gson.toJson(map);
        sendJson(ex, 201, jsonMovie);
    }

    public void handleDeleteRequest(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String[] array = path.split("/");

        try {
            int id = Integer.parseInt(array[2]);
            if (moviesStore.getMoviesMap().containsKey(id)) {
                moviesStore.deleteMovie(id);
                sendNoContent(ex);
            } else {
                error.addDetails("Фильм не найден");
                String json = gson.toJson(error.getDetails());
                error.clearMap();
                sendJson(ex, 404, json);
            }
        } catch (NumberFormatException e) {
            error.addDetails("Некорректный ID");
            String json = gson.toJson(error.getDetails());
            error.clearMap();
            sendJson(ex, 400, json);
        }
    }

    public void handleDefaultRequest(HttpExchange ex) throws IOException {
        error.addDetails("Принимаю только Get и POST запросы!");
        String json = gson.toJson(error.getDetails());
        error.clearMap();
        sendJson(ex, 405, json);
    }

    public boolean validateMovie(Movie movie) {
        if (movie.getTitle().isBlank() || movie.getTitle().length() > MAX_MOVIE_TITLE_LENGTH ||
                movie.getYear() <= MIN_YEAR || movie.getYear() >= MAX_YEAR) {
            error.setError("Ошибка валидации!");

            if (movie.getTitle().isBlank()) {
                error.addDetails("Название не должно быть пустым!");
            }

            if (movie.getTitle().length() > MAX_MOVIE_TITLE_LENGTH) {
                error.addDetails("Длина тела должна быть <= 100");
            }

            if (movie.getYear() <= MIN_YEAR || movie.getYear() >= MAX_YEAR) {
                error.addDetails("Год должен быть между 1888 и 2026!");
            }
            return false;
        }
        return true;
    }

    public boolean checkCT(HttpExchange ex) {
        return ex.getRequestHeaders().getFirst("Content-Type")
                .equalsIgnoreCase("application/json");
    }

}


