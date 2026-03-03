package dataaccess;

import Models.AuthTokenData;
import Models.GameData;
import Models.UserData;
import chess.ChessGame;

import java.util.Collection;

public interface UserDataAccess {
    /**
     * UserData methods
     */
    public UserData getUserData(String username);

    public void addUserData(UserData userData);

    /**
     * Mass deletion methods
     */
    public void clearUsers();
}