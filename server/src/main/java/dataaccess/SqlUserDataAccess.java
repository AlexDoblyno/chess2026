package dataaccess;

import models.UserData;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlUserDataAccess implements UserDataAccess, SqlAccess {

    public SqlUserDataAccess () {
        try {
            configureDatabase();
        } catch (ServerException | DataAccessException e) {
            return;
        }
    }

    @Override
    public UserData getUserData(String username) throws ServerException {
        Connection conn;
        try{
            conn = DatabaseManager.getConnection();
        } catch (DataAccessException e) {
            throw new ServerException("Error Userdata get failed: " + e.getMessage());
        }
        var fetch = "SELECT * FROM UserData WHERE username = ?";
        try {
            UserData response = getUserDataFetch(username, conn, fetch);
            if (response != null && response.username() != null && response.password() != null && response.email() != null) {
                return response;
            }
        } catch (SQLException e) {
            if(e.getMessage().contains("not found")){
                return null;
            } else {
                throw new ServerException("Error: Userdata get failed: " + e.getMessage());
            }
        }
        return null;
    }

    private static UserData getUserDataFetch(String username, Connection conn, String fetch) throws SQLException {
        try (var preparedStatement = conn.prepareStatement(fetch)) {
            preparedStatement.setString(1, username);
            try (var response = preparedStatement.executeQuery()) {
                if (response.next()) {
                    return new UserData(response.getString("username"),
                            response.getString("password"),
                            response.getString("email"));
                } else {
                    throw new SQLException("User not found");
                }
            }
        }
    }

    @Override
    public void addUserData(UserData userData) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var insert = "INSERT INTO UserData (username, password, email) VALUES (?, ?, ?)";
            try (var preparedStatement = conn.prepareStatement(insert)) {
                preparedStatement.setString(1, userData.username());
                preparedStatement.setString(2, userData.password());
                preparedStatement.setString(3, userData.email());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Userdata add failed: " + e.getMessage());
        }
    }

    @Override
    public void clearUsers() throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var clear = "DELETE FROM UserData";
            try (var preparedStatement = conn.prepareStatement(clear)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("UserData clear failed: " + e.getMessage());
        }
    }

    private final String[] createStatements = {
            """
                CREATE TABLE IF NOT EXISTS  UserData (
                `username` varChar(256) NOT NULL PRIMARY KEY,
                `password` varChar(60) NOT NULL,
                `email` varChar(256) NOT NULL UNIQUE
                )
            """
    };

    // 这里已经删除了 executeUpdate 方法，它会自动继承接口里的默认方法！

    @Override
    public void configureDatabase() throws ServerException, DataAccessException {
        // 直接调用接口里写好的默认逻辑
        configureDatabase(createStatements);
    }
}