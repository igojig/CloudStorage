package ru.igojig.server.callback;

import java.util.Optional;

public interface AuthCallback {
    Optional<String> authCallback(String login, String password);
}
