package dataaccess;

import models.AuthTokenData;

public interface AuthDataAccess {
    /**
     * AuthData methods
     */
    void addAuthData(AuthTokenData authData);

    void removeAuthData(AuthTokenData authData);

    AuthTokenData getAuthData(String authData);

    /**
     * Mass deletion methods
     */
    void clearAuthTokens();
}