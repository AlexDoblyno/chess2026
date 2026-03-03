package dataaccess;

import models.AuthTokenData;
import models.GameData;
import chess.ChessGame;

import java.util.Collection;

public interface GameDataAccess {
    /**
     * GameData methods
     */
    Collection<GameData> getGameList();

    GameData getGameByName(String gameName);

    GameData getGameByID(int gameID);

    void createGame(GameData gameData);

    void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID);

    /**
     * Mass deletion methods
     */
    void clearGames();
}