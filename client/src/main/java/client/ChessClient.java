package client;

import chess.ChessGame;
import exception.ResponseException;
import models.AuthTokenData;
import models.GameData;
import models.UserData;
import ui.*;

import java.util.Collection;

import static chess.ChessGame.TeamColor.OBSERVE;

public class ChessClient {
    private final ServerFacade server;
    private final String serverURL;
    private final DataCache dataCache;
    private ChessboardDrawer drawBoard;

    // Set up client server connection
    public ChessClient(String serverURL) {
        this.serverURL = serverURL;
        server = new ServerFacade(this.serverURL);
        dataCache = new DataCache();
        drawBoard = new ChessboardDrawer();
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
        return parameters[1] + " has been successfully logged in!";
    }

    public String listGames() throws ResponseException {
        String authToken = dataCache.getAuthToken();
        Collection<GameData> gameList = server.listGame(authToken);
        dataCache.setGameCache(gameList);

        // Create string display result using StringBuilder
        StringBuilder resultString = new StringBuilder();
        resultString.append(EscapeSequences.ERASE_SCREEN);
        resultString.append(EscapeSequences.SET_TEXT_BOLD).append("\n --< Active Games >--\n")
                .append(EscapeSequences.RESET_TEXT_BOLD_FAINT);

        int index = 1;
        for (GameData game : gameList) {
            resultString.append(String.format("%s%d.%s %s%s%s [ White: %s%s%s | Black: %s%s%s ]\n",
                    EscapeSequences.SET_TEXT_COLOR_BLUE, index++, EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_TEXT_BOLD, game.gameName(), EscapeSequences.RESET_TEXT_BOLD_FAINT,
                    EscapeSequences.SET_TEXT_COLOR_WHITE, game.whiteUsername(), EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_BG_COLOR_DARK_GREY, game.blackUsername(), EscapeSequences.RESET_TEXT_COLOR));
        }
        return resultString.toString();
    }

    public String createGame(String... parameters) throws ResponseException {
        // parameters[0] is the game name. AuthToken is stored in the DataCache.
        server.createGame(dataCache.getAuthToken(), parameters[0]);
        return "Successfully created game room: " + parameters[0];
    }

    // 升级版 joinGame：防崩溃、智能识别参数顺序、友好提示已满房间
    public String joinGame(String... parameters) {
        try {
            if (parameters.length < 2) {
                return "Error: Expected format is 'join [team color] [game ID]', e.g., 'join white 1'";
            }

            String colorStr;
            String idStr;

            // 智能判断参数顺序：允许 "join white 1" 也可以 "join 1 white"
            if (parameters[0].matches("\\d+")) {
                idStr = parameters[0];
                colorStr = parameters[1];
            } else {
                colorStr = parameters[0];
                idStr = parameters[1];
            }

            ChessGame.TeamColor teamColor = getTeamColor(new String[]{colorStr});
            if (teamColor == null) {
                return "Error: Invalid team color. Please choose 'white' or 'black'.";
            }

            int gameIndex;
            try {
                gameIndex = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                return "Error: Invalid game ID number.";
            }

            Collection<GameData> gameList = server.listGame(dataCache.getAuthToken());
            dataCache.setGameCache(gameList);

            // 检查房间是否存在
            GameData gameData = dataCache.getGameByIndex(gameIndex);
            if (gameData == null) {
                return "Error: Game not found. Please check your game list.";
            }

            // 尝试加入游戏，拦截被占用的报错
            try {
                server.joinGame(dataCache.getAuthToken(), teamColor, gameData.gameID());
            } catch (ResponseException e) {
                return EscapeSequences.SET_TEXT_COLOR_RED + "Failed to join: " + e.getMessage() + EscapeSequences.RESET_TEXT_COLOR;
            }

            // Set the gameboard drawer
            drawBoard.setChessGame(gameData.game());
            drawBoard.setPerspective(teamColor);

            StringBuilder resultString = new StringBuilder(drawBoard.drawBoardString());
            resultString.append(String.format("Game - %s\nWhite - %s%s%s\nBlack - %s%s%s",
                    EscapeSequences.SET_TEXT_COLOR_BLUE, gameData.gameName(), EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_TEXT_COLOR_WHITE, gameData.whiteUsername(), EscapeSequences.RESET_TEXT_COLOR,
                    EscapeSequences.SET_BG_COLOR_DARK_GREY, gameData.blackUsername(), EscapeSequences.RESET_TEXT_COLOR));
            return resultString.toString();

        } catch (Exception e) {
            return "An unexpected error occurred: " + e.getMessage();
        }
    }

    // 升级版 observeGame：具备和 join 相同的防崩溃机制
    public String observeGame(String... parameters) {
        try {
            int gameIndex;
            try {
                gameIndex = Integer.parseInt(parameters[0]);
            } catch (NumberFormatException e) {
                return "Error: Invalid game ID number.";
            }

            Collection<GameData> gameList = server.listGame(dataCache.getAuthToken());
            dataCache.setGameCache(gameList);

            GameData gameData = dataCache.getGameByIndex(gameIndex);
            if (gameData == null) {
                return "Error: Game not found. Please check your game list.";
            }

            // 拦截可能发生的异常（虽然观战通常不会被占用，但为了系统健壮性加持）
            try {
                server.joinGame(dataCache.getAuthToken(), OBSERVE, gameData.gameID());
            } catch (ResponseException e) {
                return EscapeSequences.SET_TEXT_COLOR_RED + "Failed to observe: " + e.getMessage() + EscapeSequences.RESET_TEXT_COLOR;
            }

            // Set the gameboard drawer with default WHITE perspective for observers
            drawBoard.setChessGame(gameData.game());
            drawBoard.setPerspective(ChessGame.TeamColor.WHITE);
            return drawBoard.drawBoardString();

        } catch (Exception e) {
            return "An unexpected error occurred: " + e.getMessage();
        }
    }

    private static ChessGame.TeamColor getTeamColor(String[] parameters) {
        // Determine team color
        ChessGame.TeamColor teamColor;
        if (parameters[0].contains("white") || parameters[0].contains("WHITE")) {
            teamColor = ChessGame.TeamColor.WHITE;
        } else if (parameters[0].contains("black") || parameters[0].contains("BLACK")) {
            teamColor = ChessGame.TeamColor.BLACK;
        } else {
            teamColor = null;
        }
        return teamColor;
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
}