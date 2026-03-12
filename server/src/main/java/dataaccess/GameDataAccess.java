package dataaccess;

import Models.AuthTokenData;
import Models.GameData;
import chess.ChessGame;

import java.util.Collection;

public interface GameDataAccess {
    /**
     * GameData methods
     */
    Collection<GameData> getGameList() throws ServerException;

    GameData getGameByName(String gameName) throws ServerException;

    GameData getGameByID(int gameID) throws ServerException;

    void createGame(GameData gameData) throws ServerException;

    void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID) throws ServerException;

    /**
     * Mass deletion methods
     */
    void clearGames() throws ServerException;
}