package ru.igojig.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.igojig.common.CloudUtil;
import ru.igojig.common.Command;
import ru.igojig.common.HandlerState;
import ru.igojig.common.Header;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class ServerFirstInHandler extends ChannelInboundHandlerAdapter {

    private final String username;

    private HandlerState currentState = HandlerState.IDLE;
    private int nextLength; // длина следующей получаемой части
    private long fileLength;
    private long receivedFileLength;
    private String fileName;
    private BufferedOutputStream out;

    Path rootPath=Path.of(".", "server_repository");

    public ServerFirstInHandler(String username) {
        this.username = username;
        createUserDir(username);

    }

    private void createUserDir(String username) {
        rootPath=rootPath.resolve(username);
        if(!Files.exists(rootPath)){
            try {
                Files.createDirectory(rootPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Пользователь подключился: " + ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Пользователь " + username + " отключился" + ctx);
        ctx.close();
//        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        while (buf.readableBytes() > 0) {
            if (currentState == HandlerState.IDLE) {
                byte controlByte = buf.readByte();
                // проверяем заголовок, исходя из него выставляем HandlerState
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
//                    Path path=Path.of(".", "server_repository", fileName);
                    Path path=rootPath.resolve(fileName);

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
            // ждем файл
            if(currentState== HandlerState.FILE){
                // если файл нулевой длины
                if(fileLength==0){
                    System.out.println("Файл: " + fileName + " принят. Размер: " + fileLength);
                    currentState= HandlerState.IDLE;
                    out.close();

                    // получили файл
                    // передаем клиенту список файлов
                    sendFileListToClient(ctx.channel());
                } else {

                    while (buf.readableBytes() > 0) {

                        byte readed = buf.readByte();
                        out.write(readed);
                        receivedFileLength++;
                        if (receivedFileLength == fileLength) {
                            System.out.println("Файл: " + fileName + " принят. Размер: " + fileLength);
                            currentState = HandlerState.IDLE;
                            out.close();

                            // получили файл
                            // передаем клиенту список файлов
                            sendFileListToClient(ctx.channel());

                            break;
                        }
                    }
                }
            }

            // получаем список файлов - содержимое директории
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
                    String[] fileList=new String(bytes, StandardCharsets.UTF_8).split(CloudUtil.STRING_DELIMITER);
                    System.out.println("Получили список файлов от клиента:");
                    System.out.println(Arrays.toString(fileList));
                    currentState= HandlerState.IDLE;
                }
            }
//----------------------------------------------------------------
// получили команду -> прокидываем в следующий Handler??????

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
                        // отправили списрк файлов
                       sendFileListToClient(ctx.channel());

                    } else if(command==Command.GET_FILE.getCommand()){
                        // получили команду на запрос файла [длина_имени_файда][имя_файла]
                        System.out.println("Запрос от клиента: " + Command.GET_FILE);
                        currentState=HandlerState.COMMAND_GET_FILENAME_LENGTH;
                        nextLength=0;

                    }else if(command==Command.DELETE.getCommand()){
                        // получили команду на удаление
                        System.out.println("Запрос от клиента: " + Command.DELETE);
                        currentState=HandlerState.COMMAND_DELETE_LENGTH;
                    }
                    else {
                        System.out.println("Неизвестная команда: " + command);
                    }
                }
            }

            if(currentState==HandlerState.COMMAND_DELETE_LENGTH){
                if(buf.readableBytes()>=4) {
                    nextLength=buf.readInt();
                    currentState=HandlerState.COMMAND_DELETE;
                }
            }

            if(currentState==HandlerState.COMMAND_DELETE){
                if(buf.readableBytes()>=nextLength){
                    byte[] bytes=new byte[nextLength];
                    buf.readBytes(bytes);
                    String fileToDelete=new String(bytes, StandardCharsets.UTF_8);
//                    Path path=Path.of(".","server_repository", fileToDelete);
                    Path path=rootPath.resolve(fileToDelete);
                    System.out.println("Удаляем файл: " + path);
                    currentState=HandlerState.IDLE;
                    Files.delete(path);
                    sendFileListToClient(ctx.channel());
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
                    // формат: старое_имя & новое_имя
                    String[] str=new String(bytes, StandardCharsets.UTF_8).split(CloudUtil.STRING_DELIMITER);
                    String oldName=str[0];
                    String newName=str[1];
//                    Path pathOld=Path.of(".", "server_repository", oldName);
//                    Path pathNew=Path.of(".", "server_repository", newName);
                    Path pathOld=rootPath.resolve(oldName);
                    Path pathNew=rootPath.resolve(newName);
                    Path result=Files.move(pathOld, pathNew, StandardCopyOption.REPLACE_EXISTING);
                    if(Files.exists(result)){
                        System.out.println("Файл: " + pathOld + " переименован в: " + pathNew);
                    }
                    sendFileListToClient(ctx.channel());
                    currentState= HandlerState.IDLE;
                }
            }

            if(currentState==HandlerState.COMMAND_GET_FILENAME_LENGTH){
                if(buf.readableBytes()>=4){
                    nextLength=buf.readInt();
                    currentState=HandlerState.COMMAND_GET_FILENAME;
                }
            }

            if(currentState==HandlerState.COMMAND_GET_FILENAME){
                if(buf.readableBytes()>=nextLength){
                    byte[] bytes=new byte[nextLength];
                    buf.readBytes(bytes);
                    String filename=new String(bytes, StandardCharsets.UTF_8);
                    currentState=HandlerState.IDLE;
//                    Path path=Path.of(".","server_repository");
                    Path path=rootPath.resolve(filename);
                    CloudUtil.sendFile(path, ctx.channel(), f->{
                        if (!f.isSuccess()) {
                            f.cause().printStackTrace();
                        }
                        if (f.isSuccess()) {
                            System.out.println("Файл: "+ path.getFileName() + " передан на клиент");
                        }
                    });
                }
            }

        }


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

    private void sendFileListToClient(Channel channel){
        CloudUtil.sendFileListInDir(rootPath, channel, f->{
            if (!f.isSuccess()) {
                f.cause().printStackTrace();
            }
            if (f.isSuccess()) {
                System.out.println("Список файлов успешно передан на клиент");
            }
        });
    }
}
