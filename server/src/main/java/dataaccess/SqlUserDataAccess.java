package dataaccess;

import models.AuthTokenData;
import models.UserData;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlUserDataAccess implements UserDataAccess, SqlAccess {

    public SqlUserDataAccess () {
        try {
            configureDatabase();
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserData getUserData(String username) throws ServerException, server.ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var fetch = "SELECT * FROM UserData WHERE username = ?";

            UserData response = getUserDataFetch(username, conn, fetch);
            if (response.username() != null && response.password() != null && response.email() != null) {
                return response;
            }
        } catch (SQLException e) {
            throw new ServerException("Userdata get failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
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
                if (response.next()) {
                    return new UserData(response.getString("username"),
                            response.getString("password"),
                            response.getString("email"));
                }
                else {
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
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configureDatabase() throws ServerException, DataAccessException {
        DatabaseManager.createDatabase();
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