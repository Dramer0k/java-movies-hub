package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest extends BaseApiTestMethod {
    private static final String CT_JSON = "application/json; charset=UTF-8";
    private static final HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    private static MoviesServer server;
    private static HttpClient client;
    private final Gson gson = new Gson();
    private final ErrorResponse error = new ErrorResponse();
    static MoviesStore store = new MoviesStore();

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
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception   {

        HttpRequest request = getRequest("/movies");
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
    void  postMovies_UnprocessableEntity_UnsupportedMediaType() throws IOException, InterruptedException {
        store.addMovie("Волколак", 1994);
        String jsonString = gson.toJson(store.getMovie(0));

        HttpRequest request = postRequest_movies(jsonString, "application/json");
        HttpResponse<String> response = client.send(request, bodyHandler);

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен возвращать формат данных и кодировку");

        assertEquals(201, response.statusCode(), "Успешное добавление - код должен быть 201");

        String json_invalidYear = "{\"title\": \"" + "Dogs" + "\", \"year\": " + 2036 + "}";
        HttpRequest request2 = postRequest_movies(json_invalidYear, "application/json");
        HttpResponse<String> response2 = client.send(request2, bodyHandler);
        assertEquals(422, response2.statusCode(), "Ошибка валидации - код должен быть 422!");

        //Запрос с некорректным типом
        HttpRequest request_invalid_CT = postRequest_movies(jsonString, "text");
        HttpResponse<String> response3 = client.send(request_invalid_CT, bodyHandler);
        assertEquals(415, response3.statusCode(), "Неправильное значение заголовка - код должен быть 415!");
    }

    @Test
    void getMoviesId_NotFound_BadRequest() throws IOException, InterruptedException {
        store.addMovie("Байлор", 1998);
        HttpRequest request = getRequest("/movies/0");
        HttpResponse<String> response = client.send(request, bodyHandler);
        assertEquals(200, response.statusCode(), "Фильм найден - код должен быть 200");
        assertEquals(CT_JSON, response.headers().firstValue("Content-type").get());

        HttpRequest request_NotFound = getRequest("/movies/14");
        HttpResponse<String> response_NotFound = client.send(request_NotFound, bodyHandler);
        assertEquals(404, response_NotFound.statusCode(), "Фильм не найдет - код должен быть 404");

        HttpRequest request_BadRequest = getRequest("/movies/qwerty");
        HttpResponse<String> response_BadRequest = client.send(request_BadRequest, bodyHandler);
        assertEquals(400, response_BadRequest.statusCode(), "Некорректный id - код должен быть 400");
    }

    @Test
    void deleteMovie_NoContent_NotFound() throws IOException, InterruptedException {
        store.addMovie("Черный карандаш", 1892);

        HttpRequest request = deleteRequest("/movies/0");
        HttpResponse<String> response = client.send(request, bodyHandler);
        assertEquals(204, response.statusCode(), "Фильм удален - код должен быть 204");
        assertEquals(CT_JSON, response.headers().firstValue("Content-type").get());

        HttpRequest request_NotFound = deleteRequest("/movies/14");
        HttpResponse<String> response_NotFound = client.send(request_NotFound, bodyHandler);
        assertEquals(404, response_NotFound.statusCode(), "Фильм не найден - код должен быть 404");

        HttpRequest request_BadRequest = deleteRequest("/movies/qwerty");
        HttpResponse<String> response_BadRequest = client.send(request_BadRequest, bodyHandler);
        assertEquals(400, response_BadRequest.statusCode(), "Неверный id - код должен быть 400");
    }

    @Test
    void getMoviesForYear_BadRequest() throws IOException, InterruptedException {
        store.addMovie("Корабль", 2020);

        HttpRequest request = getRequest("/movies?year=2020");
        HttpResponse<String> response = client.send(request, bodyHandler);
        assertEquals(200, response.statusCode(), "Успешный фильтр - код должен быть 200");
        assertEquals(CT_JSON, response.headers().firstValue("Content-type").get());

        HttpRequest request_emptyBody = getRequest("/movies?year=2021");
        HttpResponse<String> response_emptyBody = client.send(request_emptyBody, bodyHandler);
        assertTrue(response_emptyBody.body().equalsIgnoreCase("[]"), "В теле должен быть пустой массив");

        HttpRequest request_BadRequest = getRequest("/movies?year=QWERTY");
        HttpResponse<String> response_BadRequest = client.send(request_BadRequest, bodyHandler);
        assertEquals(400, response_BadRequest.statusCode(), "Некорректный запрос - код должен быть 400");
    }
}