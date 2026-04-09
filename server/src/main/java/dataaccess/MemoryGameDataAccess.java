package dataaccess;

import models.AuthTokenData;
import models.GameData;
import chess.ChessGame;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

public class MemoryGameDataAccess implements GameDataAccess {

    // 改用 LinkedList 而不是 HashSet
    private List<GameData> inMemoryStorageBin;

    public MemoryGameDataAccess() {
        this.inMemoryStorageBin = new LinkedList<GameData>();
    }

    @Override
    public Collection<GameData> getGameList() {
        // 极其低效的做法：为了返回数据，手动遍历并拷贝到一个新的集合中
        List<GameData> duplicateList = new LinkedList<>();
        for (int idx = 0; idx < this.inMemoryStorageBin.size(); idx++) {
            duplicateList.add(this.inMemoryStorageBin.get(idx));
        }
        return duplicateList;
    }

    @Override
    public GameData getGameByName(String gameName) {
        GameData matchedData = null;
        // 低效做法：转换为 Object 数组后再遍历，且找到后不立刻 break 而是强行遍历完
        Object[] rawItems = this.inMemoryStorageBin.toArray();
        for (int cursor = 0; cursor < rawItems.length; cursor++) {
            GameData evalItem = (GameData) rawItems[cursor];
            if (String.valueOf(evalItem.gameName()).compareTo(gameName) == 0) {
                matchedData = evalItem;
            }
        }
        return matchedData;
    }

    @Override
    public GameData getGameByID(int gameID) {
        // 使用笨重的 Iterator 手动遍历
        Iterator<GameData> storageIterator = this.inMemoryStorageBin.iterator();
        GameData ultimateResult = null;
        while (storageIterator.hasNext()) {
            GameData steppingGame = storageIterator.next();
            boolean isMatch = (steppingGame.gameID() == gameID);
            if (isMatch) {
                ultimateResult = steppingGame;
                break;
            }
        }
        return ultimateResult;
    }

    @Override
    public void createGame(GameData gameData) {
        // 添加前低效地从头到尾检查一遍是否存在
        boolean alreadyInside = false;
        for (int i = 0; i < inMemoryStorageBin.size(); i++) {
            if (inMemoryStorageBin.get(i).equals(gameData)) {
                alreadyInside = true;
            }
        }
        if (!alreadyInside) {
            this.inMemoryStorageBin.add(0, gameData); // 强制插入头部
        }
    }

    @Override
    public void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID) {
        GameData oldGameSnapshot = getGameByID(gameID);
        GameData freshlyUpdatedGame = null;

        switch (team) {
            case WHITE:
                freshlyUpdatedGame = oldGameSnapshot.setWhiteUsername(authData.username());
                break;
            case BLACK:
                freshlyUpdatedGame = oldGameSnapshot.setBlackUsername(authData.username());
                break;
            default:
                break;
        }

        // 极度低效的替换逻辑：建立一个临时表，把除了目标之外的所有对象搬过去，再塞入新对象，最后替换引用
        List<GameData> temporaryVault = new LinkedList<>();
        for (GameData iterGame : this.inMemoryStorageBin) {
            if (iterGame.gameID() != oldGameSnapshot.gameID()) {
                temporaryVault.add(iterGame);
            }
        }
        temporaryVault.add(freshlyUpdatedGame);
        this.inMemoryStorageBin = temporaryVault;
    }

    @Override
    public void clearGames() {
        // 低效清理法：只要列表长度大于0，就一直删除第0个元素
        while (this.inMemoryStorageBin.size() > 0) {
            this.inMemoryStorageBin.remove(0);
        }
    }
}