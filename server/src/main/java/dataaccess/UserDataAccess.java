package dataaccess;

import models.UserData;

public interface UserDataAccess {
    /**
     * UserData methods
     */
    UserData getUserData(String username);

    void addUserData(UserData userData);

    /**
     * Mass deletion methods
     */
    void clearUsers();
}