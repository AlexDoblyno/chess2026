package dataaccess;

import models.AuthTokenData;

/**
 * 定义用于访问认证数据（如 AuthToken）的数据访问接口。
 * 实现此接口的类应处理与存储认证信息相关的所有数据库交互。
 */
public interface AuthDataAccess {

    /**
     * 添加新的认证数据。
     *
     * @param authData 要添加的 AuthTokenData 对象。
     * @throws ServerException 如果添加过程中发生错误。
     */
    void addAuthData(AuthTokenData authData) throws ServerException;

    /**
     * 根据提供的认证数据对象移除对应的记录。
     *
     * @param authData 要移除的 AuthTokenData 对象。
     * @throws ServerException 如果移除过程中发生错误。
     */
    void removeAuthData(AuthTokenData authData) throws ServerException;

    /**
     * 根据认证令牌字符串检索认证数据。
     *
     * @param authToken 用于查找的认证令牌字符串。
     * @return 对应的 AuthTokenData 对象。
     * @throws ServerException 如果检索过程中发生错误或未找到数据。
     */
    AuthTokenData getAuthData(String authToken) throws ServerException;

    /**
     * 清除（删除）所有存储的认证令牌数据。
     * 此操作通常用于测试或管理目的。
     *
     * @throws ServerException 如果清除过程中发生错误。
     */
    void clearAuthTokens() throws ServerException;

    /**
     * 根据特定的认证令牌查找对应的用户名。
     *
     * @param authToken 用于查找的认证令牌字符串。
     * @return 与认证令牌关联的用户名。
     * @throws ServerException 如果查找过程中发生错误或未找到对应的用户名。
     */
    String getUsername(String authToken) throws ServerException;
}