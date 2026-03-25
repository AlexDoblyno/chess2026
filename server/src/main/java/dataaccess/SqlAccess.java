package dataaccess;

import java.sql.SQLException;

public interface SqlAccess {

    // 强制实现类提供自己的建表方法
    void configureDatabase() throws ServerException, DataAccessException;

    // 默认的 executeUpdate 方法，所有实现了 SqlAccess 的类都可以直接用
    default int executeUpdate(String statement, Object... params) throws ServerException {
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            for (int i = 0; i < params.length; i++) {
                preparedStatement.setObject(i + 1, params[i]);
            }
            return preparedStatement.executeUpdate();
        } catch (SQLException | DataAccessException e) {
            throw new ServerException("Update failed: " + e.getMessage());
        }
    }

    // 默认的数据库配置逻辑，传入具体的建表语句即可
    default void configureDatabase(String[] createStatements) throws ServerException, DataAccessException {
        DatabaseManager.createDatabase();
        try (var conn = DatabaseManager.getConnection()) {
            for (var statement : createStatements) {
                try (var preparedStatement = conn.prepareStatement(statement)) {
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new ServerException(e.getMessage());
        }
    }
}