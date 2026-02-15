package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.practicum.moviehub.http.MoviesServer.MOVIES;

public class MoviesApiTest extends BaseApiTestMethod {
    private static final String CT_JSON = "application/json; charset=UTF-8";
    private static final HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    private static MoviesServer server;
    private static HttpClient client;
    private static Movie correctMovie = new Movie("Волколак", 1994);
    private static Movie incorrectMovie = new Movie("Волколак", 2036);
    private final Gson gson = new Gson();
    private final ErrorResponse error = new ErrorResponse();
    private static MoviesStore store = new MoviesStore();

    @BeforeAll
    static void beforeAll() throws IOException {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        server = new MoviesServer(store, 8080);
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
        error.clearMap();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    @DisplayName("Получить в ответе массив данных")
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception   {
        HttpRequest request = getRequest(MOVIES);
        HttpResponse<String> response = client.send(request, bodyHandler);
        assertEquals(200, response.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен возвращать формат данных и кодировку");

        assertEquals(CT_JSON, response.headers().firstValue("Content-type").get());

        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается пустой JSON-массив");
    }

    @Test
    @DisplayName("Добавить фильм в корректном формате")
    void postMovies_CorrectRequest() throws IOException, InterruptedException {
        String jsonString = gson.toJson(correctMovie);
        HttpRequest request = postRequest_movies(jsonString, "application/json");
        HttpResponse<String> response = client.send(request, bodyHandler);

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен возвращать формат данных и кодировку");

        assertEquals(201, response.statusCode(), "Успешное добавление - код должен быть 201");
    }

    @Test
    @DisplayName("Добавить фильм (Ошибка валидации)")
    void postMovies_UnprocessableEntity() throws IOException, InterruptedException {
        String json = gson.toJson(incorrectMovie);
        HttpRequest request = postRequest_movies(json, "application/json");
        HttpResponse<String> response = client.send(request, bodyHandler);

        assertEquals(422, response.statusCode(), "Ошибка валидации - код должен быть 422!");
    }

    @Test
    @DisplayName("Отправить запрос с некорректным типом")
    void postMovies_UnsupportedMediaType() throws IOException, InterruptedException {
        String jsonString = gson.toJson(correctMovie);
        HttpRequest request = postRequest_movies(jsonString, "text");
        HttpResponse<String> response = client.send(request, bodyHandler);

        assertEquals(415, response.statusCode(), "Неправильное значение заголовка - код должен быть 415!");
    }

    @Test
    @DisplayName("Успешный поиск фильма по id")
    void getMoviesId_NotFound_BadRequest() throws IOException, InterruptedException {
        store.addMovie(correctMovie);
        HttpRequest request = getRequest(MOVIES + "0");
        HttpResponse<String> response = client.send(request, bodyHandler);

        assertEquals(200, response.statusCode(), "Фильм найден - код должен быть 200");
        assertEquals(CT_JSON, response.headers().firstValue("Content-type").get());
    }

    @Test
    @DisplayName("Фильм по id в базе не найден")
    void getMoviesId_NotFound() throws IOException, InterruptedException {
        HttpRequest request = getRequest(MOVIES + "/14");
        HttpResponse<String> response = client.send(request, bodyHandler);

        assertEquals(404, response.statusCode(), "Фильм не найдет - код должен быть 404");
    }

    @Test
    @DisplayName("Введен некорректный id при поиске фильма")
    void getMoviesId_BadRequest() throws IOException, InterruptedException {
        HttpRequest requestBadRequest = getRequest(MOVIES + "/qwerty");
        HttpResponse<String> responseBadRequest = client.send(requestBadRequest, bodyHandler);

        assertEquals(400, responseBadRequest.statusCode(), "Некорректный id - код должен быть 400");
    }

    @Test
    @DisplayName("Успешное удаление фильма")
    void deleteMovie_CorrectRequest() throws IOException, InterruptedException {
        store.addMovie(correctMovie);
        HttpRequest request = deleteRequest(MOVIES + "/0");
        HttpResponse<String> response = client.send(request, bodyHandler);

        assertEquals(204, response.statusCode(), "Фильм удален - код должен быть 204");
        assertEquals(CT_JSON, response.headers().firstValue("Content-type").get());
    }

    @Test
    @DisplayName("Фильм не был найден")
    void deleteMovie_NoContent() throws IOException, InterruptedException {
        HttpRequest requestNotFound = deleteRequest(MOVIES + "/14");
        HttpResponse<String> responseNotFound = client.send(requestNotFound, bodyHandler);

        assertEquals(404, responseNotFound.statusCode(), "Фильм не найден - код должен быть 404");
    }

    @Test
    @DisplayName("Введен неверный id при попытке удалить фильм")
    void deleteMovie_NotFound() throws IOException, InterruptedException {
        HttpRequest requestBadRequest = deleteRequest(MOVIES + "/qwerty");
        HttpResponse<String> responseBadRequest = client.send(requestBadRequest, bodyHandler);

        assertEquals(400, responseBadRequest.statusCode(), "Неверный id - код должен быть 400");
    }

    @Test
    @DisplayName("Получить список фильмов по году")
    void getMoviesForYear_CorrectRequest() throws IOException, InterruptedException {
        store.addMovie(correctMovie);
        HttpRequest request = getRequest(MOVIES + "?year=2020");
        HttpResponse<String> response = client.send(request, bodyHandler);

        assertEquals(200, response.statusCode(), "Успешный фильтр - код должен быть 200");
        assertEquals(CT_JSON, response.headers().firstValue("Content-type").get());
    }

    @Test
    @DisplayName("Получить список за год, где нет фильмов")
    void getMoviesForYear_EmptyList() throws IOException, InterruptedException {
        HttpRequest requestEmptyBody = getRequest(MOVIES + "?year=2021");
        HttpResponse<String> responseEmptyBody = client.send(requestEmptyBody, bodyHandler);

        assertEquals("[]", responseEmptyBody.body(), "В теле должен быть пустой массив");
    }

    @Test
    @DisplayName("Некорректный запрос списка фильмов (ошибка валидации)")
    void getMoviesForYear_BadRequest() throws IOException, InterruptedException {
        HttpRequest request = getRequest(MOVIES + "?year=QWERTY");
        HttpResponse<String> response = client.send(request, bodyHandler);

        assertEquals(400, response.statusCode(), "Некорректный запрос - код должен быть 400");
    }

    @Test
    @DisplayName("Необрабатываемый метод")
    void unprocessedMethod() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080" + MOVIES))
                .PUT(HttpRequest.BodyPublishers.ofString("unprocessedMethod"))
                .build();
        HttpResponse<String> response = client.send(request, bodyHandler);

        assertEquals(405, response.statusCode(), "Необрабатываемый метод - код должен быть 405");
        assertEquals("[\"Принимаю только Get и POST запросы!\"]", response.body(), "Что-то не то с телом");
    }

}