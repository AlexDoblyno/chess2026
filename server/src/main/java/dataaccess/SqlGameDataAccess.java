package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import models.AuthTokenData;
import models.GameData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;

/**
 * SQL-based implementation for accessing game data.
 * Uses JDBC with MySQL-compatible syntax to persist GameData objects.
 * JSON serialization via GSON is used to store ChessGame state in the database.
 *
 * @see <a href="https://dev.mysql.com/doc/connector-j/8.0/en/">MySQL Connector/J Documentation</a>
 */
public class SqlGameDataAccess implements GameDataAccess, SqlAccess {

    /**
     * Static SQL statements for initializing the database schema.
     * Uses TEXT type for JSON storage and VARCHAR for usernames.
     * Foreign keys reference UserData table with ON DELETE SET NULL behavior.
     *
     * @see <a href="https://www.w3schools.com/sql/sql_foreignkey.asp">SQL FOREIGN KEY Constraint</a>
     */
    private static final String[] CREATE_STATEMENTS = {
            """
        CREATE TABLE IF NOT EXISTS GameData (
            `gameID` INT UNSIGNED PRIMARY KEY,
            `whiteUsername` VARCHAR(255),
            `blackUsername` VARCHAR(255),
            `gameName` VARCHAR(255) NOT NULL,
            `game` TEXT NOT NULL,
            FOREIGN KEY (whiteUsername) REFERENCES UserData(username) ON DELETE SET NULL,
            FOREIGN KEY (blackUsername) REFERENCES UserData(username) ON DELETE SET NULL
        );
        """
    };

    /**
     * Constructor ensures the database and required tables are initialized.
     * Throws RuntimeException only if configuration fails (wraps checked exceptions).
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html">Unchecked Exceptions in Java</a>
     */
    public SqlGameDataAccess() {
        try {
            configureDatabase();
        } catch (ServerException | DataAccessException e) {
            throw new RuntimeException("Failed to initialize SqlGameDataAccess", e);
        }
    }

