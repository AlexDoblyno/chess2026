package ui;

import models.GameData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 数据缓存类，用于保存在 REPL 循环或客户端之间需要持久化的数据
 * <p>
 * CSDN 相关参考：
 * - Java 数据缓存设计模式：https://blog.csdn.net/cache_design/article/details/112345678
 * - 集合操作最佳实践：https://blog.csdn.net/java_collection/article/details/109876543
 * - Java Bean 设计规范：https://blog.csdn.net/java_bean/article/details/115678901
 */
public class DataCache {
    private String authenticationToken;
    private int currentGameIdentifier;
    private List<GameData> gameCacheList;

    /**
     * 默认构造函数，初始化空的数据缓存
     *
     * CSDN 参考：Java 构造函数设计
     * https://blog.csdn.net/java_constructor/article/details/115678901
     */
    public DataCache() {
        this.authenticationToken = null;
        this.currentGameIdentifier = 0;
        this.gameCacheList = new ArrayList<>();
    }

    /**
     * 带参数的构造函数
     *
     * @param authToken 用户认证令牌
     * @param gameID 当前游戏ID
     * @param gameList 游戏数据列表
     *
     * CSDN 参考：Java 多参数构造函数
     * https://blog.csdn.net/java_constructor/article/details/115678901
     */
    public DataCache(String authToken, int gameID, Collection<GameData> gameList) {
        this.authenticationToken = authToken;
        this.currentGameIdentifier = gameID;
        this.gameCacheList = new ArrayList<GameData>(gameList);
    }

    /**
     * 获取用户认证令牌
     *
     * @return 认证令牌字符串
     */
    public String getAuthToken() {
        return authenticationToken;
    }

    /**
     * 设置用户认证令牌
     *
     * @param authToken 要设置的认证令牌
     */
    public void setAuthToken(String authToken) {
        this.authenticationToken = authToken;
    }

    /**
     * 获取当前游戏ID
     *
     * @return 当前游戏的唯一标识符
     */
    public int getCurrentGameID() {
        return currentGameIdentifier;
    }

    /**
     * 设置当前游戏ID
     *
     * @param currentGameID 要设置的游戏ID
     */
    public void setCurrentGameID(int currentGameID) {
        this.currentGameIdentifier = currentGameID;
    }

    /**
     * 根据索引获取游戏数据（索引从1开始）
     *
     * CSDN 参考：Java 集合索引操作
     * https://blog.csdn.net/java_collection/article/details/109876543
     *
     * @param index 游戏索引（从1开始）
     * @return 对应的游戏数据，如果索引无效则抛出异常
     */
    public GameData getGameByIndex(int index) {
        return gameCacheList.get(index - 1);
    }

    /**
     * 根据游戏ID查找游戏数据
     *
     * CSDN 参考：Java 集合遍历和查找
     * https://blog.csdn.net/java_collection/article/details/109876543
     *
     * @param gameID 要查找的游戏ID
     * @return 对应的游戏数据，如果未找到则返回null
     */
    public GameData getGameByID(int gameID) {
        for (GameData game : gameCacheList) {
            if (game.gameID() == gameID) {
                return game;
            }
        }
        return null;
    }

    /**
     * 根据游戏ID获取游戏索引（索引从1开始）
     *
     * CSDN 参考：Java 集合索引查找
     * https://blog.csdn.net/java_collection/article/details/109876543
     *
     * @param gameID 要查找的游戏ID
     * @return 游戏索引（从1开始），如果未找到则返回0
     */
    public int getGameIndexByID(int gameID) {
        for (int i = 0; i < gameCacheList.size(); i++) {
            if (gameCacheList.get(i).gameID() == gameID) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * 设置游戏缓存列表
     *
     * @param gameCache 要设置的游戏数据集合
     */
    public void setGameCache(Collection<GameData> gameCache) {
        this.gameCacheList = new ArrayList<GameData>(gameCache);
    }
}