package dataaccess;

import Models.AuthTokenData;

public interface AuthDataAccess {
    /**
     * AuthData methods
     */
    public void addAuthData (AuthTokenData authData);

    public void removeAuthData (AuthTokenData authData);

    public AuthTokenData getAuthData (String authData);

    /**
     * Mass deletion methods
     */
    public void clearAuthTokens();
}