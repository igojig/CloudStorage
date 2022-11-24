module cloud.storage.server {

//    requires cloud.storage.common;

    requires jdk.unsupported;

    requires io.netty.all;

    requires org.xerial.sqlitejdbc;
    requires cloud.storage.common;
    requires org.apache.logging.log4j;
    exports ru.igojig.server;
    exports ru.igojig.server.handlers;
    exports ru.igojig.server.callback;
}