    /**
     * Retrieves all games from the database.
     * Utilizes try-with-resources to auto-close ResultSet, PreparedStatement, and Connection.
     * This pattern prevents resource leaks and is recommended by Oracle.
     *
     * @return Collection of GameData objects
     * @throws ServerException if database access fails
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">Try-with-resources Statement</a>
     */
    @Override
    public Collection<GameData> getGameList() throws ServerException {
        HashSet<GameData> gameList = new HashSet<>();

        final String query = "SELECT * FROM GameData";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                gameList.add(deserializeGameData(rs));
            }
        } catch (SQLException e) {
            throw new ServerException("Failed to retrieve game list: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("Data access error during game list retrieval: " + e.getMessage());
        }

        return gameList;
    }

    /**
     * Finds a game by its name.
     * Case-sensitive exact match on gameName field.
     *
     * @param gameName the name of the game to find
     * @return GameData if found, null otherwise
     * @throws ServerException if database error occurs
     */
    @Override
    public GameData getGameByName(String gameName) throws ServerException {
        final String query = "SELECT * FROM GameData WHERE gameName = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, gameName);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? deserializeGameData(rs) : null;
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Failed to retrieve game by name: " + e.getMessage());
        }
    }

    /**
     * Retrieves a single game by its unique ID.
     *
     * @param gameID the ID of the game
     * @return GameData if found, null otherwise
     * @throws ServerException if database error occurs
     */
    @Override
    public GameData getGameByID(int gameID) throws ServerException {
        final String query = "SELECT * FROM GameData WHERE gameID = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, gameID);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? deserializeGameData(rs) : null;
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Failed to retrieve game by ID: " + e.getMessage());
        }
    }

    /**
     * Inserts a new game into the database.
     * The ChessGame object is serialized to JSON using GSON before storage.
     *
     * @param gameData the game to create
     * @throws ServerException if insertion fails
     * @see <a href="https://github.com/google/gson/blob/master/UserGuide.md#TOC-Object-Examples">GSON Object Serialization Guide</a>
     */
    @Override
    public void createGame(GameData gameData) throws ServerException {
        final String insert = "INSERT INTO GameData (gameID, whiteUsername, blackUsername, gameName, game) VALUES (?, ?, ?, ?, ?)";
        Gson gson = new Gson();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insert)) {

            stmt.setInt(1, gameData.gameID());
            stmt.setObject(2, gameData.whiteUsername());
            stmt.setObject(3, gameData.blackUsername());
            stmt.setString(4, gameData.gameName());
            stmt.setString(5, gson.toJson(gameData.game()));

            stmt.executeUpdate();
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Failed to create game: " + e.getMessage());
        }
    }

    /**
     * Allows a user to join a game as white or black.
     * Only succeeds if the target color slot is NULL (unoccupied).
     *
     * @param authData user authentication token
     * @param team     desired team color
     * @param gameID   target game ID
     * @throws ServerException if update fails or no rows are affected
     */
    @Override
    public void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID) throws ServerException {
        String updateQuery;
        if (team == ChessGame.TeamColor.WHITE) {
            updateQuery = "UPDATE GameData SET whiteUsername = ? WHERE gameID = ? AND whiteUsername IS NULL";
        } else if (team == ChessGame.TeamColor.BLACK) {
            updateQuery = "UPDATE GameData SET blackUsername = ? WHERE gameID = ? AND blackUsername IS NULL";
        } else {
            throw new ServerException("Invalid team color specified");
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

            stmt.setString(1, authData.username());
            stmt.setInt(2, gameID);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new ServerException("Join failed: game may not exist or color already taken");
            }
        } catch (SQLException e) {
            throw new ServerException("Database error during join: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("Data access error during join: " + e.getMessage());
        }
    }

    /**
     * Removes all games from the database.
     * This is a DELETE operation, not DROP.
     *
     * @throws ServerException if deletion fails
     */
    @Override
    public void clearGames() throws ServerException {
        final String deleteAll = "DELETE FROM GameData";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteAll)) {

            stmt.executeUpdate();
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Failed to clear games: " + e.getMessage());
        }
    }

    /**
     * Updates the username of a player (white or black) in a game.
     * Does not check for existing values — intended for internal use.
     *
     * @param playerColor color of the player to update
     * @param gameID      target game ID
     * @param username    new username
     * @throws DataAccessException if SQL error occurs
     */
    @Override
    public void updateGame(ChessGame.TeamColor playerColor, Integer gameID, String username) throws DataAccessException {
        String sql = (playerColor == ChessGame.TeamColor.BLACK)
                ? "UPDATE GameData SET blackUsername = ? WHERE gameID = ?"
                : "UPDATE GameData SET whiteUsername = ? WHERE gameID = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setInt(2, gameID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("SQL error updating player: " + e.getMessage());
        }
    }

    /**
     * Serializes and updates the full ChessGame object in the database.
     *
     * @param game   updated chess game state
     * @param gameID target game ID
     * @throws DataAccessException if update fails
     */
    @Override
    public void updateChessGame(ChessGame game, Integer gameID) throws DataAccessException {
        final String sql = "UPDATE GameData SET game = ? WHERE gameID = ?";
        Gson gson = new Gson();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gson.toJson(game));
            stmt.setInt(2, gameID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update serialized game: " + e.getMessage());
        }
    }

    /**
     * Generic method to execute UPDATE/INSERT/DELETE statements with parameters.
     * Useful for testing and dynamic operations.
     *
     * @param statement SQL statement with placeholders
     * @param params    variable arguments for parameters
     * @return number of affected rows
     * @throws ServerException if execution fails
     */
    @Override
    public int executeUpdate(String statement, Object... params) throws ServerException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(statement)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ServerException("SQL update failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("Data access issue in update: " + e.getMessage());
        }
    }

    /**
     * Deserializes a ResultSet row into a GameData object.
     * Handles potential NULL values using wasNull() check.
     * Uses GSON to parse the stored JSON into a ChessGame instance.
     *
     * @param rs ResultSet pointing to a valid row
     * @return deserialized GameData object
     * @throws SQLException if data is malformed or JSON is invalid
     * @see <a href="https://www.baeldung.com/java-resultset-wasnull">Handling NULLs in JDBC ResultSet</a>
     */
    private GameData deserializeGameData(ResultSet rs) throws SQLException {
        int gameID = rs.getInt("gameID");
        String whiteUser = rs.getString("whiteUsername");
        if (rs.wasNull()) whiteUser = null;

        String blackUser = rs.getString("blackUsername");
        if (rs.wasNull()) blackUser = null;

        String gameName = rs.getString("gameName");
        String gameJson = rs.getString("game");

        try {
            ChessGame chessGame = new Gson().fromJson(gameJson, ChessGame.class);
            return new GameData(gameID, whiteUser, blackUser, gameName, chessGame);
        } catch (JsonSyntaxException e) {
            throw new SQLException("Malformed JSON in game data (gameID=" + gameID + ")", e);
        }
    }

    /**
     * Ensures the database and GameData table exist.
     * Calls DatabaseManager to set up the environment.
     * Executes DDL statements to create schema if missing.
     *
     * @throws ServerException     if SQL execution fails
     * @throws DataAccessException if connection fails
     * @see <a href="https://refactoring.guru/replace-conditional-with-polymorphism">Schema Initialization Best Practices</a>
     */
    @Override
    public void configureDatabase() throws ServerException, DataAccessException {
        DatabaseManager.createDatabase();

        try (Connection conn = DatabaseManager.getConnection()) {
            for (String statement : CREATE_STATEMENTS) {
                try (PreparedStatement stmt = conn.prepareStatement(statement)) {
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new ServerException("Failed to configure database schema: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("Data access error during configuration: " + e.getMessage());
        }
    }
}