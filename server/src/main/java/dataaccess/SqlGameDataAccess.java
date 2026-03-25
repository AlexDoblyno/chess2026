package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import models.AuthTokenData;
import models.GameData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;

public class SqlGameDataAccess implements GameDataAccess, SqlAccess {

    public SqlGameDataAccess () {
        try {
            configureDatabase();
        } catch (ServerException | DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<GameData> getGameList() throws ServerException {
        HashSet<GameData> gameList = new HashSet<>();
        String listStatement = "SELECT * FROM GameData";

        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(listStatement);
             var response = preparedStatement.executeQuery()) {

            while (response.next()) {
                gameList.add(deserializeGameData(response));
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("GameData list get failed: " + e.getMessage());
        }
        return gameList;
    }

    @Override
    public GameData getGameByName(String gameName) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            String fetch = "SELECT * FROM GameData WHERE gameName = ?";
            try (var preparedStatement = conn.prepareStatement(fetch)) {
                preparedStatement.setString(1, gameName);
                try (var response = preparedStatement.executeQuery()) {
                    if (!response.next()) {
                        return null;
                    }
                    return deserializeGameData(response);
                }
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("gameData getGameByName failed: " + e.getMessage());
        }
    }

    @Override
    public GameData getGameByID(int gameID) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            String fetch = "SELECT * FROM GameData WHERE gameID = ?";
            try (var preparedStatement = conn.prepareStatement(fetch)) {
                preparedStatement.setInt(1, gameID);
                try (var response = preparedStatement.executeQuery()) {
                    if (!response.next()) {
                        return null;
                    }
                    return deserializeGameData(response);
                }
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("gameData getGameByID failed: " + e.getMessage());
        }
    }

    @Override
    public void createGame(GameData gameData) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            String create = "INSERT INTO GameData (gameID, whiteUsername, blackUsername, gameName, game)" +
                    "VALUES (?, ?, ?, ?, ?)";
            Gson gson = new Gson();

            try (var preparedStatement = conn.prepareStatement(create)) {
                preparedStatement.setInt(1, gameData.gameID());
                preparedStatement.setObject(2,gameData.whiteUsername());
                preparedStatement.setObject(3, gameData.blackUsername());
                preparedStatement.setString(4, gameData.gameName());

                String gameJson = gson.toJson(gameData.game());
                preparedStatement.setString(5, gameJson);

                preparedStatement.executeUpdate();
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Create game failed: " + e.getMessage());
        }
    }

    @Override
    public void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            String join = null;
            if (team == ChessGame.TeamColor.WHITE) {
                join = "UPDATE GameData SET whiteUsername = ? WHERE gameID = ? AND whiteUsername IS NULL";
            } else if (team == ChessGame.TeamColor.BLACK){
                join = "UPDATE GameData SET blackUsername = ? WHERE gameID = ? AND blackUsername IS NULL";
            }

            try (var preparedStatement = conn.prepareStatement(join)) {
                preparedStatement.setString(1, authData.username());
                preparedStatement.setInt(2, gameID);

                int executeReturn = preparedStatement.executeUpdate();
                if (executeReturn == 0) {
                    throw new ServerException("Join game failed: Invalid game or existing user");
                }
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Join game failed: " + e.getMessage());
        }
    }

    @Override
    public void clearGames() throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var clear = "DELETE FROM GameData";
            try (var preparedStatement = conn.prepareStatement(clear)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("GameData clear failed: " + e.getMessage());
        }
    }

    private GameData deserializeGameData(ResultSet response) throws SQLException {
        int gameID = response.getInt("gameID");
        String whiteUser = response.getString("whiteUsername");
        if (response.wasNull()) whiteUser = null;
        String blackUser = response.getString("blackUsername");
        if (response.wasNull()) blackUser = null;
        String dbGameName = response.getString("gameName");
        String gameJson = response.getString("game");

        try {
            ChessGame chessGame = new Gson().fromJson(gameJson, ChessGame.class);
            return new GameData(gameID, whiteUser, blackUser, dbGameName, chessGame);
        } catch (JsonSyntaxException e) {
            throw new SQLException("Invalid JSON for entry with gameID " + gameID, e);
        }
    }

    private final String[] createStatements = {
            """
                CREATE TABLE IF NOT EXISTS GameData (
                `gameID` INT UNSIGNED PRIMARY KEY,
                `whiteUsername` VARCHAR(255),
                `blackUsername` VARCHAR(255),
                `gameName` VARCHAR(255) NOT NULL,
                `game` TEXT NOT NULL,
                FOREIGN KEY (whiteUsername) REFERENCES UserData(username) ON DELETE SET NULL,
                FOREIGN KEY (blackUsername) REFERENCES UserData(username) ON DELETE SET NULL
                );"""
    };

    // 这里同样删除了 executeUpdate 方法

    @Override
    public void configureDatabase() throws ServerException, DataAccessException {
        // 直接调用接口里写好的默认逻辑
        configureDatabase(createStatements);
    }
}