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

public class ClientApp extends  Application {

     ClientController clientController;

      Map<String, CloudCallback> cloudCallbackMap=new HashMap<>();



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

        clientController=fxmlLoader.getController();
        setCallbacks();
        // запрашиваем список файлов с сервера при открытии окна
        clientController.onBtnServerUpdate(null);

        CloudUtil.setCallback((a, b)->{
            clientController.updateProgressBar(b/a);
        });

    }

    private void setCallbacks() {
        cloudCallbackMap.put("GET_FILE", o->Platform.runLater(()->clientController.updateClientFileList()));

        cloudCallbackMap.put("GET_FILE_LIST", obj -> {
            Platform.runLater(()->{clientController.updateServerFileList(((List<String>)obj[0]));
//                clientController.enableButtons();
                });
            });

        cloudCallbackMap.put("PROGRESS_BAR", new CloudCallback() {
            @Override
            public void callback(Object... obj) {

                clientController.updateProgressBar((Double) obj[0]);
            }
        });

        Network.getInstance().getCurrentChannel().pipeline().get(ClientInHandler.class).setCloudCallbackMap(cloudCallbackMap);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();

        networkStarter.await();



        launch();


//        ByteBuf buf = null;
//        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
//        buf.writeByte(ProtocolState.FILE_LIST_REQUEST.getCommand());
//        Network.getInstance().getCurrentChannel().writeAndFlush(buf);



//        ProtoFileSender.sendFile(Paths.get("demo.txt"), Network.getInstance().getCurrentChannel(), future -> {
//            if (!future.isSuccess()) {
//                future.cause().printStackTrace();
////                Network.getInstance().stop();
//            }
//            if (future.isSuccess()) {
//                System.out.println("Файл успешно передан");
////                Network.getInstance().stop();
//            }
//        });
//        Thread.sleep(2000);
//        ProtoFileSender.sendFile(Paths.get("demo1.txt"), Network.getInstance().getCurrentChannel(), future -> {
//            if (!future.isSuccess()) {
//                future.cause().printStackTrace();
////                Network.getInstance().stop();
//            }
//            if (future.isSuccess()) {
//                System.out.println("Файл успешно передан");
////                Network.getInstance().stop();
//            }
//        });

//        List<String> filelist=List.of("привет.txt", "пока.txt", "один.txt", "два.txt",  "три.txt");


//        CloudUtil.sendFileListInDir(Path.of(".", "client_repository"), Network.getInstance().getCurrentChannel(), f->{
//            if (!f.isSuccess()) {
//                f.cause().printStackTrace();
////                Network.getInstance().stop();
//            }
//            if (f.isSuccess()) {
//                System.out.println("Список файлов успешно передан");
////                Network.getInstance().stop();
//            }
//        });

//        ProtoFileSender.sendFileList(filelist, Network.getInstance().getCurrentChannel(), f->{
//            if (!f.isSuccess()) {
//                f.cause().printStackTrace();
////                Network.getInstance().stop();
//            }
//            if (f.isSuccess()) {
//                System.out.println("Список файлов успешно передан");
////                Network.getInstance().stop();
//            }
//        });

//        ProtoFileSender.sendCommand(Network.getInstance().getCurrentChannel(), f->{
//            if (!f.isSuccess()) {
//                f.cause().printStackTrace();
////                Network.getInstance().stop();
//            }
//            if (f.isSuccess()) {
//                System.out.println("Файл перименован");
////                Network.getInstance().stop();
//            }
//        });

    }
}
