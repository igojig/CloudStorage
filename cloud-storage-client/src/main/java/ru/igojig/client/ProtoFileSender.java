package ru.igojig.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import ru.igojig.common.Command;
import ru.igojig.common.Header;
import ru.igojig.common.CloudUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProtoFileSender {
    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte((byte) Header.FILE.getHeader());
        channel.writeAndFlush(buf);
//        channel.pipeline().writeAndFlush(buf);


        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(filenameBytes.length);
        channel.writeAndFlush(buf);
//        channel.pipeline().writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);
//        channel.pipeline().writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(path));
        channel.writeAndFlush(buf);
//        channel.pipeline().writeAndFlush(buf);

        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
//        ChannelFuture transferOperationFuture = channel.pipeline().writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public  static void sendFileList(List<String> fileList, Channel channel, ChannelFutureListener finishListener){
        ByteBuf buf = null;
        String str= String.join(CloudUtil.STRING_DELIMITER, fileList);

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

    public static void sendCommand( Channel channel, ChannelFutureListener finishListener){
        ByteBuf buf = null;

        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(Header.COMMAND.getHeader());
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(Command.RENAME.getCommand());
        channel.write(buf);

        String renameString="demo1.txt&переименованный.txt";
        byte[] bytes=renameString.getBytes(StandardCharsets.UTF_8);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(bytes.length);
        channel.write(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        buf.writeBytes(bytes);
        ChannelFuture transferOperationFuture  =channel.writeAndFlush(buf);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

}