package ru.igojig.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.igojig.common.Command;
import ru.igojig.common.HandlerState;
import ru.igojig.common.Header;
import ru.igojig.common.CloudUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class ServeкFirstInHandler extends ChannelInboundHandlerAdapter {



    private HandlerState currentState = HandlerState.IDLE;
    private int nextLength; // длина следующей получаемой части
    private long fileLength;
    private long receivedFileLength;
    private String fileName;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);

        while (buf.readableBytes() > 0) {
            if (currentState == HandlerState.IDLE) {
                byte controlByte = buf.readByte();
                // проверяем заголовок, иходя из него выставляем HandlerState
                if (controlByte == Header.FILE.getHeader()) {
                    // переходим в состояние получения файла
                    currentState = HandlerState.FILE_NAME_LENGTH;
                    nextLength=0;
                    fileLength=0;
                    receivedFileLength=0L;
                    fileName=null;
                } else if (controlByte == Header.FILE_LIST.getHeader()) {
                    // переходим в состояние получения списка файлов с клиента
                    currentState= HandlerState.FILE_LIST_LENGTH;
                    nextLength=0;
                } else if (controlByte == Header.COMMAND.getHeader()) {
                    // переходим в состояние получения команды от клиента
                    currentState= HandlerState.COMMAND;
                } else {
                    System.out.println("Неизвестный тип заголовка: " + controlByte);
                }
            }

//-------------------------------------------------------------------
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
                    fileName=new String(bytes, StandardCharsets.UTF_8);
//                    System.out.println(fileName);
                    Path path=Path.of(".", "server_repository", fileName);
                    if(Files.exists(path)){
                        Files.delete(path);
                    }
                    File file=path.toFile();
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    currentState = HandlerState.FILE_LENGTH;
                }
            }

            if(currentState== HandlerState.FILE_LENGTH) {
                // ждем длину файла
                if(buf.readableBytes()>=8){
                    fileLength=buf.readLong();
                    currentState= HandlerState.FILE;
                    receivedFileLength=0L;
                }
            }

            if(currentState== HandlerState.FILE){
                // ждем файл

                while (buf.readableBytes()>0){
                    byte readed= buf.readByte();
                    out.write(readed);
                    receivedFileLength++;
                    if(receivedFileLength==fileLength){
                        System.out.println("Файл: " + fileName + " принят. Размер: " + fileLength);
                        currentState= HandlerState.IDLE;
                        out.close();


                        CloudUtil.sendFileListInDir(Path.of(".","server_repository"), ctx.channel(), f->{
                            if (!f.isSuccess()) {
                                f.cause().printStackTrace();
                            }
                            if (f.isSuccess()) {
                                System.out.println("Список файлов успешно передан на клиент");
                            }
                        });

                        break;
                    }
                }
            }
//-----------------------------------------------------------
            if(currentState== HandlerState.FILE_LIST_LENGTH){
                // ждем список файлов от клиента
                // получаем размер списка - int - 4 byte
                if(buf.readableBytes()>=4){
                    nextLength=buf.readInt();
                    currentState= HandlerState.FILE_LIST;
                }
            }

            if(currentState== HandlerState.FILE_LIST){
                if(buf.readableBytes()>=nextLength){
                    byte[] bytes=new byte[nextLength];
                    buf.readBytes(bytes);
                    String[] fileList=new String(bytes, StandardCharsets.UTF_8).strip().split(CloudUtil.STRING_DELIMITER);
                    System.out.println("Получили спискок файлов от клиента:");
                    System.out.println(Arrays.toString(fileList));
                    currentState= HandlerState.IDLE;
                }
            }
//----------------------------------------------------------------
// получили команду -> прокидываем в следующий Handler

            if(currentState== HandlerState.COMMAND){
                if(buf.readableBytes()>0){
                    // вычитывем команду
                    byte command=buf.readByte();
                    if(command == Command.RENAME.getCommand()){
                        currentState= HandlerState.COMMAND_RENAME_LENGTH;
                        nextLength=0;
                        // запрос списка файлов от клиента
                    } else if (command==Command.GET_FILE_LIST.getCommand()) {
                        System.out.println("Запрос от клиента: " + Command.GET_FILE_LIST);
                        currentState=HandlerState.IDLE;
                        CloudUtil.sendFileListInDir(Path.of(".","server_repository"), ctx.channel(), f->{
                            if (!f.isSuccess()) {
                                f.cause().printStackTrace();
                            }
                            if (f.isSuccess()) {
                                System.out.println("Список файлов успешно передан на клиент");
                            }
                        });
                    }
                }
            }

            if(currentState== HandlerState.COMMAND_RENAME_LENGTH){
                // читаем длину пакета
                if(buf.readableBytes()>=4){
                    nextLength=buf.readInt();
                    currentState= HandlerState.COMMAND_RENAME;
                }
            }

            if(currentState== HandlerState.COMMAND_RENAME){
                // читаем пакет
                if(buf.readableBytes()>=nextLength){
                    byte[] bytes=new byte[nextLength];
                    buf.readBytes(bytes);
                    // формат: старое_имя ПРОБЕЛ новое_имя
                    String[] str=new String(bytes, StandardCharsets.UTF_8).split(CloudUtil.STRING_DELIMITER);
                    String oldName=str[0];
                    String newName=str[1];
                    Path pathOld=Path.of(".", "server_repository", oldName);
                    Path pathNew=Path.of(".", "server_repository", newName);
                    Path result=Files.move(pathOld, pathNew, StandardCopyOption.REPLACE_EXISTING);
                    if(Files.exists(result)){
                        System.out.println("Файл: " + pathOld + " переименован в: " + pathNew);
                    }
                    currentState= HandlerState.IDLE;
                }
            }

        }

//        while (buf.readableBytes() > 0) {
//            if (currentState == State.IDLE) {
//                byte readed = buf.readByte();
//                if (readed == (byte) 25) {
//                    currentState = State.NAME_LENGTH;
//                    receivedFileLength = 0L;
//                    System.out.println("STATE: Start file receiving");
//                } else {
//                    System.out.println("ERROR: Invalid first byte - " + readed);
//                }
//            }
//
//
//            if (currentState == State.NAME_LENGTH) {
//                if (buf.readableBytes() >= 4) {
//                    System.out.println("STATE: Get filename length");
//                    nextLength = buf.readInt();
//                    currentState = State.NAME;
//                }
//            }
//
//            if (currentState == State.NAME) {
//                if (buf.readableBytes() >= nextLength) {
//                    byte[] fileName = new byte[nextLength];
//                    buf.readBytes(fileName);
//                    System.out.println("STATE: Filename received - _" + new String(fileName, StandardCharsets.UTF_8));
//
//
//                    out = new BufferedOutputStream(new FileOutputStream("server_repository" + new String(fileName)));
//                    currentState = State.FILE_LENGTH;
//                }
//            }
//
//            if (currentState == State.FILE_LENGTH) {
//                if (buf.readableBytes() >= 8) {
//                    fileLength = buf.readLong();
//                    System.out.println("STATE: File length received - " + fileLength);
//                    currentState = State.FILE;
//                }
//            }
//
//            if (currentState == State.FILE) {
//                while (buf.readableBytes() > 0) {
//                    out.write(buf.readByte());
//                    receivedFileLength++;
//                    if (fileLength == receivedFileLength) {
//                        currentState = State.IDLE;
//                        System.out.println("File received");
//                        out.flush();
//                        out.close();
//                        break;
//                    }
//                }
//            }
//        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }


//        ctx.writeAndFlush("Hello from server");

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
