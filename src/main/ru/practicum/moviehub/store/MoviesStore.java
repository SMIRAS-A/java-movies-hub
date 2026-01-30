package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int currentId = 1;

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public void add(Movie movie) {
        movie.setId(currentId++);
        movies.put(movie.getId(), movie);
    }

    public void clear() {
        movies.clear();
        currentId = 1;
    }
}