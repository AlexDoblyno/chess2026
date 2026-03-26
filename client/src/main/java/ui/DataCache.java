package ui;

import models.GameData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// DataCache is meant to hold data that might need to persist between REPL loops or clients.
// DataCache 用于保存可能需要在 REPL 循环或客户端之间持久化的数据。
public class DataCache {
    private String authToken;
    private int currentGameID;
    private List<GameData> gameCache;

    // Empty constructor
    public DataCache() {
        authToken = null;
        currentGameID = 0;
        gameCache = new ArrayList<>();
    }

    public DataCache(String authToken, int gameID, Collection<GameData> gameList) {
        this.authToken = authToken;
        currentGameID = gameID;
        gameCache = new ArrayList<GameData>(gameList);
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public int getCurrentGameID() {
        return currentGameID;
    }

    public void setCurrentGameID(int currentGameID) {
        this.currentGameID = currentGameID;
    }

    // 🚨 核心修复：防止输入不存在的房间号导致 IndexOutOfBoundsException 崩溃
    public GameData getGameByIndex(int index) {
        try {
            return gameCache.get(index - 1);
        } catch (IndexOutOfBoundsException e) {
            // 如果越界（比如只有5个游戏却输入了6），温柔地返回 null
            return null;
        }
    }

    public void setGameCache(Collection<GameData> gameCache) {
        this.gameCache = new ArrayList<GameData>(gameCache);
    }
}