package dataaccess;

import models.AuthTokenData;

import java.sql.SQLException;

public class SqlAuthDataAccess implements AuthDataAccess, SqlAccess {

    public SqlAuthDataAccess () {
        try {
            configureDatabase();
        } catch (ServerException | DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addAuthData(AuthTokenData authData) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var insert = "INSERT INTO AuthData (authToken, username) VALUES (?, ?)";

            try (var preparedStatement = conn.prepareStatement(insert)) {
                preparedStatement.setString(1, authData.authToken());
                preparedStatement.setString(2, authData.username());
                preparedStatement.executeUpdate();
            }catch (SQLException e) {
                throw new ServerException("Authdata add failed: " + e.getMessage());
            }
        } catch (DataAccessException | SQLException e) {
            throw new ServerException("Authdata add failed: " + e.getMessage());
        }

    }

    @Override
    public void removeAuthData(AuthTokenData authData) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var remove = "DELETE FROM AuthData WHERE authToken = ?";

            try (var preparedStatement = conn.prepareStatement(remove)) {
                preparedStatement.setString(1, authData.authToken());
                // Learned that executeUpdate returns an int! Exciting
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected == 0) {
                    throw new ServerException("Auth token not found");
                }
            }
        } catch (SQLException | ServerException | DataAccessException e) {
            throw new ServerException("Authdata remove failed: " + e.getMessage());
        }

    }

    @Override
    public AuthTokenData getAuthData(String authData) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var fetch = "SELECT * FROM AuthData WHERE authToken = ?";

            try (var preparedStatement = conn.prepareStatement(fetch)) {
                preparedStatement.setString(1, authData);

                try (var response = preparedStatement.executeQuery()) {
                    if (!response.next()) {
                        throw new ServerException("Authentication token not found");
                    }
                    return new AuthTokenData(response.getString("authToken"),
                            response.getString("username"));
                }
            }
        } catch (SQLException | ServerException | DataAccessException e) {

            throw new ServerException("Error: Authdata get failed: " + e.getMessage());
        }
    }

    @Override
    public void clearAuthTokens() throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var clear = "DELETE FROM AuthData";

            try (var preparedStatement = conn.prepareStatement(clear)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("AuthData clear failed: " + e.getMessage());
        }
    }

    @Override
    public int executeUpdate(String statement, Object... params) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                for (int i = 0; i < params.length; i++) {
                    preparedStatement.setObject(i + 1, params[i]);
                }
                return preparedStatement.executeUpdate();
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Update failed: " + e.getMessage());
        }
    }

    private final String[] createStatements = {
            """
                CREATE TABLE IF NOT EXISTS AuthData (
                `authToken` varChar(64) NOT NULL PRIMARY KEY,
                `username` varChar(256) NOT NULL
                )
            """
    };

    @Override
    public void configureDatabase() throws ServerException, DataAccessException {
        DatabaseManager.createDatabase();
        try (var conn = DatabaseManager.getConnection()) {
            for (var statement : createStatements) {
                try (var preparedStatement = conn.prepareStatement(statement)) {
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new ServerException("Database creation failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("Database creation failed: " + e.getMessage());
        }

    }
}