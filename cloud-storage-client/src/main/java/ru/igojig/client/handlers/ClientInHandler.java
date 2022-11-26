package ru.igojig.client.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.igojig.common.protocol.ProtocolUtils;
import ru.igojig.common.HandlerState;
import ru.igojig.common.Header;
import ru.igojig.common.callback.ProtoCallback;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClientInHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger= LogManager.getLogger(ClientInHandler.class);
    private Map<String, ProtoCallback> cloudCallbackMap;
    private HandlerState currentState = HandlerState.IDLE;
    private int nextLength; // длина следующей получаемой части
    private long fileLength;
    private long receivedFileLength;
    private String fileName;
    private BufferedOutputStream out;
    boolean fileReceived=false;
    Path rootPath=Path.of(".", "client_repository");
    DecimalFormat decimalFormat=new DecimalFormat();
    {
        decimalFormat.setGroupingSize(3);
    }


    public void setCloudCallbackMap(Map<String, ProtoCallback> map) {
        this.cloudCallbackMap = map;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("Канал закрыт " + ctx);
        ctx.close();
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
                } else if(controlByte==Header.AUTH_ERR.getHeader()){
                    // вызываем callback
                    cloudCallbackMap.get("AUTH_ERR").callback(null);
                }
                else if (controlByte==Header.AUTH_OK.getHeader()) {
                    currentState=HandlerState.AUTH_LENGTH;
                    nextLength=0;
                } else {
                    logger.error("Неизвестный тип заголовка: " + controlByte);
                    buf.release();
                    return;
                }
            }

            if(currentState==HandlerState.AUTH_LENGTH){
                if(buf.readableBytes()>=4){
                    nextLength=buf.readInt();
                    currentState=HandlerState.AUTH;
                }
            }

            if(currentState==HandlerState.AUTH){
                if(buf.readableBytes()>=nextLength){
                    byte[] bytesStr=new byte[nextLength];
                    buf.readBytes(bytesStr);
                    String username=new String(bytesStr, StandardCharsets.UTF_8);
                    // передаем пользователя в контроллер
                    cloudCallbackMap.get("AUTH_OK").callback(username);

                    // добавляем в rootPath пользователя
                    rootPath=rootPath.resolve(username);

                    currentState=HandlerState.IDLE;
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
                    Path path=rootPath.resolve(fileName);
                    if (Files.exists(path)) {
                        Files.delete(path);
                    }
                    File file = path.toFile();
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    currentState = HandlerState.FILE_LENGTH;
                    logger.info(String.format("Принимаем файл [%s]", file.getName()));
                }
            }

            if (currentState == HandlerState.FILE_LENGTH) {
                // ждем длину файла
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    currentState = HandlerState.FILE;
                    receivedFileLength = 0L;
                    fileReceived=false;

                }
            }

            if (currentState == HandlerState.FILE) {
                // ждем файл
                if (fileLength == 0) {
                    logger.info("Файл: " + fileName + " принят. Размер: " + fileLength);
                    currentState = HandlerState.IDLE;
                    fileReceived=true;
                    out.close();

                    // обновляем список файлов на клиенте
                    cloudCallbackMap.get("GET_FILE").callback(fileName, fileLength);
                } else {
//
                    while (buf.readableBytes() > 0) {
                        byte readed = buf.readByte();

                        out.write(readed);
                        ++receivedFileLength;

                        if (receivedFileLength == fileLength) {
                            fileReceived=true;
                            cloudCallbackMap.get("PROGRESS_BAR").callback(0., 1.);
                            logger.info("Файл: " + fileName + " принят. Размер: " + decimalFormat.format(fileLength));
                            currentState = HandlerState.IDLE;
                            out.close();



                            // обновляем список файлов на клиенте
                            cloudCallbackMap.get("GET_FILE").callback(fileName, fileLength);

                            // получили файл
                            // передаем клиенту список файлов
                            ProtocolUtils.sendFileListInDir(rootPath, ctx.channel(), f -> {
                                if (!f.isSuccess()) {
                                    logger.throwing(f.cause());
                                }
                                if (f.isSuccess()) {
                                    logger.info("Список файлов успешно передан на сервер");
                                }
                            });

                            break;
                        }
                    }
                    if(!fileReceived){
                        cloudCallbackMap.get("PROGRESS_BAR").callback((double) receivedFileLength , (double) fileLength);
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
                        String[] fileString = str.split(ProtocolUtils.TOKEN_DELIMITER);
                        fileList = Arrays.stream(fileString).toList();
                    }
                    // ищем callback и вызываем его
                    cloudCallbackMap.get("GET_FILE_LIST").callback(fileList);

                    logger.info("Получили список файлов от сервера:");
                    logger.trace(fileList);
//                    System.out.println(fileList);
                    currentState = HandlerState.IDLE;
                }
            }
//----------------------------------------------------------------

        }



        if (buf.readableBytes() == 0) {
            buf.release();
        }


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.throwing(cause);
    }
}
