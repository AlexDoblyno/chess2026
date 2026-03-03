package dataaccess;

import models.UserData;

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