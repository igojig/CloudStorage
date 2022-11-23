package ru.igojig.common.filegenerators;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Arrays;


// генерим файл на 100 MB
public class FileGenerator {
    public static void main(String[] args) throws IOException {
        int size=1024*1024;
        byte[] bytes=new byte[size];
        Arrays.fill(bytes, (byte)65);
        Path path= Path.of("test_100mb.bin");
        Files.createFile(path);
        OutputStream out =Files.newOutputStream(path, StandardOpenOption.CREATE);
        BufferedOutputStream b=new BufferedOutputStream(out);
        for(int i=0;i<100;i++){
            b.write(bytes);
        }
        out.close();
        b.close();
    }
}
