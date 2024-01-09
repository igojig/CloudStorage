package ru.igojig.common.filegenerators;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Arrays;


// генерим файл на 100 MB
public class FileGenerator {
    public static void main(String[] args) {

        final int blank = 65;
        final int fileSize = 1024; // в мегабайтах

        Path path = Path.of(".", "test_100mb.bin");

        try {
            Files.createFile(path);
            try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
                try (BufferedOutputStream b = new BufferedOutputStream(out)) {
                    for (int i = 0; i < fileSize * 1024; i++) {
                        b.write(blank);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
