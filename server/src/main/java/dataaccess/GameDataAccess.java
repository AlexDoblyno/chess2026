package dataaccess;

import models.AuthTokenData;
import models.GameData;
import chess.ChessGame;

import java.util.Collection;

public interface GameDataAccess {



    public abstract Collection<GameData> getGameList() throws ServerException;

    public abstract GameData getGameByName(String gameName) throws ServerException;

    public abstract GameData getGameByID(int gameID) throws ServerException;

    public abstract void createGame(GameData gameData) throws ServerException;

    public abstract void updateGame(GameData gameData) throws ServerException;

    public abstract void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID) throws ServerException;



    public abstract void clearGames() throws ServerException;
}