package ui;

import chess.ChessGame;
import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;
import models.GameData;

public class PostloginUI extends BaseUI {

    public PostloginUI(ChessClient client) {
        super(client);
        state = UIStatesEnum.POSTLOGINUI;
    }

    @Override
    public String handler(String input) throws ResponseException {
        String[] tokens = input.split(" ");
        switch (tokens[0].toLowerCase()) {
            case "list" -> {
                return handleList();
            }
            case "create" -> {
                return handleCreate(tokens);
            }
            case "join" -> handleJoin(tokens);
            case "observe" -> handleObserve(tokens);
            case "logout" -> handleLogout();
            default -> {
                return displayHelpInfo();
            }
        };
        return null;
    }

    private String handleList() throws ResponseException {
        return client.listGames();
    }

    private String handleCreate(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 2);
        String gameName = tokens[1];
        String authToken = client.getDataCache().getAuthToken();
        String result = client.createGame(authToken, gameName);
        return result;
    }

    private void handleJoin(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 3);
        String joinTeam = tokens[1];
        String gameID = tokens[2];
        String result = client.joinGame(joinTeam, gameID);
        ChessGame.TeamColor teamColor = parseTeamColor(joinTeam);

        GameData gameData = client.getDataCache().getGameByIndex(Integer.parseInt(gameID));
        String authToken = client.getDataCache().getAuthToken();
        ChessboardDrawer drawer = new ChessboardDrawer(gameData.game(), teamColor);

        GameUI gameUI = createGameUI(drawer, true, gameData.gameID(), teamColor, authToken);

        throw new UIStateException(gameUI, result);
    }

    private void handleObserve(String[] tokens) throws ResponseException {
        validateParameterLength(tokens, 2);
        int gameID = Integer.parseInt(tokens[1]);
        String result = client.observeGame(tokens[1]);

        GameData gameData = client.getDataCache().getGameByIndex(gameID);
        String authToken = client.getDataCache().getAuthToken();
        ChessboardDrawer drawer = new ChessboardDrawer(gameData.game(), ChessGame.TeamColor.OBSERVE);

        GameUI gameUI = createGameUI(drawer, false, gameData.gameID(), ChessGame.TeamColor.OBSERVE, authToken);

        throw new UIStateException(gameUI, result);
    }

    private void handleLogout() throws ResponseException {
        String result = client.logout();
        client.getDataCache().setAuthToken(null);
        throw new UIStateException(new PreloginUI(client), result);
    }

    private ChessGame.TeamColor parseTeamColor(String teamString) {
        return (teamString.equalsIgnoreCase("WHITE") ?
                ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK);
    }

    private GameUI createGameUI(ChessboardDrawer drawer, boolean isPlayer, int gameID,
                                ChessGame.TeamColor teamColor, String authToken) throws ResponseException {
        try {
            return new GameUI(client, drawer, isPlayer, gameID, teamColor, authToken);
        } catch (Exception e) {
            throw new ResponseException(e.getMessage(), 500);
        }
    }

    @Override
    public String displayHelpInfo() {
        return """
                --- HELP ---
                Type a command to get the corresponding action.
                - list                        | List all existing games.
                - create [game name]          | Create a new game with your specified name.
                - join [team color] [game ID] | Join an existing game with the specified game ID.
                - observe [game ID]           | Observe a specified game as a spectator.
                - logout                      | Logout the current user.
                - help                        | Display this help menu.
                """;
    }
}
