package ru.igojig.client.controller;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.igojig.client.Network.Network;
import ru.igojig.common.callback.ProgressBarAction;
import ru.igojig.common.callback.ProtoCallback;
import ru.igojig.common.fileutils.FileUtils;
import ru.igojig.common.protocol.ProtocolUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


public class ClientController implements Initializable {

    private static final Logger logger = LogManager.getLogger(ClientController.class);

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
    //окно авторизации
    //-------------------------------
    public HBox hboxAuth;
    public TextField txtLogin;
    public TextField txtPassword;
    public Button btnLogin;
    public Label lblUsername;
    public TextArea txtMessage;
    public BorderPane mainPane;
    //-----------------------------
    public VBox vboxCenter;
    public VBox vboxLeft;
    public VBox vboxRight;
    public VBox vboxBottom;
    public Button btnAddFile;
    //_________________________________

    private String username; // приходит после успешной авторизации

    //=================================================

    List<Control> blockControls = new ArrayList<>();
    List<Pane> hideblePanes = new ArrayList<>();


    ObservableList<String> clientObservableList;
    ObservableList<String> serverObservableList;

    // выбранные в ListView файлы
    String selectedClientFile;
    String selectedServerFile;

    Path rootClientPath = Path.of(".", "client_repository");


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.trace("initialize controller");


        // кнопки, которые блокируются до прихода ответа с сервера или клиента
//        setBlockControls();

        setHidePanes();


        clientObservableList = lstClient.getItems();
        serverObservableList = lstServer.getItems();

        setCellFactoryForViews();

        serverObservableList.addListener((ListChangeListener<String>) c -> lblServerCount.setText(String.valueOf(serverObservableList.size())));
        clientObservableList.addListener((ListChangeListener<String>) c -> lblClientCount.setText(String.valueOf(clientObservableList.size())));

        hideblePanes.forEach(o -> {
            o.setVisible(false);
        });


//        updateClientFileList();
    }

    private void setHidePanes() {
        hideblePanes.add(vboxCenter);
        hideblePanes.add(vboxLeft);
        hideblePanes.add(vboxRight);
    }

    // скрываем кнопки после отправки файлов
