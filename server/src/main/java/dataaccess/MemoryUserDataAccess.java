package dataaccess;

import Models.UserData;

import java.util.Collection;
import java.util.HashSet;

public class MemoryUserDataAccess implements UserDataAccess {
    Collection<UserData> userDatabase;

    public MemoryUserDataAccess() {
        userDatabase = new HashSet<UserData>();
    }
    /**
     * UserData methods
     */
    public UserData getUserData(String username) {
        for (UserData user : userDatabase) {
            if (user.username().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public void addUserData(UserData userData) {
        userDatabase.add(userData);
    }

    /**
     * Mass deletion methods
     */
    public void clearUsers() {
        userDatabase.clear();
    }
}