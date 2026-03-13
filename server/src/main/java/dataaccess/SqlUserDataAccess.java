package dataaccess;

import models.UserData;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlUserDataAccess implements UserDataAccess, SqlAccess {

    public SqlUserDataAccess () {
            try {
                configureDatabase();
            } catch (ServerException e) {
                return ;
            }
        }

    @Override
    public UserData getUserData(String username) throws ServerException, server.ServerException {
        Connection conn;
        try{
            conn = DatabaseManager.getConnection();
        } catch (DataAccessException e) {
            throw new ServerException("Error Userdata get failed: " + e.getMessage());
        }
        var fetch = "SELECT * FROM UserData WHERE username = ?";
        try {
            UserData response = getUserDataFetch(username, conn, fetch);
            if (response.username() != null && response.password() != null && response.email() != null) {
                return response;
            }
        }catch (SQLException e) {
            if(e.getMessage().contains("not found")){
                return null;
            }else{
                throw new ServerException("Error: Userdata get failed: " + e.getMessage());
                }
            }
        return null;
    }

    /**
     * Helper method to assist in getting the user data
     * @param username is the given username to search for
     * @param conn is the database connection
     * @param fetch is the fetch statement
     * @return the userdata if it exists
     * @throws SQLException if no userdata exists
     */
    private static UserData getUserDataFetch(String username, Connection conn, String fetch) throws SQLException {
        try (var preparedStatement = conn.prepareStatement(fetch)) {
            preparedStatement.setString(1, username);

            try (var response = preparedStatement.executeQuery()) {
                try {
                    response.next();
                    return new UserData(response.getString("username"),
                            response.getString("password"),
                            response.getString("email"));
                } catch (SQLException e) {
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
        } catch (SQLException e) {
            throw new ServerException("Userdata add failed: " + e.getMessage());
        } catch (DataAccessException e) {
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
        } catch (SQLException e) {
            throw new ServerException("UserData clear failed: " + e.getMessage());
        } catch (DataAccessException e) {
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

    @Override
    public int executeUpdate(String statement, Object... params) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                for (int i = 0; i < params.length; i++) {
                    preparedStatement.setObject(i + 1, params[i]);
                }
                return preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new ServerException("Update failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("Update failed: " + e.getMessage());
        }
    }

    @Override
    public void configureDatabase() throws ServerException {
        try {
            DatabaseManager.createDatabase();
        } catch (DataAccessException e) {
            throw new ServerException(e.getMessage());
        }
        try (var conn = DatabaseManager.getConnection()) {
            for (var statement : createStatements) {
                try (var preparedStatement = conn.prepareStatement(statement)) {
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException(e.getMessage());
        }

    }
}