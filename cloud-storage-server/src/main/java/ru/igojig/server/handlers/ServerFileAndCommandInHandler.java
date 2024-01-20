package ru.igojig.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.igojig.common.Command;
import ru.igojig.common.HandlerState;
import ru.igojig.common.Header;
import ru.igojig.common.fileutils.FileUtils;
import ru.igojig.common.protocol.ProtocolUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.Arrays;

public class ServerFileAndCommandInHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(ServerFileAndCommandInHandler.class);

    private final String username;

    private HandlerState currentState = HandlerState.IDLE;
    private int nextLength; // длина следующей получаемой части
    private long fileLength;
    private long receivedFileLength;
    private String fileName;
    private BufferedOutputStream out;
    Path rootPath = Path.of(".", "server_repository");
    DecimalFormat decimalFormat = new DecimalFormat();

    {
        decimalFormat.setGroupingSize(3);
    }

    public ServerFileAndCommandInHandler(String username) {
        this.username = username;
        // добавляем в rootPath имя пользователя
        rootPath = rootPath.resolve(username);
        rootPath=rootPath.normalize();
        FileUtils.createUserDir(rootPath, obj -> logger.info((String) obj[0]));
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Канал активен: " + ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("Пользователь " + username + " отключился. " + ctx);
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
                    logger.info(Header.FILE);
                    // переходим в состояние получения файла
                    currentState = HandlerState.FILE_NAME_LENGTH;
                    nextLength = 0;
                    fileLength = 0;
                    receivedFileLength = 0L;
                    fileName = null;
                } else if (controlByte == Header.FILE_LIST.getHeader()) {
                    logger.info(Header.FILE_LIST);
                    // переходим в состояние получения списка файлов с клиента
                    currentState = HandlerState.FILE_LIST_LENGTH;
                    nextLength = 0;
                } else if (controlByte == Header.COMMAND.getHeader()) {
                    logger.info(Header.COMMAND);
                    // переходим в состояние получения команды от клиента
                    currentState = HandlerState.COMMAND;
                } else {
                    logger.error("Неизвестный тип заголовка: " + controlByte);
                    buf.release();
                    return;
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
                    fileName = new String(bytes, StandardCharsets.UTF_8);
                    Path path = rootPath.resolve(fileName);
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
                }
            }
            // ждем файл
            if (currentState == HandlerState.FILE) {
                // если файл нулевой длины
                if (fileLength == 0) {
                    logger.info("Файл: " + fileName + " принят. Размер: " + fileLength);
                    currentState = HandlerState.IDLE;
                    out.close();
                    // получили файл
                    // передаем клиенту список файлов
                    sendFileListToClient(ctx.channel());
                }
                else {
                    while (buf.readableBytes() > 0) {
                        byte readed = buf.readByte();
                        out.write(readed);
                        receivedFileLength++;
                        if (receivedFileLength == fileLength) {
                            logger.info("Файл: " + fileName + " принят. Размер: " + decimalFormat.format(fileLength));
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
            if (currentState == HandlerState.FILE_LIST_LENGTH) {
                // ждем список файлов от клиента
                // получаем размер списка - int - 4 byte
                if (buf.readableBytes() >= 4) {
                    nextLength = buf.readInt();
                    currentState = HandlerState.FILE_LIST;
                }
            }

            if (currentState == HandlerState.FILE_LIST) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] bytes = new byte[nextLength];
                    buf.readBytes(bytes);
                    String[] fileList = new String(bytes, StandardCharsets.UTF_8).split(ProtocolUtils.TOKEN_DELIMITER);
                    logger.info("Получили список файлов от клиента");
                    logger.trace(Arrays.toString(fileList));
                    currentState = HandlerState.IDLE;
                }
            }
//----------------------------------------------------------------
// получили команду -> прокидываем в следующий Handler??????

            if (currentState == HandlerState.COMMAND) {
                logger.trace("Получили команду:" + currentState);
                if (buf.readableBytes() > 0) {
                    // вычитывем команду
                    byte command = buf.readByte();
                    if (command == Command.RENAME.getCommand()) {
                        logger.info("Запрос от клиента: " + Command.RENAME);
                        currentState = HandlerState.COMMAND_RENAME_LENGTH;
                        nextLength = 0;
                        // запрос списка файлов от клиента
                    } else if (command == Command.GET_FILE_LIST.getCommand()) {
                        logger.info("Запрос от клиента: " + Command.GET_FILE_LIST);
                        currentState = HandlerState.IDLE;
                        // отправили списрк файлов
                        sendFileListToClient(ctx.channel());

                    } else if (command == Command.GET_FILE.getCommand()) {
                        // получили команду на запрос файла [длина_имени_файда][имя_файла]
                        logger.info("Запрос от клиента: " + Command.GET_FILE);
                        currentState = HandlerState.COMMAND_GET_FILENAME_LENGTH;
                        nextLength = 0;

                    } else if (command == Command.DELETE.getCommand()) {
                        // получили команду на удаление
                        logger.info("Запрос от клиента: " + Command.DELETE);
                        currentState = HandlerState.COMMAND_DELETE_LENGTH;
                    } else {
                        logger.error("Неизвестная команда: " + command);
                        buf.release();
                        return;

                    }
                }
            }

            if (currentState == HandlerState.COMMAND_DELETE_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    nextLength = buf.readInt();
                    currentState = HandlerState.COMMAND_DELETE;
                }
            }

            if (currentState == HandlerState.COMMAND_DELETE) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] bytes = new byte[nextLength];
                    buf.readBytes(bytes);
                    String fileToDelete = new String(bytes, StandardCharsets.UTF_8);
                    Path path = rootPath.resolve(fileToDelete);
                    currentState = HandlerState.IDLE;
                    try {
                        Files.delete(path);
                        logger.info("Удалили файл: " + path);
                    }
                    catch (IOException e){
                        logger.throwing(e);
                    }
                    sendFileListToClient(ctx.channel());
                }
            }

            if (currentState == HandlerState.COMMAND_RENAME_LENGTH) {
                // читаем длину пакета
                if (buf.readableBytes() >= 4) {
                    nextLength = buf.readInt();
                    currentState = HandlerState.COMMAND_RENAME;
                }
            }

            if (currentState == HandlerState.COMMAND_RENAME) {
                // читаем пакет
                if (buf.readableBytes() >= nextLength) {
                    byte[] bytes = new byte[nextLength];
                    buf.readBytes(bytes);
                    // формат: старое_имя & новое_имя
                    String[] str = new String(bytes, StandardCharsets.UTF_8).split(ProtocolUtils.TOKEN_DELIMITER);
                    String oldName = str[0];
                    String newName = str[1];
                    Path pathOld = rootPath.resolve(oldName);
                    Path pathNew = rootPath.resolve(newName);
                    try {
                        Path result = Files.move(pathOld, pathNew, StandardCopyOption.REPLACE_EXISTING);
                        logger.info("Файл: " + pathOld + " переименован в: " + pathNew);
                    } catch (IOException e) {
                        logger.throwing(e);
                    }

                    sendFileListToClient(ctx.channel());
                    currentState = HandlerState.IDLE;
                }
            }

            if (currentState == HandlerState.COMMAND_GET_FILENAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    nextLength = buf.readInt();
                    currentState = HandlerState.COMMAND_GET_FILENAME;
                }
            }

            if (currentState == HandlerState.COMMAND_GET_FILENAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] bytes = new byte[nextLength];
                    buf.readBytes(bytes);
                    String filename = new String(bytes, StandardCharsets.UTF_8);
                    currentState = HandlerState.IDLE;
                    Path path = rootPath.resolve(filename);
                    ProtocolUtils.sendFile(path, ctx.channel(),
                            f -> {
                                if (!f.isSuccess()) {
                                    logger.throwing(f.cause());
                                }
                                if (f.isSuccess()) {
                                    logger.info("Файл: " + path.getFileName() + " передан на клиент");
                                }
                            }, null);
                }
            }

        }


        if (buf.readableBytes() == 0) {
            buf.release();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.throwing(cause);
        ctx.close();
    }

    private void sendFileListToClient(Channel channel) {
        ProtocolUtils.sendFileListInDir(rootPath, channel, f -> {
            if (!f.isSuccess()) {
                logger.throwing(f.cause());
            }
            if (f.isSuccess()) {
                logger.info("Список файлов успешно передан на клиент");
            }
        });
    }


}
