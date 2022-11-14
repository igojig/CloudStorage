package ru.igojig.client;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import ru.igojig.common.CloudUtil;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    public ListView<String> lstClient;
    public ListView<String> lstServer;
    public Button btnRefresh;

    //=================================================

    ObservableList<String> clientObservableList;
    ObservableList<String> serverObservableList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        clientObservableList = lstClient.getItems();
        serverObservableList = lstServer.getItems();

        updateClientFileList();


    }


    public void updateServerFileList(List<String> stringList){
        serverObservableList.clear();
        serverObservableList.addAll(stringList);
    }

    public void updateClientFileList(){

        clientObservableList.clear();
        clientObservableList.addAll(CloudUtil.getFileListInDir(Path.of(".", "client_repository")));
    }


    public void onBtnRefresh(ActionEvent actionEvent) {
        CloudUtil.requestFileList(Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
//                Network.getInstance().stop();
            }
            if (future.isSuccess()) {
                System.out.println("Запросили список файлов");
//                Network.getInstance().stop();
            }
        });
    }
}
