package ui;

import client.ChessClient;
import exception.ResponseException;
import exception.UIStateException;

public class GameUI extends BaseUI {
    private static Boolean isPlayer;
    private static ChessboardDrawer drawer;

    public GameUI(ChessClient client, ChessboardDrawer drawer, boolean isPlayer) {
        super(client);
        state = UIStatesEnum.GAMEUI;
        GameUI.drawer = drawer;
        GameUI.isPlayer = isPlayer;
    }

    @Override
    public String handler(String input) throws ResponseException {
        String[] tokens = input.split(" ");
        switch(tokens[0].toLowerCase()) {
            case "quit" -> quitGame();
            default -> displayHelpInfo();
        };
        return null;
    }

    private void quitGame() throws ResponseException {
        int gameID = client.getDataCache().getCurrentGameID()+1;
        String gameName = client.getDataCache().getGameByIndex(gameID).gameName();

        client.getDataCache().setCurrentGameID(0);
        String returnStatement = "Left game " + gameName + "successfully.";
        client.logout();
        throw new UIStateException(new PostloginUI(client), returnStatement);
    }

    @Override
    public String displayHelpInfo() {
        return """
    --- GAME COMMANDS ---
    Type a command to get the corresponding action.
    - quit      | Leave your current game.
    - help      | Display this help menu.
    """;
    }
}
