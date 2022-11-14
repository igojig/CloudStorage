package ru.igojig.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloudUtil {

    public static final String STRING_DELIMITER ="&";
    public static final String HOST="localhost";
    public static final int PORT=8189;


    public static List<String> getFileListInDir(Path path) {
        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
                    .map(p->p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

    }

    public  static void sendFileListInDir(Path path, Channel channel, ChannelFutureListener finishListener){

        ByteBuf buf = null;

        List<String> fileList= CloudUtil.getFileListInDir(path);
        String str= String.join(STRING_DELIMITER, fileList);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte((byte)Header.FILE_LIST.getHeader());
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        byte[] bytes=str.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        buf.writeBytes(bytes);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void  requestFileList(Channel channel, ChannelFutureListener finishListener){
       ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1);
       buf.writeByte(Header.COMMAND.getHeader());
       channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(Command.GET_FILE_LIST.getCommand());

        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }


}
