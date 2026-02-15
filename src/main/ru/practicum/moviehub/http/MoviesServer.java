package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    protected final static String MOVIES = "/movies";
    HttpServer server;
    MoviesStore store;

    public MoviesServer(MoviesStore moviesStore, int port) throws IOException {
        store = moviesStore;

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext(MOVIES, new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать Http-сервер");
        }

    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен!");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен!");
    }
}