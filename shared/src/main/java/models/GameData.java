package models;

import chess.ChessGame;
import com.google.gson.Gson;

import java.util.Objects;

public record GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {

    public GameData setWhiteUsername(String whiteUsername) {
        return new GameData(this.gameID, whiteUsername, this.blackUsername, this.gameName, this.game);
    }

    public GameData setBlackUsername(String blackUsername) {
        return new GameData(this.gameID, this.whiteUsername, blackUsername, this.gameName, this.game);
    }

    public GameData setGame(ChessGame game) {
        return new GameData(this.gameID, this.whiteUsername, this.blackUsername, this.gameName, game);
    }

    public GameData clearPlayer(String username) {
        GameData updatedGame = this;
        if (Objects.equals(this.whiteUsername, username))
            updatedGame = updatedGame.setWhiteUsername(null);
        if (Objects.equals(this.blackUsername, username))
            updatedGame = updatedGame.setBlackUsername(null);
        return updatedGame;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}