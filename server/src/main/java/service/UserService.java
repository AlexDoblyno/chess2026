package service;

import dataaccess.AuthDataAccess;
import dataaccess.DataAccessException;
import dataaccess.ServerException;
import dataaccess.UserDataAccess;
import models.AuthTokenData;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.Base64;

public class UserService {
    private final UserDataAccess userDao;
    private final AuthDataAccess authDao;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();

    public UserService(UserDataAccess userDao, AuthDataAccess authDao) {
        this.userDao = userDao;
        this.authDao = authDao;
    }

    /**
     * 方法引用备注：
     * {@link UserService#loginUser(String, String)}
     * <p>
     * 功能：验证用户凭据并生成认证令牌。
     * 参数：
     * - username: 用户名（非空）
     * - password: 明文密码（非空）
     * 返回值：认证令牌字符串
     * 异常：
     * - DataAccessException: 用户不存在、密码错误或服务器异常时抛出
     */
    public String loginUser(String username, String password) throws DataAccessException {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        try {
            if (userDao.getUserData(username) == null) {
                throw new DataAccessException("No user found");
            } else if (!BCrypt.checkpw(password, userDao.getUserData(username).password())) {
                throw new DataAccessException("Wrong password");
            }
        } catch (ServerException | server.ServerException e) {
            throw new DataAccessException(e.getMessage());
        }

        AuthTokenData authTokenData;
        try {
            authTokenData = new AuthTokenData(generateAuthToken(), username);
            authDao.addAuthData(authTokenData);
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }
        return authTokenData.toString();
    }

    public void logoutUser(String token) throws DataAccessException {
        try {
            if (authDao.getAuthData(token) == null) {
                throw new DataAccessException("Invalid token");
            }
            authDao.removeAuthData(authDao.getAuthData(token));
        } catch (ServerException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * 方法引用备注：
     * {@link UserService#generateAuthToken()}
     * <p>
     * 功能：生成一个唯一的认证令牌，确保在数据库中不重复。
     * 返回值：Base64 编码的随机字符串
     * 异常：
     * - ServerException: 无法生成唯一令牌或数据库访问失败时抛出
     */
    private String generateAuthToken() throws ServerException {
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String authToken = ENCODER.encodeToString(randomBytes);

        try {
            // Verify uniqueness
            if (authDao.getAuthData(authToken) != null) {
                return generateAuthToken();
            }
        } catch (dataaccess.ServerException e) {
            if (e.getMessage().contains("not found")) {
                return authToken;
            } else {
                new server.ServerException(e.getMessage(), 500);
            }
        }
        return authToken;
    }
}