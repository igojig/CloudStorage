module cloud.storage.common {
    requires io.netty.all;


    requires jdk.unsupported;
//    requires io.netty.buffer;
//    requires io.netty.transport;
    exports ru.igojig.common;
    exports ru.igojig.common.callback;
    exports ru.igojig.common.fileutils;
}