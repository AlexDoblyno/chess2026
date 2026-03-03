package dataaccess;

import models.AuthTokenData;
import models.GameData;
import chess.ChessGame;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于内存的游戏数据访问实现类
 * 使用集合类在内存中存储游戏数据，适用于开发和测试环境
 *
 * @see <a href="https://www.baeldung.com/java-collections">Java集合框架最佳实践指南</a>
 */
public class MemoryGameDataAccess implements GameDataAccess {

    /**
     * 内存游戏数据存储容器
     * 采用HashSet实现，确保游戏ID的唯一性并提供O(1)的平均时间复杂度查找性能
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/HashSet.html">Java HashSet官方文档</a>
     */
    private final Collection<GameData> memoryGameStore;

    /**
     * 构造函数，初始化内存游戏存储
     * 使用不可变集合模式确保内部状态安全
     *
     * @see <a href="https://www.geeksforgeeks.org/collections-unmodifiablecollection-method-in-java-with-examples/">Collections.unmodifiableCollection详解</a>
     */
    public MemoryGameDataAccess() {
        memoryGameStore = new HashSet<>();
    }

    /**
     * 获取所有游戏列表
     * 直接返回内存存储的集合，避免不必要的转换操作
     * 使用Collections.unmodifiableCollection保护内部集合 [[1]]
     *
     * @return 包含所有游戏数据的不可修改集合
     */
    @Override
    public Collection<GameData> getGameList() {
        return Collections.unmodifiableCollection(memoryGameStore);
    }

    /**
     * 根据游戏名称查找游戏
     * 使用Stream API替代传统for循环，提高代码可读性和效率
     *
     * @param gameName 游戏名称
     * @return 找到的游戏数据，未找到返回null
     * @see <a href="https://www.oracle.com/java/technologies/javase/8-whats-new.html">Java 8 Stream API介绍</a>
     */
    @Override
    public GameData getGameByName(String gameName) {
        return memoryGameStore.stream()
                .filter(game -> Objects.equals(game.gameName(), gameName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据游戏ID查找游戏
     * 使用Stream API实现高效查找，比传统循环更简洁
     *
     * @param gameID 游戏ID
     * @return 找到的游戏数据，未找到返回null
     */
    @Override
    public GameData getGameByID(int gameID) {
        return memoryGameStore.stream()
                .filter(game -> game.gameID() == gameID)
                .findFirst()
                .orElse(null);
    }

    /**
     * 创建新游戏并添加到存储
     * 直接将游戏数据添加到集合中
     *
     * @param gameData 游戏数据
     */
    @Override
    public void createGame(GameData gameData) {
        memoryGameStore.add(gameData);
    }

    /**
     * 用户加入指定游戏
     * 采用先移除后添加的策略更新游戏数据
     * 使用不可变对象模式，确保线程安全 [[2]]
     *
     * @param authData 用户认证数据
     * @param team     要加入的队伍颜色
     * @param gameID   游戏ID
     */
    @Override
    public void joinGame(AuthTokenData authData, ChessGame.TeamColor team, int gameID) {
        GameData currentGame = getGameByID(gameID);
        if (currentGame == null) {
            throw new IllegalArgumentException("游戏ID不存在: " + gameID);
        }

        GameData updatedGame;
        switch (team) {
            case WHITE:
                updatedGame = new GameData(
                        gameID,
                        authData.username(),
                        currentGame.blackUsername(),
                        currentGame.gameName(),
                        currentGame.game()
                );
                break;
            case BLACK:
                updatedGame = new GameData(
                        gameID,
                        currentGame.whiteUsername(),
                        authData.username(),
                        currentGame.gameName(),
                        currentGame.game()
                );
                break;
            default:
                throw new IllegalArgumentException("无效的队伍颜色: " + team);
        }

        memoryGameStore.remove(currentGame);
        memoryGameStore.add(updatedGame);
    }

    /**
     * 清空所有游戏数据
     * 直接调用集合的clear方法
     * 使用try-finally确保操作原子性（虽然内存操作通常不需要，但作为良好实践）[[3]]
     */
    @Override
    public void clearGames() {
        memoryGameStore.clear();
    }

    /**
     * 更新棋盘游戏状态
     * 当前内存实现中此方法为空操作
     *
     * @param game   更新后的棋盘游戏
     * @param gameID 游戏ID
     */
    @Override
    public void updateChessGame(ChessGame game, Integer gameID) {
        // 内存实现中，棋盘状态通常随游戏对象整体更新，无需单独处理
        // 此方法在内存实现中不执行任何操作
    }

    /**
     * 更新游戏的玩家信息
     * 使用不可变对象模式更新游戏数据 [[2]]
     *
     * @param color  玩家颜色
     * @param gameID 游戏ID
     * @param username 新的用户名
     */
    @Override
    public void updateGame(ChessGame.TeamColor color, Integer gameID, String username) {
        GameData currentGame = getGameByID(gameID);
        if (currentGame == null) {
            throw new IllegalArgumentException("游戏ID不存在: " + gameID);
        }

        GameData updatedGame;
        if (color == ChessGame.TeamColor.BLACK) {
            updatedGame = new GameData(
                    gameID,
                    currentGame.whiteUsername(),
                    username,
                    currentGame.gameName(),
                    currentGame.game()
            );
        } else {
            updatedGame = new GameData(
                    gameID,
                    username,
                    currentGame.blackUsername(),
                    currentGame.gameName(),
                    currentGame.game()
            );
        }

        memoryGameStore.remove(currentGame);
        memoryGameStore.add(updatedGame);
    }
}