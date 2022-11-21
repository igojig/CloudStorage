module cloud.storage.server {
//    requires io.netty.transport;
//    requires io.netty.buffer;
    requires cloud.storage.common;
    requires io.netty.all;
    requires org.xerial.sqlitejdbc;
    exports ru.igojig.server;
    exports ru.igojig.server.handlers;
    exports ru.igojig.server.callback;
}