module cloud.storage.server {
//    requires io.netty.transport;
//    requires io.netty.buffer;
    requires cloud.storage.common;
    requires io.netty.all;

    exports ru.igojig.server;
}