package client;

import chess.ChessGame;
import exception.ResponseException;
import models.AuthTokenData;
import models.GameData;
import models.UserData;
import ui.DataCache;
import ui.EscapeSequences;

import java.util.Collection;

public class ChessClient {
    private final ServerFacade server;
    private final String serverURL;
    private final DataCache dataCache;

    // Set up client server connection
    public ChessClient(String serverURL) {
        this.serverURL = serverURL;
        server = new ServerFacade(this.serverURL);
        dataCache = new DataCache();
    }

    public String register(String... parameters) throws ResponseException {
        UserData user = new UserData(parameters[0], parameters[1], parameters[2]);

        // Set auth token in cached data object
        AuthTokenData authTokenData = server.registerUser(user);
        dataCache.setAuthToken(authTokenData.authToken());

        return user.username() + " has been successfully registered!";
    }

    public String login(String... parameters) throws ResponseException {
        AuthTokenData authTokenData = server.loginUser(parameters[0], parameters[1]);
        dataCache.setAuthToken(authTokenData.authToken());
        return parameters[0] + " has been successfully logged in!";
    }

    public String listGames() throws ResponseException {
        Collection<GameData> gameList = refreshGameCache();

        StringBuilder resultString = new StringBuilder();
        resultString.append(EscapeSequences.ERASE_SCREEN);
        resultString.append(EscapeSequences.SET_TEXT_BOLD).append("\n --< Active Games >--\n")
                .append(EscapeSequences.RESET_TEXT_BOLD_FAINT);

        if (gameList.isEmpty()) {
            resultString.append("No games found. Create one to get started.\n");
            return resultString.toString();
        }

        int index = 1;
        for (GameData game : gameList) {
            resultString.append(String.format("%s%d.%s %s%s%s [ White: %s%s%s | Black: %s%s%s ]\n",
                    EscapeSequences.SET_TEXT_COLOR_BLUE, index++, EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_TEXT_BOLD, game.gameName(), EscapeSequences.RESET_TEXT_BOLD_FAINT,
                    EscapeSequences.SET_TEXT_COLOR_WHITE, displaySeat(game.whiteUsername()), EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_BG_COLOR_DARK_GREY, displaySeat(game.blackUsername()), EscapeSequences.RESET_TEXT_COLOR));
        }
        return resultString.toString();
    }

    public String createGame(String... parameters) throws ResponseException {
        // parameters[0] is the game name. AuthToken is stored in the DataCache.
        server.createGame(dataCache.getAuthToken(), parameters[0]);
        return "Successfully created game room: " + parameters[0];
    }

    public GameData joinGame(ChessGame.TeamColor teamColor, int gameNumber) throws ResponseException {
        if (teamColor != ChessGame.TeamColor.WHITE && teamColor != ChessGame.TeamColor.BLACK)
            throw new ResponseException("Choose white or black when joining a game.", 400);

        GameData gameData = loadGameChoice(gameNumber);
        server.joinGame(dataCache.getAuthToken(), teamColor, gameData.gameID());
        dataCache.setCurrentGameID(gameData.gameID());
        return refreshSelectedGame(gameNumber, gameData);
    }

    public GameData observeGame(int gameNumber) throws ResponseException {
        GameData gameData = loadGameChoice(gameNumber);
        dataCache.setCurrentGameID(gameData.gameID());
        return gameData;
    }

    private GameData loadGameChoice(int gameNumber) throws ResponseException {
        refreshGameCache();
        GameData gameData = dataCache.getGameByIndex(gameNumber);
        if (gameData == null)
            throw new ResponseException("Game not found. Type 'list' and pick a valid game number.", 400);
        return gameData;
    }

    private GameData refreshSelectedGame(int gameNumber, GameData fallbackGame) throws ResponseException {
        refreshGameCache();
        GameData refreshedGame = dataCache.getGameByIndex(gameNumber);
        return refreshedGame != null ? refreshedGame : fallbackGame;
    }

    private Collection<GameData> refreshGameCache() throws ResponseException {
        Collection<GameData> gameList = server.listGame(dataCache.getAuthToken());
        dataCache.setGameCache(gameList);
        return gameList;
    }

    private static String displaySeat(String username) {
        if (username == null || username.isBlank())
            return "(open)";
        return username;
    }

    public String quitGame() {
        return "Quit";
    }

    public String logout() throws ResponseException {
        server.logoutUser(dataCache.getAuthToken());
        dataCache.setAuthToken(null);
        return "Logout successful!";
    }

    public DataCache getDataCache() {
        return dataCache;
    }

    public String getServerURL() {
        return serverURL;
    }
}