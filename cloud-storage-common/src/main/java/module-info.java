module ru.igojig.cloud.common {
    requires io.netty.all;

    requires jdk.unsupported;
    requires org.apache.logging.log4j;
//    requires io.netty.buffer;
//    requires io.netty.transport;
    exports ru.igojig.common;
    exports ru.igojig.common.callback;
    exports ru.igojig.common.fileutils;
    exports ru.igojig.common.protocol;
}