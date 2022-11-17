package ru.igojig.client.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.igojig.common.callback.CloudCallback;
import ru.igojig.common.HandlerState;
import ru.igojig.common.Header;
import ru.igojig.common.CloudUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClientInHandler extends ChannelInboundHandlerAdapter {

    private Map<String, CloudCallback> cloudCallbackMap;

    private HandlerState currentState = HandlerState.IDLE;
    private int nextLength; // длина следующей получаемой части
    private long fileLength;
    private long receivedFileLength;
    private String fileName;
    private BufferedOutputStream out;

    Path rootPath=Path.of(".", "client_repository");

    public void setCloudCallbackMap(Map<String, CloudCallback> map) {
        this.cloudCallbackMap = map;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        while (buf.readableBytes() > 0) {
            if (currentState == HandlerState.IDLE) {
                byte controlByte = buf.readByte();
                if (controlByte == Header.FILE.getHeader()) {
                    // переходим в состояние получения файла
                    currentState = HandlerState.FILE_NAME_LENGTH;
                    nextLength = 0;
                    fileLength = 0;
                    receivedFileLength = 0L;
                    fileName = null;
                } else if (controlByte == Header.FILE_LIST.getHeader()) {
                    // переходим в состояние получения списка файлов с сервера
                    currentState = HandlerState.FILE_LIST_LENGTH;
                    nextLength = 0;
                } else if (controlByte == Header.COMMAND.getHeader()) {
                    // переходим в состояние получения команды с сервера
                    currentState = HandlerState.COMMAND;
                } else {
                    System.out.println("Неизвестный тип заголовка: " + controlByte);
                }
            }

            if (currentState == HandlerState.FILE_NAME_LENGTH) {
                // ждем длину имени файла
                if (buf.readableBytes() >= 4) {
                    nextLength = buf.readInt();
                    currentState = HandlerState.FILE_NAME;
                }
            }

            if (currentState == HandlerState.FILE_NAME) {
                // ждем имя файла
                if (buf.readableBytes() >= nextLength) {
                    byte[] bytes = new byte[nextLength];
                    buf.readBytes(bytes);
                    fileName = new String(bytes, StandardCharsets.UTF_8);
//                    System.out.println(fileName);
//                    Path path = Path.of(".", "client_repository", fileName);
                    Path path=rootPath.resolve(fileName);
                    if (Files.exists(path)) {
                        Files.delete(path);
                    }
                    File file = path.toFile();
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    currentState = HandlerState.FILE_LENGTH;
                }
            }

            if (currentState == HandlerState.FILE_LENGTH) {
                // ждем длину файла
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    currentState = HandlerState.FILE;
                    receivedFileLength = 0L;
                }
            }

            if (currentState == HandlerState.FILE) {
                // ждем файл
                if (fileLength == 0) {
                    System.out.println("Файл: " + fileName + " принят. Размер: " + fileLength);
                    currentState = HandlerState.IDLE;
                    out.close();

                    // обновляем список файлов на клиенте
                    cloudCallbackMap.get("GET_FILE").callback(null);
                } else {

                    while (buf.readableBytes() > 0) {
                        byte readed = buf.readByte();
                        out.write(readed);
                        receivedFileLength++;

                        cloudCallbackMap.get("PROGRESS_BAR").callback(1.0 * receivedFileLength / fileLength);
                        if (receivedFileLength == fileLength) {
                            cloudCallbackMap.get("PROGRESS_BAR").callback(0.);
                            System.out.println("Файл: " + fileName + " принят. Размер: " + fileLength);
                            currentState = HandlerState.IDLE;
                            out.close();

                            // обновляем список файлов на клиенте
                            cloudCallbackMap.get("GET_FILE").callback(null);

                            // получили файл
                            // передаем клиенту список файлов
                            CloudUtil.sendFileListInDir(rootPath, ctx.channel(), f -> {
                                if (!f.isSuccess()) {
                                    f.cause().printStackTrace();
                                }
                                if (f.isSuccess()) {
                                    System.out.println("Список файлов успешно передан на сервер");
                                }
                            });

                            break;
                        }
                    }
                }
            }

            //-----------------------------------------------------------
            if (currentState == HandlerState.FILE_LIST_LENGTH) {
                // ждем список файлов от сервера
                // получаем размер списка - int - 4 byte
                if (buf.readableBytes() >= 4) {
                    nextLength = buf.readInt();
                    currentState = HandlerState.FILE_LIST;
                }
            }

            // читаем список файлов
            if (currentState == HandlerState.FILE_LIST) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] bytes = new byte[nextLength];
                    buf.readBytes(bytes);
                    String str = new String(bytes, StandardCharsets.UTF_8);
                    List<String> fileList;
                    // если список файлов пустой
                    if (str.equals("")) {
                        fileList = Collections.emptyList();
                    } else {
                        String[] fileString = str.split(CloudUtil.STRING_DELIMITER);
                        fileList = Arrays.stream(fileString).toList();

                    }
                    // ищем callback и вызываем его
                    cloudCallbackMap.get("GET_FILE_LIST").callback(fileList);

                    System.out.println("Получили список файлов от сервера:");
                    System.out.println(fileList);
                    currentState = HandlerState.IDLE;
                }
            }
//----------------------------------------------------------------

        }

//
//        if(inboundState == InboundState.IDLE){
//            System.out.println("Что-то получили");
//
//
//            if(buf.readableBytes()>0){
//                int cmd=buf.readByte();
//
//                System.out.println("Команда: " + cmd);
//                inboundState = InboundState.parse(cmd);
//                System.out.println(inboundState);
//                if(inboundState.isError()){
//                    System.out.println("Unknown command");
//                }
//            }
//
//        }
//
//        // получили команду на отправку списка файлов
//        if(inboundState==InboundState.FILE_LIST_REQUEST){
//            File file=new File("client_repository");
//            String[] filesInDir= file.list();
//            StringBuilder sb=new StringBuilder();
//            for(String s: filesInDir){
//                sb.append(s).append(" ");
//            }
//            String str=sb.toString();
//            byte[] bytes=str.getBytes(StandardCharsets.UTF_8);
//            ByteBuf byteBuf=ByteBufAllocator.DEFAULT.directBuffer(1);
//            //передаем длину массива
//            byteBuf.writeByte((byte)bytes.length);
//            ctx.write(byteBuf);
//            byteBuf=ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
//            // передаем массив
//            byteBuf.writeBytes(bytes);
//            ctx.writeAndFlush(byteBuf);
//
//        }
//
//
//
//        if(inboundState == InboundState.FILE_LIST){
//            ByteBuf byteBuf= ByteBufAllocator.DEFAULT.directBuffer(nextLen);
//            if(buf.readableBytes()>=nextLen){
//                byte[] bytes=new byte[nextLen];
//                buf.readBytes(bytes);
//                String str=new String(bytes, StandardCharsets.UTF_8);
//                System.out.println(str);
//                inboundState = InboundState.CMD;
//            }
//        }

        if (buf.readableBytes() == 0) {
            buf.release();
        }


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
}
