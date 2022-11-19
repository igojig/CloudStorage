package ru.igojig.client.controller;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import ru.igojig.client.Network;
import ru.igojig.common.CloudUtil;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    public ListView<String> lstClient;
    public ListView<String> lstServer;

    public Button btnSendToServer;
    public Button btnSendToClient;
    public Button btnServerRename;
    public Button btnServerDelete;
    public Label lblClientCount;
    public Label lblServerCount;
    public ProgressBar progressBar;
    public Button btnServerUpdate;
    public Button btnClientUpdate;
    public Button btnClientRename;
    public Button btnClientDelete;

    //=================================================

    List<Control> controls=new ArrayList<>();


    ObservableList<String> clientObservableList;
    ObservableList<String> serverObservableList;

    // выбранные в ListView файлы
    String selectedClientFile;
    String selectedServerFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        // кнопки которые блокируются до прихода ответа с серевера или клиента
        controls.add(btnServerUpdate);
        controls.add(btnSendToServer);
        controls.add(btnServerDelete);
        controls.add(btnServerRename);
        controls.add(btnSendToClient);
        controls.add(btnClientUpdate);
        controls.add(btnClientRename);
        controls.add(btnClientDelete);

        clientObservableList = lstClient.getItems();
        serverObservableList = lstServer.getItems();

        setCellFactoryForViews();

        serverObservableList.addListener((ListChangeListener<String>) c -> lblServerCount.setText(String.valueOf(serverObservableList.size())));
        clientObservableList.addListener((ListChangeListener<String>) c -> lblClientCount.setText(String.valueOf(clientObservableList.size())));

        updateClientFileList();
    }

    private void setCellFactoryForViews() {
        lstClient.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = lstClient.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                lstClient.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedClientFile = null;
                    } else {
                        selectionModel.select(index);
                        selectedClientFile = cell.getItem();
                    }
                    event.consume();
                }
            });
            return cell;
        });

        lstServer.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = lstServer.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                lstServer.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedServerFile = null;
                    } else {
                        selectionModel.select(index);
                        selectedServerFile = cell.getItem();
                    }
                    event.consume();
                }
            });
            return cell;
        });
    }


    public void updateServerFileList(List<String> stringList){
        serverObservableList.clear();
        selectedServerFile=null;
        serverObservableList.addAll(stringList);
        if(stringList.isEmpty()){
            lblServerCount.setText("0");
        }
        enableButtons();
    }

    public void updateClientFileList(){

        clientObservableList.clear();
        selectedClientFile=null;
        List<String> list=CloudUtil.getFileListInDir(Path.of(".", "client_repository"));
        clientObservableList.addAll(list);
        if(list.isEmpty()){
            lblClientCount.setText("0");
        }
        enableButtons();
    }




    public void onSendToServer(ActionEvent actionEvent) throws IOException {

        if(checkClientFileIsSelected()){
            return;
        }

        disableButtons();

        System.out.println(selectedClientFile);
        Path path=Path.of(".", "client_repository");
        path=path.resolve(selectedClientFile);
        System.out.println(path);
        Path finalPath = path;
        CloudUtil.sendFile(path, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл: " + finalPath.getFileName().toString() + " передан на сервер" );
            }
        } );
    }

    public void onSendToClient(ActionEvent actionEvent) {
        if(checkServerFileIsSelected()){
            return;
        }
        disableButtons();
        System.out.println(selectedServerFile);
        CloudUtil.sendCommandFileRequest(selectedServerFile, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Отправлен запрос на файл: " + selectedServerFile );
            }
        } );
    }

    public void onServerRename(ActionEvent actionEvent) {
       if(checkServerFileIsSelected()){
           return;
       }

        disableButtons();

        String newFileName=null;
        TextInputDialog dialog = new TextInputDialog("new_file");
        dialog.setTitle("Переименование файла на сервере");
        dialog.setHeaderText("Файл: " + selectedServerFile);
        dialog.setContentText("Введите новое имя:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            newFileName= result.get();
        } else {
            return;
        }

        String finalNewFileName = newFileName;
        CloudUtil.sendCommandRenameFile(selectedServerFile, newFileName, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл: " + selectedServerFile + " переименован: " + finalNewFileName);
            }
        } );

    }

    public void onServerDelete(ActionEvent actionEvent) {
        if(checkServerFileIsSelected()){
            return;
        }

        disableButtons();

        CloudUtil.sendCommandDeleteFile(selectedServerFile, Network.getInstance().getCurrentChannel(),future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Файл: " + selectedServerFile + " удален на сервере");
            }
        } );


    }

    public boolean checkServerFileIsSelected(){
        if(selectedServerFile==null || selectedServerFile.equals("")){
            Alert a=new Alert(Alert.AlertType.ERROR, "Выберите файл");
            a.show();
            return true;
        }
        return false;
    }

    public boolean checkClientFileIsSelected(){
        if(selectedClientFile==null || selectedClientFile.equals("")){
            Alert a=new Alert(Alert.AlertType.ERROR, "Выберите файл");
            a.show();
            return true;
        }
        return false;
    }

    public void disableButtons(){
        for(Control control:controls){
            control.setDisable(true);
        }
    }

    public void enableButtons(){
        for(Control control:controls){
            control.setDisable(false);
        }
    }

    public void updateProgressBar(double value){
        progressBar.setProgress(value);
    }

    public void onBtnServerUpdate(ActionEvent actionEvent) {
        disableButtons();
        CloudUtil.requestFileList(Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("Запросили список файлов");
            }
        });
    }

    public void onBtnClientUpdate(ActionEvent actionEvent) {
        disableButtons();
        updateClientFileList();
    }

    public void onClientRename(ActionEvent actionEvent) {
        if(checkClientFileIsSelected()){
            return;
        }
        disableButtons();
        String oldFileName=selectedClientFile;
        String newFileName=null;
        TextInputDialog dialog = new TextInputDialog("new_file");
        dialog.setTitle("Переименование файла на клиенте");
        dialog.setHeaderText("Файл: " + selectedClientFile);
        dialog.setContentText("Введите новое имя:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            newFileName= result.get();
        } else {
            return;
        }

        Path oldPath=Path.of(".", "client_repository", oldFileName);
        Path newPath=Path.of(".", "client_repository", newFileName);
        try {
            Files.move(  oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Переименование не удалось");
        }
        updateClientFileList();
    }

    public void onClientDelete(ActionEvent actionEvent) {
        if(checkClientFileIsSelected()){
            return;
        }
        disableButtons();
        Path path=Path.of(".", "client_repository", selectedClientFile);
        try {
            Files.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Не удалось удалить файл");
        }

        updateClientFileList();

    }
}
