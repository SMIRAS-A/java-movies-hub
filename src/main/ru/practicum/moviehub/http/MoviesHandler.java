package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        if (pathParts.length == 2 && pathParts[1].equals("movies")) {
            handleRoot(ex);
            return;
        }

        if (pathParts.length == 3 && pathParts[1].equals("movies")) {
            handleWithId(ex, pathParts[2]);
            return;
        }

        sendJson(ex, 404, gson.toJson(new ErrorResponse("Эндпоинт не найден", null)));
    }

    private void handleRoot(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            String query = ex.getRequestURI().getQuery();
            if (query != null && query.startsWith("year=")) {
                try {
                    int year = Integer.parseInt(query.substring(5));
                    List<Movie> movies = moviesStore.getByYear(year);
                    sendJson(ex, 200, gson.toJson(movies));
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса - 'year'", null)));
                }
            } else {
                List<Movie> movies = moviesStore.getAll();
                sendJson(ex, 200, gson.toJson(movies));
            }
            return;
        }

        if (method.equalsIgnoreCase("POST")) {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendJson(ex, 415, gson.toJson(new ErrorResponse("Unsupported Media Type", null)));
                return;
            }

            try {
                String body = readText(ex);
                Movie movie = gson.fromJson(body, Movie.class);

                if (movie == null) {
                    sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный JSON", null)));
                    return;
                }

                List<String> validationErrors = validate(movie);
                if (!validationErrors.isEmpty()) {
                    sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", validationErrors)));
                    return;
                }

                moviesStore.add(movie);
                sendJson(ex, 201, gson.toJson(movie));

            } catch (JsonSyntaxException e) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный JSON", null)));
            }
            return;
        }

        ex.sendResponseHeaders(405, -1);
    }

    private void handleWithId(HttpExchange ex, String idString) throws IOException {
        int id;
        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный ID", null)));
            return;
        }

        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            Optional<Movie> movie = moviesStore.getById(id);
            if (movie.isPresent()) {
                sendJson(ex, 200, gson.toJson(movie.get()));
            } else {
                sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден", null)));
            }
            return;
        }

        if (method.equalsIgnoreCase("DELETE")) {
            if (moviesStore.delete(id)) {
                sendNoContent(ex);
            } else {
                sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден", null)));
            }
            return;
        }

        ex.sendResponseHeaders(405, -1);
    }

    private List<String> validate(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("длина названия не должна превышать 100 символов");
        }

        int currentYear = LocalDate.now().getYear();
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между 1888 и " + (currentYear + 1));
        }

        return errors;
    }
}