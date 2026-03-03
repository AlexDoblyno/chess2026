package service;

import models.AuthTokenData;
import models.GameData;
import models.UserData;
import chess.ChessGame;
import dataaccess.*;
import org.mindrot.jbcrypt.BCrypt;
import server.ServerException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;

public class Service {
    UserDataAccess userDataAccess;
    AuthDataAccess authDataAccess;
    GameDataAccess gameDataAccess;
    AuthTokenData authTokenData;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();

    public Service() {
        userDataAccess = new SqlUserDataAccess();
        authDataAccess = new SqlAuthDataAccess();
        gameDataAccess = new SqlGameDataAccess();
    }


    /**
     * ChessService to register a user in the database
     * @param userData is the UserData object containing the user's data
     * @return the AuthTokenData object created upon registration and logging in to the system
     * @throws ServerException 403: name already taken
     */
    public AuthTokenData register(UserData userData) throws ServerException {
        // Input validation on the service level
        if (userData == null || userData.username() == null || userData.password() == null || userData.email() == null) {
            throw new ServerException("bad request", 400);
        }
        try {
            if (userDataAccess.getUserData(userData.username()) == null) {
                // Hash password
                String hashedPW = BCrypt.hashpw(userData.password(), BCrypt.gensalt());
                UserData hashedData = new UserData(userData.username(), hashedPW, userData.email());

                userDataAccess.addUserData(hashedData);
                authTokenData = new AuthTokenData(generateAuthToken(), hashedData.username());
                authDataAccess.addAuthData(authTokenData);

                return authTokenData;
            }
            else {
                throw new ServerException("already taken", 403);
            }
        } catch (dataaccess.ServerException e) {
            if (e.getMessage().contains("unauthorized")) {
                throw new ServerException(e.getMessage(), 401);
            }
            throw new ServerException(e.getMessage(), 500);
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
        try {
            UserData userData = userDataAccess.getUserData(username);
            if (userData == null) {
                throw new ServerException("unauthorized", 401);
            }
            if (!BCrypt.checkpw(password, userData.password())) {
                throw new ServerException("unauthorized", 401);
            }

            authTokenData = new AuthTokenData(generateAuthToken(), username);
            authDataAccess.addAuthData(authTokenData);

            return authTokenData;
        } catch (ServerException | dataaccess.ServerException e) {
            if (e.getMessage().contains("unauthorized")) {
                throw new ServerException(e.getMessage(), 401);
            }
            throw new ServerException(e.getMessage(), 500);
        }
    }

    /**
     * Log out an existing user from the database
     * @param authToken is the current login session's authToken
     * @throws ServerException 401
     */
    public void logOut(String authToken) throws ServerException {
        try {
            AuthTokenData authData = authDataAccess.getAuthData(authToken);
            if (authData == null) {
                throw new ServerException("unauthorized", 401);
            }
            authDataAccess.removeAuthData(authData);
        } catch (ServerException | dataaccess.ServerException e) {
            if (e.getMessage().contains("unauthorized")) {
                throw new ServerException(e.getMessage(), 401);
            }
            throw new ServerException(e.getMessage(), 500);
        }
    }

    /**
     * List all games currently in the database
     * @param authToken is the user's current login session's authToken
     * @return the list of all games
     * @throws ServerException 401
     */
    public Collection<GameData> listGames(String authToken) throws ServerException {
        try {
            AuthTokenData authData = authDataAccess.getAuthData(authToken);
            if (authData != null) {
                return gameDataAccess.getGameList();
            }
            throw new ServerException("unauthorized", 401);
        } catch (dataaccess.ServerException e) {
            throw new ServerException(e.getMessage(), 500);
        }
    }

    /**
     * Create a new game if the existing game name doesn't exist
     * @param authToken is the user's current login session's stored-in-the-database's authToken
     * @param gameName
     * @return
     * @throws ServerException
     */
    public int createGame(String authToken, String gameName) throws ServerException {
        try {
            AuthTokenData authData = authDataAccess.getAuthData(authToken);
            if (authData != null) {
                if (gameDataAccess.getGameByName(gameName) == null) {
                    ChessGame newGame = new ChessGame();
                    int gameID = generateGameID();
                    while (gameDataAccess.getGameByID(gameID) != null) {
                        gameID = generateGameID();
                    }
                    GameData newGameData = new GameData(gameID, null, null, gameName, newGame);
                    gameDataAccess.createGame(newGameData);
                    return gameID;
                }
                throw new ServerException("already taken", 403);
            }
            throw new ServerException("unauthorized", 401);
        } catch (dataaccess.ServerException e) {
            throw new ServerException(e.getMessage(), 500);
        }
    }

    /**
     * joinGame will assign the given user to the selected team color of the chosen game.
     * @param givenAuthData is the user's authData. contains username and authToken.
     * @param teamColor is the team we will assign the player to.
     * @param gameID is the ID of the game we will try to join.
     */
    public void joinGame(String givenAuthData, ChessGame.TeamColor teamColor, int gameID) throws ServerException {
        try {
            // Check for exceptions
            AuthTokenData auth = authDataAccess.getAuthData(givenAuthData);
            if (auth == null) {
                throw new ServerException("unauthorized", 401);
            }

            GameData gameData = gameDataAccess.getGameByID(gameID);
            if (gameData == null) {
                throw new ServerException("bad request", 400);
            }

            // Set the user to the specified team
            if (teamColor == ChessGame.TeamColor.WHITE) {
                if (gameData.whiteUsername() != null) {
                    throw new ServerException("already taken", 403);
                }
                gameDataAccess.joinGame(auth, ChessGame.TeamColor.WHITE, gameID);
            } else if (teamColor == ChessGame.TeamColor.BLACK) {
                if (gameData.blackUsername() != null) {
                    throw new ServerException("already taken", 403);
                }
                gameDataAccess.joinGame(auth, ChessGame.TeamColor.BLACK, gameID);
            }
        } catch (dataaccess.ServerException e) {
            throw new ServerException(e.getMessage(), 500);
        }
    }

    /**
     * guess what clearApp does to our database
     */
    public void clearApp() throws ServerException {
        try {
            gameDataAccess.clearGames();
            userDataAccess.clearUsers();
            authDataAccess.clearAuthTokens();
        } catch (dataaccess.ServerException e) {
            throw new ServerException(e.getMessage(), 500);
        }
    }

    /**
     * The following are functions to generate IDs for our application.
     */
    // 该功能的一般实现来自
    // https://stackoverflow.com/questions/13992972/how-to-create-an-authentication-token-using-java
    // 我不记得我们自己讨论过如何做到这一点，所以我采用了这种实现方式。
    private String generateAuthToken() throws ServerException {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String authToken = ENCODER.encodeToString(randomBytes);

        try {
            // Verify uniqueness
            if (authDataAccess.getAuthData(authToken) != null) {
                return generateAuthToken();
            }
        } catch (dataaccess.ServerException e) {
            throw new ServerException(e.getMessage(), 500);
        }
        return authToken;
    }

    private int generateGameID() {
        byte[] randomBytes = new byte[4];
        SECURE_RANDOM.nextBytes(randomBytes);
        // Turn bytes into integer
        int gameID = Math.abs(java.nio.ByteBuffer.wrap(randomBytes).getInt());

        return gameID;
//        try {
//            // Verify uniqueness
//            while (gameDataAccess.getGameByID(gameID) != null) {
//                SECURE_RANDOM.nextBytes(randomBytes);
//                gameID = java.nio.ByteBuffer.wrap(randomBytes).getInt();
//            }
//            return gameID;
//        } catch (dataaccess.ServerException e) {
//            throw new ServerException(e.getMessage(), 500);
//        }
    }
}