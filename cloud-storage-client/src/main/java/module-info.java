module cloud.storage.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.netty.all;
//    requires io.netty.transport;
//    requires io.netty.buffer;
    requires cloud.storage.common;



    exports ru.igojig.client;
    opens ru.igojig.client to javafx.fxml;

    exports ru.igojig.client.controller;
    opens ru.igojig.client.controller to javafx.fxml;


}