//    private void setBlockControls() {
//        blockControls.add(btnServerUpdate);
//        blockControls.add(btnSendToServer);
//        blockControls.add(btnServerDelete);
//        blockControls.add(btnServerRename);
//        blockControls.add(btnSendToClient);
//        blockControls.add(btnClientUpdate);
//        blockControls.add(btnClientRename);
//        blockControls.add(btnClientDelete);
//    }

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


    public void updateServerFileList(List<String> stringList) {
        serverObservableList.clear();
        selectedServerFile = null;
        serverObservableList.addAll(stringList);
        if (stringList.isEmpty()) {
            lblServerCount.setText("0");
        }
        txtMessage.appendText("Получили список файлов от сервера\n");

//        enableButtons();
    }

    public void updateClientFileListWithFileInfo(String fiilename, long filesize) {

        DecimalFormat dc = new DecimalFormat();
        dc.setGroupingSize(3);
        txtMessage.appendText(String.format("Приняли файл:[%s], размер[%s]%n", fiilename, dc.format(filesize)));

        updateClientFileList();
    }

    public void updateClientFileList() {
        clientObservableList.clear();
        selectedClientFile = null;
        List<String> list = FileUtils.getFileListInDir(rootClientPath);
        clientObservableList.addAll(list);
        if (list.isEmpty()) {
            lblClientCount.setText("0");
        }
//        enableButtons();
    }


    public void onSendToServer(ActionEvent actionEvent) throws IOException {

        if (checkClientFileIsSelected()) {
            return;
        }

        Path path = rootClientPath.resolve(selectedClientFile);
        ProtocolUtils.sendFile(path, Network.getInstance().getCurrentChannel(),
                future -> {
                    if (!future.isSuccess()) {
                        logger.throwing(future.cause());
                    }
                    if (future.isSuccess()) {
                        logger.info("Файл: " + path.getFileName().toString() + " передан на сервер");
                        Platform.runLater(() -> txtMessage.appendText("Файл: " + path.getFileName().toString() + " передан на сервер\n"));

                    }
                },
                (partLength, fullLength) -> Platform.runLater(() -> updateProgressBar(partLength, fullLength)));
    }

    public void onSendToClient(ActionEvent actionEvent) {
        if (checkServerFileIsSelected()) {
            return;
        }

        ProtocolUtils.sendCommandGetFile(selectedServerFile, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                logger.throwing(future.cause());
            }
            if (future.isSuccess()) {
                logger.info("Отправлен запрос на файл: " + selectedServerFile);
                Platform.runLater(() -> txtMessage.appendText("Отправлен запрос на файл: " + selectedServerFile + "\n"));
            }
        });
    }

    public void onServerRename(ActionEvent actionEvent) {
        if (checkServerFileIsSelected()) {
            return;
        }

//        disableButtons();

        String newFileName = null;
        TextInputDialog dialog = new TextInputDialog("new_file");
        dialog.setTitle("Переименование файла на сервере");
        dialog.setHeaderText("Файл: " + selectedServerFile);
        dialog.setContentText("Введите новое имя:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            newFileName = result.get();
        } else {
            return;
        }

        String finalNewFileName = newFileName;
        ProtocolUtils.sendCommandRenameFile(selectedServerFile, newFileName, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                logger.throwing(future.cause());
            }
            if (future.isSuccess()) {
                logger.info("Файл: " + selectedServerFile + " переименован на сервере: " + finalNewFileName);
                Platform.runLater(() -> txtMessage.appendText("Файл: " + selectedServerFile + " переименован на сервере: " + finalNewFileName + "\n"));
            }
        });

    }

    public void onServerDelete(ActionEvent actionEvent) {
        if (checkServerFileIsSelected()) {
            return;
        }

//        disableButtons();

        ProtocolUtils.sendCommandDeleteFile(selectedServerFile, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                logger.throwing(future.cause());
            }
            if (future.isSuccess()) {
                logger.info("Файл: " + selectedServerFile + " удален на сервере");
                Platform.runLater(() -> txtMessage.appendText("Файл: " + selectedServerFile + " удален на сервере\n"));
            }
        });


    }

    public boolean checkServerFileIsSelected() {
        if (selectedServerFile == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Выберите файл");
            a.show();
            return true;
        }
        return false;
    }

    public boolean checkClientFileIsSelected() {
        if (selectedClientFile == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Выберите файл");
            a.show();
            return true;
        }
        return false;
    }

    public void disableButtons() {
        for (Control control : blockControls) {
            control.setDisable(true);
        }
    }

    public void enableButtons() {
        for (Control control : blockControls) {
            control.setDisable(false);
        }
    }

    public void updateProgressBar(double received, double fullLength) {
        progressBar.setProgress(received / fullLength);
    }

    public void onBtnServerUpdate(ActionEvent actionEvent) {

//        disableButtons();

        ProtocolUtils.requestFileList(Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                logger.throwing(future.cause());
            }
            if (future.isSuccess()) {
                logger.info("Запросили список файлов");
                Platform.runLater(() -> txtMessage.appendText("Запросили список файлов\n"));
            }
        });
    }

    public void onBtnClientUpdate(ActionEvent actionEvent) {
//        disableButtons();
        updateClientFileList();
    }

    public void onClientRename(ActionEvent actionEvent) {
        if (checkClientFileIsSelected()) {
            return;
        }

//        disableButtons();

        String oldFileName = selectedClientFile;
        String newFileName = null;
        TextInputDialog dialog = new TextInputDialog("new_file");
        dialog.setTitle("Переименование файла на клиенте");
        dialog.setHeaderText("Файл: " + selectedClientFile);
        dialog.setContentText("Введите новое имя:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            newFileName = result.get();
        } else {
            return;
        }

        Path oldPath = rootClientPath.resolve(oldFileName);
        Path newPath = rootClientPath.resolve(newFileName);
        try {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            txtMessage.appendText(String.format("Файл %s переименован в %s%n", oldPath, newPath));
            logger.info(String.format("Файл %s переименован в %s", oldPath, newPath));
        } catch (IOException e) {
            logger.throwing(e);
            logger.warn("Переименование не удалось: " + oldPath);
            Platform.runLater(() -> txtMessage.appendText("Переименование не удалось: " + oldPath + "\n"));

        }
        updateClientFileList();
    }

    public void onClientDelete(ActionEvent actionEvent) {
        if (checkClientFileIsSelected()) {
            return;
        }

//        disableButtons();

        Path path = rootClientPath.resolve(selectedClientFile);
        try {
            Files.delete(path);
            txtMessage.appendText(String.format("Файл %s удален%n", path));
            logger.info("Файл: " + path + " удален");
        } catch (IOException e) {
            logger.throwing(e);
            logger.warn("Не удалось удалить файл: " + path);
            txtMessage.appendText("Не удалось удалить файл: " + path);
            txtMessage.appendText("\n");

        }

        updateClientFileList();

    }

    public void onLogin(ActionEvent actionEvent) {

        String login = txtLogin.getText().strip();
        String password = txtPassword.getText().strip();
        if (login.isEmpty() || password.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Логин и пароль не должны быть пустыми");

            alert.showAndWait();
            return;
        }

        ProtocolUtils.sendAuth(login, password, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                logger.throwing(future.cause());
            }
            if (future.isSuccess()) {
                logger.info("Отправили запрос на авторизацию. Login: [" + login + "] Password: [" + password + "]");
                Platform.runLater(() -> txtMessage.appendText("Отправили запрос на авторизацию. Login: [" + login + "] Password: [" + password + "]" + "\n"));

            }
        });


    }

    public void onGetAuthOk(String username) {
        this.username = username;
        lblUsername.setText(username);
        txtMessage.appendText("Пользователь подключился как: " + username);
        txtMessage.appendText("\n");
        logger.info("Пользователь подключился как: " + username);
        hideLoginAndShowMainForm();

        // добавляем в rootPath имя пользователя
        rootClientPath = rootClientPath.resolve(username);
        rootClientPath = rootClientPath.normalize();

//        System.out.println(rootClientPath.toAbsolutePath());

        FileUtils.createUserDir(rootClientPath, (obj) -> {
            Platform.runLater(() -> txtMessage.appendText((String) obj[0] + "\n"));
            logger.info((String) obj[0]);
        });

        onBtnServerUpdate(null);
        onBtnClientUpdate(null);
    }

    private void hideLoginAndShowMainForm() {
        hideblePanes.forEach(o -> o.setVisible(true));
        hboxAuth.setVisible(false);
        hboxAuth.setManaged(false);
    }

    public void onAddFile(ActionEvent actionEvent) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open  File");
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile == null) {
            return;
        }
        logger.info("Добавляем файл: " + selectedFile.getName());
        Path selectedPath = selectedFile.toPath();
        Path newPath = rootClientPath.resolve(selectedPath.getFileName());

        ProtoCallback infoController = obj -> {
            Platform.runLater(() -> updateClientFileListWithFileInfo((String) obj[0], (Long) obj[1]));
        };

        ProgressBarAction setProgress = (part, fullLength) -> {
            Platform.runLater(() -> this.updateProgressBar(part, fullLength));
        };


        FileUtils.addExternalFile(selectedPath, newPath, infoController, setProgress);


