package dataaccess;

import chess.ChessGame;
import models.AuthTokenData;
import models.GameData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class MemoryGameDataAccess implements GameDataAccess {

    private final List<GameData> games = new ArrayList<>();

    @Override
    public Collection<GameData> getGameList() {
        return new ArrayList<>(games);
    }

    @Override
    public GameData getGameByName(String gameName) {
        return games.stream()
                .filter(game -> Objects.equals(game.gameName(), gameName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public GameData getGameByID(int gameID) {
        return games.stream()
                .filter(game -> game.gameID() == gameID)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void createGame(GameData gameData) {
        games.add(gameData);
    }

    @Override
    public void updateGame(GameData gameData) {
        games.removeIf(existingGame -> existingGame.gameID() == gameData.gameID());
        games.add(gameData);
    }

    @Override
    public void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID) {
        GameData gameData = getGameByID(gameID);
        if (gameData == null) {
            return;
        }

        GameData updatedGame = switch (team) {
            case WHITE -> gameData.setWhiteUsername(authData.username());
            case BLACK -> gameData.setBlackUsername(authData.username());
            default -> gameData;
        };

        updateGame(updatedGame);
    }

    @Override
    public void clearGames() {
        games.clear();
    }
}