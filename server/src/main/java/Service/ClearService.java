package service;

import dataaccess.AuthDataAccess;
import dataaccess.DataAccessException;
import dataaccess.GameDataAccess;
import dataaccess.ServerException;
import dataaccess.UserDataAccess;

/**
 * ClearServiceRefactored: 提供清理游戏数据、认证数据以及用户数据的功能。
 * <p>
 * 主要参考原始的 ClearService 实现，功能保持一致，仅做了命名与结构方面的轻微调整。
 * <p>
 * 参考示例链接：
 * <a href="https://blog.csdn.net/hustspy1990/article/details/50692084">CSDN 一篇关于多层数据清理的思路（示例）</a>
 */
public class ClearService {

    /**
     * 访问游戏数据的DAO接口
     */
    private final GameDataAccess gameDataAccess;

    /**
     * 访问认证数据的DAO接口
     */
    private final AuthDataAccess authDataAccess;

    /**
     * 访问用户数据的DAO接口
     */
    private final UserDataAccess userDataAccess;

    /**
     * 构造方法：注入Game、Auth、User三个数据访问层对象
     *
     * @param gameDAO GameDataAccess对象
     * @param authDAO AuthDataAccess对象
     * @param userDAO UserDataAccess对象
     */
    public ClearService(GameDataAccess gameDAO, AuthDataAccess authDAO, UserDataAccess userDAO) {
        this.gameDataAccess = gameDAO;
        this.authDataAccess = authDAO;
        this.userDataAccess = userDAO;
    }

    /**
     * 清理存储的数据，包括：游戏记录、认证令牌以及用户数据。执行顺序为：
     * <ol>
     *     <li>调用 {@code clearGames()} 清空游戏数据</li>
     *     <li>调用 {@code clearAuthTokens()} 清空认证令牌</li>
     *     <li>调用 {@code clearUsers()} 清空用户数据</li>
     * </ol>
     *
     * @throws DataAccessException 若GameDataAccess/AuthDataAccess/UserDataAccess出现ServerException
     */
    public void clear() throws DataAccessException {
        try {
            gameDataAccess.clearGames();
            authDataAccess.clearAuthTokens();
            userDataAccess.clearUsers();
        } catch (ServerException e) {
            // 将底层ServerException封装为DataAccessException抛出，以便业务逻辑统一处理
            throw new DataAccessException(e.getMessage());
        }
    }
}