//        Thread thread=new Thread(()->{
//            try( FileInputStream fis=new FileInputStream(selectedPath.toFile());
//                 BufferedInputStream bis=new BufferedInputStream(fis);
//
//                 FileOutputStream fos=new FileOutputStream(newPath.toFile());
//                 BufferedOutputStream bos=new BufferedOutputStream(fos);) {
//
//
//                int read=0;
//                long readedBytes=0;
//                byte[] buf=new byte[8192];
//                while((read = bis.read(buf))>0) {
//                    readedBytes+=read;
//                    bos.write(buf, 0, read);
//                    long finalReadedBytes = readedBytes;
//                    progressBar.setProgress(1.0* finalReadedBytes /length);
//                }
//                progressBar.setProgress(0.);
//                cloudCallback.callback(newPath.getFileName().toString(), length);
//            } catch (IOException e) {
//               e.printStackTrace();
//            }
//        });
//        thread.start();


//        Path path =selectedFile.toPath();
//        Path  newFile=path.getFileName();
//        newFile=rootClientPath.resolve(newFile);
//        try {
//            Files.copy(path, newFile, StandardCopyOption.REPLACE_EXISTING);
//            logger.info(String.format("Файл [%s] скопирован в [%s]", path.getFileName().toString(), newFile));
//        } catch (IOException e) {
//            logger.throwing(e);
//        }
//        updateClientFileList();
    }


    public String getUsername() {
        return username;
    }
}
