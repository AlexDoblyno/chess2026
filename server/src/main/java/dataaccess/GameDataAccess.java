package dataaccess;

import Models.AuthTokenData;
import Models.GameData;
import chess.ChessGame;

import java.util.Collection;

public interface GameDataAccess {
    /**
     * GameData methods
     */
    public Collection<GameData> getGameList();

    public GameData getGameByName(String gameName);

    public GameData getGameByID(int gameID);

    public void createGame(GameData gameData);

    public void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID);

    /**
     * Mass deletion methods
     */
    public void clearGames();
}