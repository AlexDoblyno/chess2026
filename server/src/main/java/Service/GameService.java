package service;

import chess.ChessGame;
import dataaccess.AuthDataAccess;
import dataaccess.DataAccessException;
import dataaccess.GameDataAccess;
import dataaccess.ServerException;
import dataaccess.UserDataAccess;
import models.GameData;

import java.security.SecureRandom;
import java.util.Collection;

/**
 * GameService: 用于提供与“游戏”相关的业务功能，例如列出现有游戏、创建新游戏、加入游戏等。
 * <p>
 * 关于随机数生成的部分思路，参考：
 * <a href="https://stackoverflow.com/questions/13992972/how-to-create-an-authentication-token-using-java">
 *     StackOverflow: how-to-create-an-authentication-token-using-java
 * </a>
 * <p>
 * 在设计随机ID生成时，还可结合CSDN上的部分思路（例如相关UUID方案），以进一步增强唯一性：
 * <a href="https://blog.csdn.net/pearl0506/article/details/78919920">
 *     CSDN: 关于由 SecureRandom 推导 UUID 的示例
 * </a>
 */
public class GameService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserDataAccess userDataAccess;
    private final GameDataAccess gameDataAccess;
    private final AuthDataAccess authDataAccess;

    /**
     * 构造方法：注入所需的 DAO 接口
     *
     * @param userDAO 用户数据访问接口
     * @param gameDAO 游戏数据访问接口
     * @param authDAO 认证数据访问接口
     */
    public GameService(UserDataAccess userDAO, GameDataAccess gameDAO, AuthDataAccess authDAO) {
        this.userDataAccess = userDAO;
        this.gameDataAccess = gameDAO;
        this.authDataAccess = authDAO;
    }

    /**
     * 校验传入的认证令牌是否有效。
     * 若无效，则抛出 DataAccessException。
     *
     * @param authToken 需要验证的令牌
     * @throws DataAccessException 调用底层服务异常或令牌查找失败时抛出
     */
    public void verifyAuthToken(String authToken) throws DataAccessException {
        try {
            if (authDataAccess.getAuthData(authToken) == null) {
                throw new DataAccessException("Invalid Auth Token");
            }
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * 如果传入的令牌有效，则返回系统中所有游戏的集合。
     *
     * @param authToken 认证令牌
     * @return 存在的游戏集合
     * @throws DataAccessException 若令牌或底层查询出现问题
     */
    public Collection<GameData> listGames(String authToken) throws DataAccessException {
        // 先检查令牌
        try {
            if (authDataAccess.getAuthData(authToken) == null) {
                throw new DataAccessException("Invalid Auth Token");
            }
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 再获取游戏列表
        try {
            return gameDataAccess.getGameList();
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * 创建新的棋局，若名称已存在则抛出异常，不允许重复游戏名。
     * ID 生成算法部分参考 SecureRandom 产生随机正整数。
     *
     * @param gameName 新游戏名称
     * @return 新创建游戏的 ID
     * @throws DataAccessException 如果名称无效/已被占用/或底层出现异常
     */
    public Integer createGame(String gameName) throws DataAccessException {
        if (gameName == null || gameName.trim().isEmpty()) {
            throw new DataAccessException("Invalid parameter");
        }

        try {
            if (gameDataAccess.getGameByName(gameName) == null) {
                // 若未找到同名游戏，则创建新的
                ChessGame newGame = new ChessGame();
                int gameID = generateGameID();

                // 确保ID在数据库中无重复
                while (gameDataAccess.getGameByID(gameID) != null) {
                    gameID = generateGameID();
                }
                GameData newGameData = new GameData(gameID, null, null, gameName, newGame);
                gameDataAccess.createGame(newGameData);
                return gameID;
            } else {
                throw new DataAccessException("already taken");
            }
        } catch (ServerException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    /**
     * 让拥有有效认证令牌的用户加入指定的游戏。
     * 如果游戏不存在、颜色未知或颜色已占用，会抛出异常。
     *
     * @param authToken   用户的认证令牌
     * @param gameID      要加入的游戏ID
     * @param playerColor 加入时所选的队伍颜色（WHITE或BLACK）
     * @throws DataAccessException 当令牌无效、游戏不存在、或颜色被占用时抛出
     */
    public void joinGame(String authToken, Integer gameID, ChessGame.TeamColor playerColor) throws DataAccessException {
        // 先获取用户名
        String username;
        try {
            username = authDataAccess.getAuthData(authToken).username();
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 检查传入的 gameID
        if (gameID == null) {
            throw new DataAccessException("Invalid gameID");
        }

        // 查找游戏并验证可行性
        try {
            GameData existingGame = gameDataAccess.getGameByID(gameID);
            if (existingGame == null) {
                throw new DataAccessException("Error: bad request");
            }

            // 检查颜色是否合法
            if (playerColor != ChessGame.TeamColor.WHITE && playerColor != ChessGame.TeamColor.BLACK) {
                throw new IllegalArgumentException("Error: Invalid team color");
            }

            // 若该颜色位已被占用，则拒绝
            boolean whiteTaken = (playerColor == ChessGame.TeamColor.WHITE && existingGame.whiteUsername() != null);
            boolean blackTaken = (playerColor == ChessGame.TeamColor.BLACK && existingGame.blackUsername() != null);
            if (whiteTaken || blackTaken) {
                throw new DataAccessException("Error: Player color already taken.");
            }

            // 更新数据库，使之在该颜色处绑定用户名
            gameDataAccess.updateGame(playerColor, gameID, username);

        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * 产生一个随机的正整数游戏ID。
     * 利用 {@linkplain SecureRandom SecureRandom} 提高随机性。
     *
     * @return 随机正整数
     */
    private int generateGameID() {
        byte[] randomBytes = new byte[4];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Math.abs(java.nio.ByteBuffer.wrap(randomBytes).getInt());
    }
}