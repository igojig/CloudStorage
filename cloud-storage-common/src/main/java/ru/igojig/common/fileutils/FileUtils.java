package ru.igojig.common.fileutils;

import ru.igojig.common.callback.CloudCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    synchronized public static boolean createUserDir(Path path, CloudCallback cloudCallback) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                cloudCallback.callback("Создали директорию пользователя: " + path);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                cloudCallback.callback("Не удалось создать директорию пользователя: " + path);
                return false;
            }
        }
        cloudCallback.callback("Директория пользователя существует " + path);
        return true;
    }
}
