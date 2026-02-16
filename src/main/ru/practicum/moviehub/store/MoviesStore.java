package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MoviesStore {
    Map<Integer, Movie> movieMap = new HashMap<>();

    public void addMovie(Movie movie) {
        for (Movie mov : movieMap.values()) {
            if (Objects.equals(mov.getTitle(), movie.getTitle())) {
                return;
            }
        }
        movieMap.put(movieMap.size(), movie);
    }

    public Movie getMovie(int id) {
        return movieMap.get(id);
    }

    public int getMovieId(String movie) {
        int movieId = 0;
        for (int id : movieMap.keySet()) { //на выходе нужно получить id, если буду бегать по value я его не получу
            if (Objects.equals(movieMap.get(id).getTitle(), movie)) {
                movieId = id;
            }
        }
        return movieId;
    }

    public Map<Integer, Movie> getMoviesMap() {
        return movieMap;
    }

    public void deleteMovie(int id) {
        movieMap.remove(id);
    }

    public boolean checkMovie(String title) {
        for (Movie movie : movieMap.values()) {
            if (Objects.equals(movie.getTitle(), title)) {
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