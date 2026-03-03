package service;

import dataaccess.AuthDataAccess;
import dataaccess.DataAccessException;
import dataaccess.ServerException;
import dataaccess.UserDataAccess;
import models.AuthTokenData;
import models.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 针对一个注册功能服务的改写示例：
 * 1. 不改变方法和字段命名，以保证与项目其他部分兼容。
 * 2. 调整部分结构顺序，并增加外部链接注释。
 *
 * <p>BCrypt安全参考：
 * <a href="https://stackoverflow.com/questions/516809">https://stackoverflow.com/questions/516809</a>
 */
public class RegisterService {

    private final UserDataAccess userDataDAO;
    private final AuthDataAccess authDataDAO;

    /**
     * 随机数生成器（参考：
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/security/SecureRandom.html">
     * Oracle SecureRandom文档
     * </a>）
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Base64编码器（参考：
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Base64.html">
     * 位于java.util的Base64
     * </a>）
     */
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();

    /**
     * 构造方法：注入用户和认证数据访问层对象
     *
     * @param userDA  用户数据访问类
     * @param authDA  认证数据访问类
     */
    public RegisterService(UserDataAccess userDA, AuthDataAccess authDA) {
        this.userDataDAO = userDA;
        this.authDataDAO = authDA;
    }

    /**
     * 创建新用户，并在数据库中存储其哈希密码和对应的唯一认证令牌。
     *
     * <p>核心步骤：
     * <ol>
     *     <li>校验参数</li>
     *     <li>查询是否存在同名用户</li>
     *     <li>BCrypt进行密码哈希</li>
     *     <li>存入数据库并生成AuthToken</li>
     * </ol>
     *
     * <p>随机令牌生成参考：
     * <a href="https://stackoverflow.com/questions/13992972">
     * how-to-create-an-authentication-token-using-java
     * </a>
     *
     * @param username  新用户的用户名
     * @param password  新用户的明文密码
     * @param email     新用户的邮箱
     * @return          生成的认证令牌(以JSON形式)
     * @throws DataAccessException 如果用户名已存在或其他数据问题
     */
    public String createNewUser(String username, String password, String email) throws DataAccessException {
        validateInputs(username, password, email);

        // 确保用户名未占用
        try {
            if (userDataDAO.getUserData(username) != null) {
                throw new DataAccessException("Username is already taken");
            }
        } catch (ServerException | server.ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        AuthTokenData authTokenData;
        try {
            // 使用BCrypt进行密码哈希
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            UserData newUserObj = new UserData(username, hashedPassword, email);

            // 存储用户信息
            userDataDAO.addUserData(newUserObj);

            // 生成并存储令牌
            authTokenData = new AuthTokenData(generateUniqueAuthToken(), newUserObj.username());
            authDataDAO.addAuthData(authTokenData);

        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        // 返回JSON串形式的令牌
        return authTokenData.toString();
    }

    /**
     * 生成唯一认证令牌。当出现冲突（重复令牌）时会重新生成。
     * 参考链接：
     * <a href="https://stackoverflow.com/questions/13992972">
     * how-to-create-an-authentication-token-using-java
     * </a>
     *
     * @return 不可重复碰撞的令牌
     * @throws ServerException 如果查询令牌可用性时出现错误
     */
    private String generateUniqueAuthToken() throws ServerException {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String candidate = ENCODER.encodeToString(randomBytes);

        try {
            if (authDataDAO.getAuthData(candidate) != null) {
                // 发现冲突则递归再次生成
                return generateUniqueAuthToken();
            }
        } catch (dataaccess.ServerException e) {
            if (e.getMessage().contains("not found")) {
                // 未查找到说明可用
                return candidate;
            } else {
                new server.ServerException(e.getMessage(), 500);
            }
        }
        return candidate;
    }

    /**
     * 校验用户名、密码、邮箱是否有空值，如果存在则抛出IllegalArgumentException
     *
     * @param username 待测用户名
     * @param password 待测密码
     * @param email    待测邮箱
     * @throws IllegalArgumentException 若存在空值或空字符串
     */
    private void validateInputs(String username, String password, String email) {
        if (isBlank(username) || isBlank(password) || isBlank(email)) {
            throw new IllegalArgumentException("Username, password, and email are required");
        }
    }

    /**
     * 检查字符串是否为空或null
     *
     * @param value 待检测字符串。
     * @return 如果为空或null则返回true
     */
    private boolean isBlank(String value) {
        return (value == null || value.isEmpty());
    }
}