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
        // 现在当玩家输入 exit 时，触发离开房间的逻辑
        return switch(tokens[0].toLowerCase()) {
            case "exit", "leave" -> exitGame(); // 顺便加了一个 leave，两者都可以离开
            default -> displayHelpInfo();
        };
    }

    // 将原先的 quitGame 改名为 exitGame
    private String exitGame() throws ResponseException {
        int gameID = client.getDataCache().getCurrentGameID() + 1;
        String gameName = client.getDataCache().getGameByIndex(gameID).gameName();

        client.getDataCache().setCurrentGameID(0);
        String returnStatement = "Left game room '" + gameName + "' successfully. You are still logged in.";

        // 【核心逻辑】：只抛出 UIStateException 切换回大厅，什么都不做。
        // 因为没有调用 client.logout()，你的 AuthToken 依然安全地存在 DataCache 中。
        throw new UIStateException(new PostloginUI(client), returnStatement);
    }

    @Override
    public String displayHelpInfo() {
        return """
    --- GAME COMMANDS ---
    Type a command to get the corresponding action.
    - exit      | Leave your current game room and return to the lobby.
    - leave     | Same as exit.
    - help      | Display this help menu.
    """;
    }
}