package models;

import chess.ChessGame;
import com.google.gson.Gson;

public record GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {

    // 删除了未使用的 setGameID()

    public GameData setWhiteUsername(String whiteUsername) {
        return new GameData(this.gameID, whiteUsername, this.blackUsername, this.gameName, this.game);
    }

    public GameData setBlackUsername(String blackUsername) {
        return new GameData(this.gameID, this.whiteUsername, blackUsername, this.gameName, this.game);
    }

    // 删除了未使用的 setGameName()

    public String toString() {
        return new Gson().toJson(this);
    }
}