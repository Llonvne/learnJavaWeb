package cn.llonvne.learnJavaWeb.databse

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

private object Database {
    val connection: Connection by lazy {

        val conn = DriverManager.getConnection(
            "jdbc:mysql://42.192.148.93:3306/orderingsystem",
            System.getenv("db_username"),
            System.getenv("db_pass")
        )
        conn
    }
}

val connection: Connection by lazy {
    Database.connection
}

/**
 * @param sql 等待处理的 SQL 语句
 * @param processor 处理 ResultSet 的函数
 */
fun executeQuery(sql: String, processor: (ResultSet) -> Unit) {
    Database.connection.createStatement().use {
        it.executeQuery(sql).use(processor)
    }
}

fun execute(sql: String): Boolean {
    Database.connection.createStatement().use {
        return it.execute(sql)
    }
}

fun transaction(
    sqls: List<String>,
    rollbackEach: (Boolean) -> Boolean = { _ -> false },
    rollbackEnd: () -> Boolean,
): Boolean {
    Database.connection.autoCommit = false
    for (sql in sqls) {
        if (rollbackEach(execute(sql))) {
            Database.connection.rollback();
            Database.connection.autoCommit = true
            return false
        }
    }
    if (rollbackEnd()) {
        Database.connection.rollback();
        Database.connection.autoCommit = true
        return false
    }
    Database.connection.autoCommit = true
    return true
}