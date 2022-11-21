package ru.igojig.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import ru.igojig.common.CloudUtil;
import ru.igojig.server.callback.AuthCallback;
import ru.igojig.server.handlers.AuthInHandler;
import ru.igojig.server.service.AuthService;
import ru.igojig.server.service.impl.AuthServiceImpl;


public class ServerApp  {

    AuthService authService;

    AuthCallback authCallback=((login, password) -> authService.getUsernameByLoginAndPassword(login, password));

    public ServerApp(){
        authService=new AuthServiceImpl();
    }

    public void run() throws Exception {


        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast( new AuthInHandler(authCallback));
                        }
                    });
            // .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(CloudUtil.PORT).sync();
            System.out.println("Server started");
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            closeDBConnection();
        }
    }

    public void connectToDatabase(){

        authService.openConnection();
    }

    public void closeDBConnection(){
        authService.closeConnection();
    }


    public static void main(String[] args) throws Exception {

        ServerApp serverApp=new ServerApp();
        serverApp.connectToDatabase();

//        String str=serverApp.authService.getUsernameByLoginAndPassword("petrov", "1").orElse("not found");
//        System.out.println(str);
//        serverApp.closeConnection();
        serverApp.run();
    }
}
