package ru.igojig.client.Network;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.igojig.client.handlers.ClientInHandler;
import ru.igojig.common.protocol.ProtocolUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Network {

    private static final Logger logger= LogManager.getLogger(Network.class);
    private static final Network ourInstance = new Network();

    public static Network getInstance() {
        return ourInstance;
    }

    private Network() {
    }

    private  Channel currentChannel;

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void start(CountDownLatch countDownLatch) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(ProtocolUtils.HOST, ProtocolUtils.PORT))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast( new ClientInHandler());
                            currentChannel = socketChannel;

                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            logger.info("Client started");
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.throwing(e);
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                logger.throwing(e);
            }
        }
    }

    public void stop() {
        currentChannel.close();
        logger.info("Сетевое соединение закрыто");
    }
}
