package ru.igojig.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.igojig.common.CloudUtil;
import ru.igojig.common.callback.CloudCallback;
import ru.igojig.client.controller.ClientController;
import ru.igojig.client.handlers.ClientInHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ClientApp extends Application {

    ClientController clientController;

    Map<String, CloudCallback> cloudCallbackMap = new HashMap<>();


    @Override
    public void stop() throws Exception {
        Network.getInstance().stop();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("client.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 700);
        primaryStage.setTitle("Network storage");
        primaryStage.setScene(scene);
        primaryStage.show();

        clientController = fxmlLoader.getController();
        // для InboundHandlerAdapter
        setCallbacks();
        // для CloudUtils
        CloudUtil.setCallback((a, b) -> {Platform.runLater(()->
            clientController.updateProgressBar(a, b)); // received fileLength
        });
        // запрашиваем список файлов с сервера при открытии окна
//        clientController.onBtnServerUpdate(null);


    }

    private void setCallbacks() {
        cloudCallbackMap.put("GET_FILE", o -> Platform.runLater(() -> clientController.updateClientFileListWithFileInfo((String) o[0], (Long) o[1])));

        cloudCallbackMap.put("GET_FILE_LIST", obj -> {
            Platform.runLater(() -> {
                clientController.updateServerFileList(((List<String>) obj[0]));
            });
        });

        cloudCallbackMap.put("AUTH_OK", o -> {
            Platform.runLater(() -> clientController.onGetAuthOk((String) o[0]));
        });

        cloudCallbackMap.put("AUTH_ERR", o -> {
            clientController.txtMessage.appendText("Ошибка авторизации");
            clientController.txtMessage.appendText("\n");
        });

//        cloudCallbackMap.put("PROGRESS_BAR", obj ->clientController.updateProgressBar((Double) obj[0], (Double) obj[1]));
        cloudCallbackMap.put("PROGRESS_BAR",obj ->Platform.runLater(()->clientController.updateProgressBar((Double) obj[0], (Double) obj[1])));

        Network.getInstance().getCurrentChannel().pipeline().get(ClientInHandler.class).setCloudCallbackMap(cloudCallbackMap);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();

        networkStarter.await();


        launch();

    }
}
