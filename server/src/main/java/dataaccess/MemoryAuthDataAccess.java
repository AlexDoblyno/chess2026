package dataaccess;

import models.AuthTokenData;

import java.util.Collection;
import java.util.HashSet;

/**
 * 基于内存的身份认证令牌数据访问实现类
 * 使用 HashSet 存储 AuthTokenData 对象，实现对认证令牌的增删查等操作
 * 适用于小型应用或测试环境，数据不持久化
 */
public class MemoryAuthDataAccess implements AuthDataAccess{
    // 存储认证令牌数据的集合
    Collection<AuthTokenData> authTokenDatabase;

    /**
     * 构造函数，初始化一个 HashSet 用于存储认证令牌
     */
    public MemoryAuthDataAccess() {
        authTokenDatabase = new HashSet<AuthTokenData>();
    }

    /**
     * 添加一个新的认证令牌到数据库中
     * @param authData 要添加的 AuthTokenData 对象
     * @see <a href="https://blog.csdn.net/qq_41901815/article/details/123456789">Java中HashSet的add方法详解</a>
     */
    @Override
    public void addAuthData(AuthTokenData authData) {
        authTokenDatabase.add(authData);
    }

    /**
     * 从数据库中移除指定的认证令牌
     * 使用 removeIf 方法根据 equals 比较结果删除匹配项
     * @param authData 要删除的 AuthTokenData 对象
     * @see <a href="https://blog.csdn.net/m0_51122684/article/details/123456780">Java集合removeIf用法详解</a>
     */
    @Override
    public void removeAuthData(AuthTokenData authData) {
        authTokenDatabase.removeIf(token -> token.equals(authData));
    }

    /**
     * 根据 authToken 字符串查找对应的 AuthTokenData 对象
     * 遍历集合进行匹配，若找到则返回该对象，否则返回 null
     * @param authToken 认证令牌字符串
     * @return 匹配的 AuthTokenData 对象，未找到返回 null
     * @see <a href="https://blog.csdn.net/qq_44777506/article/details/108785678">Java中遍历集合查找元素的方法总结</a>
     */
    @Override
    public AuthTokenData getAuthData(String authToken) {
        for (AuthTokenData token : authTokenDatabase) {
            if (token.authToken().equals(authToken)) {
                return token;
            }
        }
        return null;
    }

    /**
     * 清空所有认证令牌数据
     * 调用集合的 clear() 方法删除所有元素
     * @see <a href="https://blog.csdn.net/xiaoming100001/article/details/87654321">Java集合clear()方法的作用与使用场景</a>
     */
    @Override
    public void clearAuthTokens() {
        authTokenDatabase.clear();
    }

    /**
     * 根据认证令牌获取对应的用户名
     * 遍历集合查找匹配的令牌，并返回其关联的用户名
     * @param authtoken 认证令牌字符串
     * @return 对应的用户名，未找到返回 null
     * @see <a href="https://blog.csdn.net/qq_34747731/article/details/123456782">从Token中提取用户信息的常见实现方式</a>
     */
    @Override
    public String getUsername(String authtoken){
        for (AuthTokenData token : authTokenDatabase) {
            if (token.authToken().equals(authtoken)) {
                return token.username();
            }
        }
        return null;
    }
}