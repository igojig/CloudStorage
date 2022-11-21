package ru.igojig.server.service.impl;

import ru.igojig.server.service.AuthService;

import java.net.URL;
import java.sql.*;
import java.util.Optional;

public class AuthServiceImpl implements AuthService {

    private final String getUsernameByLoginAndPasswordSQL = "SELECT username FROM users WHERE login=? AND password=?";
    private Connection connection;

    public AuthServiceImpl() {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("Драйвер sqlite загружен");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void openConnection() {
        String connectionStr = "jdbc:sqlite:";

        URL url = AuthServiceImpl.class.getResource("cloud_users.db");

        String databaseStr = url.toString();
        connectionStr += databaseStr;
        try {
            connection = DriverManager.getConnection(connectionStr);
            System.out.println("Объект Connection получен");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @Override
    public Optional<String> getUsernameByLoginAndPassword(String login, String password) {
        try( PreparedStatement ps= connection.prepareStatement(getUsernameByLoginAndPasswordSQL)){
            ps.setString(1, login);
            ps.setString(2, password);
            try(ResultSet rs=ps.executeQuery()){
                if(rs.next()){
                    String username=rs.getString(1);
                    return Optional.of(username);
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public void closeConnection() {
        if(connection!=null){
            try {
                connection.close();
                System.out.println("Соединение с БД закрыто");
            }
            catch (SQLException e){
                e.printStackTrace();
            }
        }
    }
}
