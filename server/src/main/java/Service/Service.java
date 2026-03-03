package service;

import Models.AuthTokenData;
import Models.GameData;
import Models.UserData;
import chess.ChessGame;
import dataaccess.*;
import server.ServerException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;

public class Service {
    UserDataAccess userDataAccess;
    AuthDataAccess authDataAccess;
    GameDataAccess gameDataAccess;
    AuthTokenData authTokenData;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();

    public Service() {
        userDataAccess = new MemoryUserDataAccess();
        authDataAccess = new MemoryAuthDataAccess();
        gameDataAccess = new MemoryGameDataAccess();
    }


    /**
     * ChessService to register a user in the database
     * @param userData is the UserData object containing the user's data
     * @return the AuthTokenData object created upon registration and logging in to the system
     * @throws ServerException 403: name already taken
     */
    public AuthTokenData register(UserData userData) throws ServerException {
        if (userDataAccess.getUserData(userData.username()) == null) {

            userDataAccess.addUserData(userData);

            authTokenData = new AuthTokenData(generateAuthToken(), userData.username());

            authDataAccess.addAuthData(authTokenData);

            return authTokenData;
        }
        else {
            throw new ServerException("already taken", 403);
        }
    }

    /**
     * Log in a user into the database
     * @param username is the user's username
     * @param password is the user's password
     * @return the AuthTokenData object created upon login
     * @throws ServerException 401
     */
    public AuthTokenData login(String username, String password) throws ServerException {
        UserData userData = userDataAccess.getUserData(username);
        if (userData == null) throw new ServerException("unauthorized", 401);
        if(!userData.password().equals(password)) throw new ServerException("unauthorized", 401);

        authTokenData = new AuthTokenData(generateAuthToken(), username);
        authDataAccess.addAuthData(authTokenData);

        return authTokenData;
    }

    /**
     * Log out an existing user from the database
     * @param authToken is the current login session's authToken
     * @throws ServerException 401
     */
    public void logOut(String authToken) throws ServerException {
        AuthTokenData authData = authDataAccess.getAuthData(authToken);
        if (authData == null) {
            throw new ServerException("unauthorized", 401);
        }
        authDataAccess.removeAuthData(authData);
    }

    /**
     * List all games currently in the database
     * @param authToken is the user's current login session's authToken
     * @return the list of all games
     * @throws ServerException 401
     */
    public Collection<GameData> listGames(String authToken) throws ServerException {
        AuthTokenData authData = authDataAccess.getAuthData(authToken);
        if (authData != null) {
            return gameDataAccess.getGameList();
        }
        throw new ServerException("unauthorized", 401);
    }

    /**
     * Create a new game if the existing game name doesn't exist
     * @param authToken is the user's current login session's stored-in-the-database's authToken
     * @param gameName
     * @return
     * @throws ServerException
     */
    public int createGame(String authToken, String gameName) throws ServerException {
        AuthTokenData authData = authDataAccess.getAuthData(authToken);
        if (authData != null) {
            if (gameDataAccess.getGameByName(gameName) == null) {
                ChessGame newGame = new ChessGame();
                int gameID = generateGameID();
                GameData newGameData = new GameData(gameID, null, null, gameName, newGame);
                gameDataAccess.createGame(newGameData);
                return gameID;
            }
            throw new ServerException("already taken", 403);
        }
        throw new ServerException("unauthorized", 401);
    }

    /**
     * joinGame will assign the given user to the selected team color of the chosen game.
     * @param givenAuthData is the user's authData. contains username and authToken.
     * @param teamColor is the team we will assign the player to.
     * @param gameID is the ID of the game we will try to join.
     */
    public void joinGame(String givenAuthData, ChessGame.TeamColor teamColor, int gameID) throws ServerException {
        // Check for exceptions
        AuthTokenData auth = authDataAccess.getAuthData(givenAuthData);
        if (auth == null) throw new ServerException("unauthorized", 401);

        GameData gameData = gameDataAccess.getGameByID(gameID);
        if (gameData == null) throw new ServerException("bad request", 400);

        // Set the user to the specified team
        if(teamColor == ChessGame.TeamColor.WHITE){
            if (gameData.whiteUsername() != null) throw new ServerException("already taken", 403);
            gameDataAccess.joinGame(auth, ChessGame.TeamColor.WHITE, gameID);
        }
        else if (teamColor == ChessGame.TeamColor.BLACK){
            if (gameData.blackUsername() != null) throw new ServerException("already taken", 403);
            gameDataAccess.joinGame(auth, ChessGame.TeamColor.BLACK, gameID);
        }
    }

    /**
     * guess what clearApp does to our database
     */
    public void clearApp() {
        gameDataAccess.clearGames();
        userDataAccess.clearUsers();
        authDataAccess.clearAuthTokens();
    }

    /**
     * The following are functions to generate IDs for our application.
     */
    // The general implementation for this function came from
    // https://stackoverflow.com/questions/13992972/how-to-create-an-authentication-token-using-java
    // I don't recall us talking about how to do this ourselves, so I used this implementation.
    private String generateAuthToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String authToken = encoder.encodeToString(randomBytes);

        // Verify uniqueness
        if (authDataAccess.getAuthData(authToken) != null) {
            return generateAuthToken();
        }
        return authToken;
    }

    private int generateGameID() {
        byte[] randomBytes = new byte[4];
        secureRandom.nextBytes(randomBytes);
        // Turn bytes into integer
        int gameID = Math.abs(java.nio.ByteBuffer.wrap(randomBytes).getInt());

        // Verify uniqueness
        while (gameDataAccess.getGameByID(gameID) != null) {
            secureRandom.nextBytes(randomBytes);
            gameID = java.nio.ByteBuffer.wrap(randomBytes).getInt();
        }
        return gameID;
    }
}