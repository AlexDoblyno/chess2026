package dataaccess;

import models.AuthTokenData;

import java.util.Collection;
import java.util.HashSet;

public class MemoryAuthDataAccess implements AuthDataAccess{
    Collection<AuthTokenData> authTokenDatabase;

    public MemoryAuthDataAccess() {
        authTokenDatabase = new HashSet<AuthTokenData>();
    }

    @Override
    public void addAuthData(AuthTokenData authData) {
        authTokenDatabase.add(authData);
    }

    @Override
    public void removeAuthData(AuthTokenData authData) {
        authTokenDatabase.removeIf(token -> token.equals(authData));
    }

    @Override
    public AuthTokenData getAuthData(String authToken) {
        for (AuthTokenData token : authTokenDatabase) {
            if (token.authToken().equals(authToken)) {
                return token;
            }
        }
        return null;
    }

    @Override
    public void clearAuthTokens() {
        authTokenDatabase.clear();
    }
}