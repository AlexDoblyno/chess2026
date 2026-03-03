package dataaccess;

import models.UserData;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * SqlUserDataAccess：采用 SQL 数据库 方式来实现对 UserData 的增删改查操作。
 * <p>
 * 配合 {@link DatabaseManager} 来获取或创建数据库连接。若在执行 SQL 语句时出现问题，会抛出 {@link ServerException}。
 *
 * <p>参考链接：
 * <a href="https://blog.csdn.net/weixin_41189588/article/details/90225448">
 *     CSDN：Java数据库连接与PreparedStatement示例
 * </a>
 */
public class SqlUserDataAccess implements UserDataAccess, SqlAccess {

    /**
     * 初始化时尝试创建（或更新）数据库结构
     */
    public SqlUserDataAccess() {
        try {
            configureDatabase();
        } catch (ServerException e) {
            // 可以在此处记录日志，或进一步处理
            return;
        }
    }

    /**
     * 根据指定用户名获取用户信息。
     * <p>如果不存在对应用户名，返回null。此外，也可能抛出异常。
     *
     * @param username 要查询的用户名
     * @return 对应的UserData，若不存在则返回null
     * @throws ServerException         数据库访问失败
     * @throws server.ServerException  项目中另一版本的ServerException
     */
    @Override
    public UserData getUserData(String username) throws ServerException, server.ServerException {
        Connection conn;
        try {
            conn = DatabaseManager.getConnection();
        } catch (DataAccessException e) {
            throw new ServerException("Error Userdata get failed: " + e.getMessage());
        }

        var fetch = "SELECT * FROM UserData WHERE username = ?";
        try {
            UserData response = getUserDataFetch(username, conn, fetch);
            if (response.username() != null && response.password() != null && response.email() != null) {
                return response;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("not found")) {
                // 未找到对应用户
                return null;
            } else {
                throw new ServerException("Error: Userdata get failed: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 将新的用户记录插入到数据库中。
     *
     * @param userData 要添加的UserData对象
     * @throws ServerException 若数据库插入操作出错
     */
    @Override
    public void addUserData(UserData userData) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var insert = "INSERT INTO UserData (username, password, email) VALUES (?, ?, ?)";

            try (var preparedStatement = conn.prepareStatement(insert)) {
                preparedStatement.setString(1, userData.username());
                preparedStatement.setString(2, userData.password());
                preparedStatement.setString(3, userData.email());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new ServerException("Userdata add failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("Userdata add failed: " + e.getMessage());
        }
    }

    /**
     * 清空所有用户数据，仅在测试或管理员操作时使用。
     *
     * @throws ServerException 若执行删除操作失败
     */
    @Override
    public void clearUsers() throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            var clear = "DELETE FROM UserData";

            try (var preparedStatement = conn.prepareStatement(clear)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new ServerException("UserData clear failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("UserData clear failed: " + e.getMessage());
        }
    }

    /**
     * 执行通用的数据库更新或插入操作。
     *
     * @param statement SQL更新语句
     * @param params    可变参，用于PreparedStatement参数
     * @return SQL执行后所影响的行数
     * @throws ServerException 若数据库访问出错
     */
    @Override
    public int executeUpdate(String statement, Object... params) throws ServerException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement(statement)) {
                for (int i = 0; i < params.length; i++) {
                    preparedStatement.setObject(i + 1, params[i]);
                }
                return preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new ServerException("Update failed: " + e.getMessage());
        } catch (DataAccessException e) {
            throw new ServerException("Update failed: " + e.getMessage());
        }
    }

    /**
     * 创建并初始化UserData表（若不存在）。
     *
     * @throws ServerException 数据库操作失败
     */
    @Override
    public void configureDatabase() throws ServerException {
        try {
            DatabaseManager.createDatabase();
        } catch (DataAccessException e) {
            throw new ServerException(e.getMessage());
        }

        try (var conn = DatabaseManager.getConnection()) {
            for (var statement : createStatements) {
                try (var preparedStatement = conn.prepareStatement(statement)) {
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException | DataAccessException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * 辅助方法，用于在 getUserData 中执行数据库检索。
     *
     * @param username 待查询用户名
     * @param conn     数据库连接
     * @param fetch    SQL查询语句
     * @return 如果成功，则返回用户信息，否则抛出异常
     * @throws SQLException 若查询不到用户或执行失败
     */
    private static UserData getUserDataFetch(String username, Connection conn, String fetch) throws SQLException {
        try (var preparedStatement = conn.prepareStatement(fetch)) {
            preparedStatement.setString(1, username);

            try (var response = preparedStatement.executeQuery()) {
                try {
                    response.next();
                    return new UserData(response.getString("username"),
                            response.getString("password"),
                            response.getString("email"));
                } catch (SQLException e) {
                    // “User not found”，抛出以便上层捕获并处理
                    throw new SQLException("User not found");
                }
            }
        }
    }

    /**
     * 创建UserData表(若不存在)的SQL语句集合
     */
    private final String[] createStatements = {
            """
                CREATE TABLE IF NOT EXISTS UserData (
                `username` VARCHAR(256) NOT NULL PRIMARY KEY,
                `password` VARCHAR(60) NOT NULL,
                `email` VARCHAR(256) NOT NULL UNIQUE
                )
            """
    };
}