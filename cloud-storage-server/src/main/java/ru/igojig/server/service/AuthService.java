package ru.igojig.server.service;

import java.util.Optional;

public interface AuthService {
    Optional<String> getUsernameByLoginAndPassword(String login, String password);
    void openConnection();
    void closeConnection();
}
