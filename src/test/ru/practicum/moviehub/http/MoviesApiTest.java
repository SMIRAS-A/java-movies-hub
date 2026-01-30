package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final int PORT = 8080;
    private static final String BASE = "http://localhost:" + PORT;
    private static MoviesServer server;
    private static HttpClient client;
    private static final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), PORT);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server.getMoviesStore().clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("[]", resp.body().trim());
    }

    @Test
    void postMovie_whenValid_returnsCreatedAndMovie() throws Exception {
        Movie movie = new Movie(0, "Inception", 2010);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        JsonObject respJson = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Inception", respJson.get("title").getAsString());
        assertTrue(respJson.get("id").getAsInt() > 0);
    }

    @Test
    void postMovie_whenTitleEmpty_returns422() throws Exception {
        Movie movie = new Movie(0, "", 2010);
        String json = gson.toJson(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie movie = new Movie(0, "Matrix", 1999);
        server.getMoviesStore().add(movie);
        int id = movie.getId();

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        JsonObject respJson = JsonParser.parseString(resp.body()).getAsJsonObject();
        assertEquals("Matrix", respJson.get("title").getAsString());
    }

    @Test
    void getMovieById_whenNotExists_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/999")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        Movie movie = new Movie(0, "Terminator", 1984);
        server.getMoviesStore().add(movie);
        int id = movie.getId();

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/" + id)).DELETE().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode());
        assertTrue(server.getMoviesStore().getById(id).isEmpty());
    }

    @Test
    void getMoviesByYear_returnsFiltered() throws Exception {
        server.getMoviesStore().add(new Movie(0, "A", 2000));
        server.getMoviesStore().add(new Movie(0, "B", 2000));
        server.getMoviesStore().add(new Movie(0, "C", 2001));

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=2000")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertEquals(2, movies.size());
        assertEquals("A", movies.get(0).getTitle());
        assertEquals("B", movies.get(1).getTitle());
    }
}