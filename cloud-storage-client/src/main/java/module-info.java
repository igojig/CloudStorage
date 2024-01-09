module ru.igojig.cloud.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.netty.all;

    requires ru.igojig.cloud.common;

    requires jdk.unsupported;
    requires org.apache.logging.log4j;


    exports ru.igojig.client;
    opens ru.igojig.client to javafx.fxml;

    exports ru.igojig.client.controller;
    opens ru.igojig.client.controller to javafx.fxml;
    opens ru.igojig.client.Network to javafx.fxml;
    exports ru.igojig.client.Network;

}