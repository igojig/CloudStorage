package ru.igojig.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.igojig.common.CloudUtil;
import ru.igojig.common.HandlerState;
import ru.igojig.common.Header;
import ru.igojig.server.callback.AuthCallback;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AuthInHandler extends ChannelInboundHandlerAdapter {

    private static Set<String> userSet = new HashSet<>();

    enum AuthStatus {
        AUTH,
        AUTH_NOT
    }

    private AuthStatus authStatus = AuthStatus.AUTH_NOT;

    private final AuthCallback authCallback;

    private HandlerState currentState = HandlerState.IDLE;

    private int nextLength;
    String username = null;

    public AuthInHandler(AuthCallback authCallback) {
        this.authCallback = authCallback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Пользователь подключился: " + ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        userSet.remove(username);
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        if (authStatus == AuthStatus.AUTH_NOT) {
            if (byteBuf.readableBytes() > 0) {
                if (currentState == HandlerState.IDLE) {

                    byte controlByte = byteBuf.readByte();
                    if (controlByte == Header.AUTH_REQUEST.getHeader()) {
                        currentState = HandlerState.AUTH_LENGTH;
                    } else {
                        System.out.println("Неизвестный тип заголовка авторизации");
                        // релизим буфер
                        byteBuf.release();
                        return;
                    }
                }

                if (currentState == HandlerState.AUTH_LENGTH) {
                    if (byteBuf.readableBytes() >= 4) {
                        nextLength = byteBuf.readInt();
                        currentState = HandlerState.AUTH;
                    }
                }

                if (currentState == HandlerState.AUTH) {
                    if (byteBuf.readableBytes() >= nextLength) {
                        byte[] authBytes = new byte[nextLength];
                        byteBuf.readBytes(authBytes);
                        String strAuth = new String(authBytes, StandardCharsets.UTF_8);
                        String[] arr = strAuth.split(CloudUtil.STRING_DELIMITER);
                        String login = arr[0];
                        String password = arr[1];

                        // получаем пользователя через callback
                        Optional<String> optUser = authCallback.authCallback(login, password);
                        if (optUser.isPresent()) {
                            username = optUser.get();
                            if (!userSet.contains(username)) {
                                authStatus = AuthStatus.AUTH;
                                userSet.add(username);
                                CloudUtil.sendAuthOk(username, ctx.channel(), f -> {
                                    if (!f.isSuccess()) {
                                        f.cause().printStackTrace();
                                    }
                                    if (f.isSuccess()) {
                                        System.out.println("Сообщение об успешной авторизации передано на клиент");
                                    }
                                });

                                ctx.pipeline().addLast(new ServerFirstInHandler(username));

                                System.out.println("Подключился пользователь:" + username);
                            } else {
                                CloudUtil.sendAuthErr(ctx.channel(), f -> {
                                    if (!f.isSuccess()) {
                                        f.cause().printStackTrace();
                                    }
                                    if (f.isSuccess()) {
                                        System.out.println("Сообщение об ошибочной авторизации передано на клиент");
                                    }
                                });
                                System.out.printf("Пользователь %s уже присутствует в системе%n", username);
                            }

                        } else {
                            CloudUtil.sendAuthErr(ctx.channel(), f -> {
                                if (!f.isSuccess()) {
                                    f.cause().printStackTrace();
                                }
                                if (f.isSuccess()) {
                                    System.out.println("Сообщение об ошибочной авторизации передано на клиент");
                                }
                            });
                            System.out.println("Ошибка авторизации. Login: " + login + " Password: " + password);
                        }
                        currentState = HandlerState.IDLE;
                    }
                }

            }
//
//            byteBuf.release();
        }
        if (authStatus == AuthStatus.AUTH) {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        cause.printStackTrace();
        ctx.close();
    }
}
