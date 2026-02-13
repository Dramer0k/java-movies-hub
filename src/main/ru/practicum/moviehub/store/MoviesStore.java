package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
    Map<Integer, Movie> movieMap = new HashMap<>();

    public void addMovie(String title, int year) {
        for (Movie movie : movieMap.values()) {
            if (movie.getTitle().equalsIgnoreCase(title)) {
                return;
            }
        }
        movieMap.put(movieMap.size(), new Movie(title, year));
    }

    public Movie getMovie(int id) {
        return movieMap.get(id);
    }

    public int getMovieId(String movie) {
        int movieId = 0;
        for (int id : movieMap.keySet()) {
            if (movieMap.get(id).getTitle().equalsIgnoreCase(movie)) {
                movieId = id;
            }
        }
        return movieId;
    }

    public Map<Integer, Movie> getAllMovie() {
        return movieMap;
    }

    public void deleteMovie(int id) {
        movieMap.remove(id);
    }

    public boolean checkMovie(String string) {
        for (Movie movie : movieMap.values()) {
            if (movie.getTitle().equalsIgnoreCase(string)) {
                return true;
            }
        }
        return false;
    }


    public void clear() {
        movieMap.clear();
    }

    @Override
    public String toString() {
        return "MoviesStore{" +
                "movieMap=" + movieMap +
                '}';
    }
}