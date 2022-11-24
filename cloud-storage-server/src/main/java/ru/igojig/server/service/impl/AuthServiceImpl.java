package ru.igojig.server.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.igojig.server.service.AuthService;

import java.net.URL;
import java.sql.*;
import java.util.Optional;

public class AuthServiceImpl implements AuthService {

    private static final Logger logger= LogManager.getLogger(AuthServiceImpl.class);

    private final String getUsernameByLoginAndPasswordSQL = "SELECT username FROM users WHERE login=? AND password=?";
    private Connection connection;

    public AuthServiceImpl() {
        try {
            Class.forName("org.sqlite.JDBC");
            logger.info("Драйвер sqlite загружен");
        } catch (ClassNotFoundException e) {
            logger.throwing(e);
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
            logger.info("Объект Connection получен");
        } catch (SQLException e) {
            logger.throwing(e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public Optional<String> getUsernameByLoginAndPassword(String login, String password) {
        try( PreparedStatement ps= connection.prepareStatement(getUsernameByLoginAndPasswordSQL)){
            ps.setString(1, login);
            ps.setString(2, password);
            logger.trace(ps.toString());
            try(ResultSet rs=ps.executeQuery()){
                if(rs.next()){
                    String username=rs.getString(1);
                    return Optional.of(username);
                }
            }
        }
        catch (SQLException e){
            logger.throwing(e);
        }

        return Optional.empty();
    }

    @Override
    public void closeConnection() {
        if(connection!=null){
            try {
                connection.close();
                logger.info("Соединение с БД закрыто");
            }
            catch (SQLException e){
                logger.throwing(e);
            }
        }
    }
}
