package dataaccess;

import java.sql.*;
import java.util.Properties;

/**
 * 数据库管理器，用于创建数据库、加载配置并获取数据库连接。
 * Best practices suggest managing connections efficiently, though this simple manager uses DriverManager directly [[3]].
 * For production applications, consider using connection pooling [[1]].
 */
public class DatabaseManager {
    private static String databaseName;
    private static String dbUsername;
    private static String dbPassword;
    private static String connectionUrl;

    /*
     * 从 db.properties 文件加载数据库信息。
     * Static initializers are used to load properties when the class is first accessed.
     */
    static {
        loadPropertiesFromResources();
    }

    /**
     * 创建数据库（如果它尚不存在）。
     * Uses a simple statement to create the database.
     * It's generally recommended to handle SQL exceptions properly [[1]].
     */
    static public void createDatabase() throws DataAccessException {
        var statement = "CREATE DATABASE IF NOT EXISTS " + databaseName;
        // Using try-with-resources ensures the connection and PreparedStatement are closed automatically [[2]].
        try (var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create database", ex);
        }
    }

    /**
     * 创建到数据库的连接，并根据 db.properties 中指定的属性设置目录。
     * 连接到数据库应该是短期的，当您完成连接后必须关闭它。
     * 最简单的方法是使用 try-with-resource 块。
     * <br/>
     * <code>
     * try (var conn = DatabaseManager.getConnection()) {
     * // execute SQL statements.
     * }
     * </code>
     * 注意：此实现直接使用 DriverManager，对于高并发应用，建议使用连接池 [[1]]。
     */
    static Connection getConnection() throws DataAccessException {
        try {
            // 不要将以下行包装在 try-with-resources 中，因为调用者需要使用连接。
            var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
            conn.setCatalog(databaseName);
            return conn;
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get connection", ex);
        }
    }

    /**
     * 从应用程序资源中加载数据库属性。
     * 使用当前线程的上下文类加载器来查找资源。
     */
    private static void loadPropertiesFromResources() {
        // 使用 try-with-resources 确保输入流被关闭。
        try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
            if (propStream == null) {
                throw new Exception("Unable to load db.properties");
            }
            Properties props = new Properties();
            props.load(propStream);
            loadProperties(props);
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties", ex);
        }
    }

    /**
     * 从 Properties 对象填充数据库连接所需的静态变量。
     * @param props 包含数据库配置的 Properties 对象。
     */
    private static void loadProperties(Properties props) {
        databaseName = props.getProperty("db.name");
        dbUsername = props.getProperty("db.user");
        dbPassword = props.getProperty("db.password");

        var host = props.getProperty("db.host");
        var port = Integer.parseInt(props.getProperty("db.port"));
        // 构建 JDBC 连接 URL。
        connectionUrl = String.format("jdbc:mysql://%s:%d", host, port);
    }
}