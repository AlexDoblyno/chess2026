package dataaccess;

import models.AuthTokenData;
import models.GameData;
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

    //Method for updating the username of a player in a game. Design for websocket
    void updateGame(ChessGame.TeamColor Color, Integer gameID, String username) throws DataAccessException;

    void updateChessGame(ChessGame game, Integer gameID) throws DataAccessException;
}