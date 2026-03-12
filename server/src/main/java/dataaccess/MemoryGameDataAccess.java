package dataaccess;

import Models.AuthTokenData;
import Models.GameData;
import chess.ChessGame;

import java.util.Collection;
import java.util.HashSet;

public class MemoryGameDataAccess implements GameDataAccess {
    Collection<GameData> gameDatabase;

    public MemoryGameDataAccess() {
        gameDatabase = new HashSet<GameData>();
    }

    @Override
    public Collection<GameData> getGameList() {
//        Collection<String> gameList = new HashSet<String>();
//        for (GameData game : gameDatabase) {
//            gameList.add(game.gameName());
//        }
        return gameDatabase;
    }

    @Override
    public GameData getGameByName(String gameName) {
        for (GameData game : gameDatabase) {
            if (game.gameName().equals(gameName)) {
                return game;
            }
        }
        return null;
    }

    @Override
    public GameData getGameByID(int gameID) {
        for (GameData game : gameDatabase) {
            if (game.gameID() == (gameID)) {
                return game;
            }
        }
        return null;
    }

    @Override
    public void createGame(GameData gameData) {
        gameDatabase.add(gameData);
    }

    @Override
    public void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID) {
        GameData savedGame = getGameByID(gameID);
        GameData updateGame = null;

        if (team == ChessGame.TeamColor.WHITE) {
            updateGame = savedGame.setWhiteUsername(authData.username());
        } else if (team == ChessGame.TeamColor.BLACK) {
            updateGame = savedGame.setBlackUsername(authData.username());
        }

        gameDatabase.remove(savedGame);
        gameDatabase.add(updateGame);
    }

    @Override
    public void clearGames() {
        gameDatabase.clear();
    }
}