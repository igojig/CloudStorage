package ru.igojig.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import ru.igojig.common.Command;
import ru.igojig.common.Header;
import ru.igojig.common.callback.ProgressBarAction;
import ru.igojig.common.fileutils.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProtocolUtils {
    public static final String TOKEN_DELIMITER = "&";
    public static final String HOST = "localhost";
    public static final int PORT = 8189;
    public static final int COPY_BUFFER_SIZE = 8192;

     public static void sendFileListInDir(Path path, Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = null;
        List<String> fileList = FileUtils.getFileListInDir(path);
        String strFileList = String.join(ProtocolUtils.TOKEN_DELIMITER, fileList);
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

     public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener, ProgressBarAction progressCallback) throws IOException {
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
        // пишем длину самого файла
        buf.writeLong(Files.size(path));
        channel.write(buf);
        // пишем сам файл
        ChannelFuture transferOperationFuture = channel.writeAndFlush(region, channel.newProgressivePromise());

        // если вызов со стороны сервера, то callback==null
        if(progressCallback!=null) {
            transferOperationFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                    progressCallback.progress((double) progress, (double) total);
                }
                @Override
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                    progressCallback.progress(0., 1.);
                }
            });
        }

        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

     public static void sendCommandGetFile(String filename, Channel channel, ChannelFutureListener finishListener) {
        ByteBuf buf = null;
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 1 + 4 + filenameBytes.length);
        buf.writeByte(Header.COMMAND.getHeader());
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
        String strCommand = oldName + ProtocolUtils.TOKEN_DELIMITER + newName;
        byte[] bytes = strCommand.getBytes(StandardCharsets.UTF_8);

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
        String authStr = login + ProtocolUtils.TOKEN_DELIMITER + password;
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
