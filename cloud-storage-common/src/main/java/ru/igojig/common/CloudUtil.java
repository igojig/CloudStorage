package ru.igojig.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import ru.igojig.common.callback.ProgressBarActive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudUtil {


    private static ProgressBarActive callback;

    public static final String STRING_DELIMITER = "&";
    public static final String HOST = "localhost";
    public static final int PORT = 8189;

    public static void setCallback(ProgressBarActive callback) {
        CloudUtil.callback = callback;
    }

    public static List<String> getFileListInDir(Path path) {
        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
//                    .filter(p-> {
//                        try {
//                            return !Files.isHidden(p);
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    })
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

    }

     public static void sendFileListInDir(Path path, Channel channel, ChannelFutureListener finishListener) {

        ByteBuf buf = null;

        List<String> fileList = CloudUtil.getFileListInDir(path);
        String strFileList = String.join(STRING_DELIMITER, fileList);
        byte[] bytesStr = strFileList.getBytes(StandardCharsets.UTF_8);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + bytesStr.length);

        buf.writeByte(Header.FILE_LIST.getHeader());

        buf.writeInt(bytesStr.length);

        buf.writeBytes(bytesStr);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void requestFileList(Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 1);
        buf.writeByte(Header.COMMAND.getHeader());

        buf.writeByte(Command.GET_FILE_LIST.getCommand());

        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

     public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

        ByteBuf buf = null;
        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length + 8);
        // пишем байт заголовка
        buf.writeByte((byte) Header.FILE.getHeader());


        // пишем длину имени файла
        buf.writeInt(filenameBytes.length);

        // пишем имя файла
        buf.writeBytes(filenameBytes);
//        channel.write(buf);

        // пишем длину самого файла
        buf.writeLong(Files.size(path));
        channel.write(buf);


        // пишем сам файл
        ChannelFuture transferOperationFuture = channel.writeAndFlush(region, channel.newProgressivePromise());

        // если вызов со стороны сервера, то callback==null
        if(callback!=null) {
            transferOperationFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                    callback.progress((double) progress, (double) total);
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                    callback.progress(0., 1.);
                }
            });
        }


        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }


    }

     public static void sendCommandFileRequest(String filename, Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = null;

        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 1 + 4 + filenameBytes.length);

        buf.writeByte(Header.COMMAND.getHeader());

//        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(Command.GET_FILE.getCommand());


        buf.writeInt(filenameBytes.length);

        buf.writeBytes(filenameBytes);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void sendCommandRenameFile(String oldName, String newName, Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = null;
        String strBody = oldName + CloudUtil.STRING_DELIMITER + newName;
        byte[] bytes = strBody.getBytes(StandardCharsets.UTF_8);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 1 + 4 + bytes.length);
        buf.writeByte(Header.COMMAND.getHeader());

        buf.writeByte(Command.RENAME.getCommand());


        buf.writeInt(bytes.length);

        buf.writeBytes(bytes);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void sendCommandDeleteFile(String filename, Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = null;
        byte[] bytes = filename.getBytes(StandardCharsets.UTF_8);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 1 + 4 + bytes.length);
        buf.writeByte(Header.COMMAND.getHeader());

        buf.writeByte(Command.DELETE.getCommand());

        buf.writeInt(bytes.length);

        buf.writeBytes(bytes);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }

    }

     public static void sendAuth(String login, String password, Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = null;
        String authStr = login + STRING_DELIMITER + password;
        byte[] authBytes = authStr.getBytes(StandardCharsets.UTF_8);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + authBytes.length);

        buf.writeByte(Header.AUTH_REQUEST.getHeader());
        buf.writeInt(authBytes.length);
        buf.writeBytes(authBytes);

        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }

    }

     public static void sendAuthOk(String username, Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = null;
        byte[] authOkBytes = username.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + authOkBytes.length);

        buf.writeByte(Header.AUTH_OK.getHeader());
        buf.writeInt(authOkBytes.length);
        buf.writeBytes(authOkBytes);

        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void sendAuthErr(Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(Header.AUTH_ERR.getHeader());

        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }

    }


}
