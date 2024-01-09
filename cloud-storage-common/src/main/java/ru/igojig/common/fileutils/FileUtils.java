package ru.igojig.common.fileutils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.igojig.common.callback.ProgressBarAction;
import ru.igojig.common.callback.ProtoCallback;
import ru.igojig.common.protocol.ProtocolUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FileUtils {

    private static final Logger logger= LogManager.getLogger(FileUtils.class);

    private static final ExecutorService executorService= Executors.newSingleThreadExecutor();

    synchronized public static boolean createUserDir(Path path, ProtoCallback protoCallback) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                protoCallback.callback("Создали директорию пользователя: " + path);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                protoCallback.callback("Не удалось создать директорию пользователя: " + path);
                return false;
            }
        }
        protoCallback.callback("Директория пользователя существует " + path);
        return true;
    }

    public static void addExternalFile(Path externalFile, Path localFile, ProtoCallback infoController, ProgressBarAction setProgress){
        executorService.execute(()->{
            try(FileInputStream fis=new FileInputStream(externalFile.toFile());
                BufferedInputStream bis=new BufferedInputStream(fis);
                FileOutputStream fos=new FileOutputStream(localFile.toFile());
                BufferedOutputStream bos=new BufferedOutputStream(fos)) {
                long length=Files.size(externalFile);


                int read=0;
                long readedBytes=0;
                byte[] buf=new byte[ProtocolUtils.COPY_BUFFER_SIZE];
                while((read = bis.read(buf))>0) {
                    readedBytes+=read;
                    bos.write(buf, 0, read);
                    setProgress.progress((double)readedBytes,(double)length);
//                    progressBar.setProgress(1.0* finalReadedBytes /length);
                }
                setProgress.progress(0., 1.);
                logger.info(String.format("Добавили файл:[%s], размер: [%s]", localFile.getFileName().toString(), length));
//                cloudCallback.callback(newPath.getFileName().toString(), length);
                infoController.callback(localFile.getFileName().toString(), length);
            } catch (IOException e) {
                logger.throwing(e);
            }
        });
    }

    public static List<String> getFileListInDir(Path path) {
        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
//                    .filter(p-> Files.isHidden(p))
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.throwing(e);
            return Collections.emptyList();
        }
    }

    public static void stopExecutor(){
        logger.trace("Shutting down executor");
        executorService.shutdown();
    }
}
