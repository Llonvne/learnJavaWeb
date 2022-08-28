package cn.llonvne.learnJavaWeb.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KCallable
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.javaType
import kotlin.system.exitProcess


object Database {
    /**
     * 数据库链接对象
     */
    internal val connection: Connection by lazy {
        val conn = DriverManager.getConnection(
            "jdbc:mysql://42.192.148.93:3306/orderingsystem",
            System.getenv("db_username"),
            System.getenv("db_pass")
        )
        conn
    }

    /**
     * 数据库名称
     */
    val name: String = let {
        var name = ""
        executeQuery("select database();") {
            while (it.next()) {
                name = it.getString(1)
            }
        }
        name
    }

    /**
     * 数据表名
     */
    val tableNames: List<String> = let {
        val tables = mutableListOf<String>()
        executeQuery("select table_name from information_schema.tables where table_schema='$name'") {
            while (it.next()) {
                tables.add(it.getString(1))
            }
        }
        tables
    }

    private val tableRegistries: MutableMap<String, TableStruct> by lazy {
        val map: MutableMap<String, TableStruct> = mutableMapOf()
        for (tableName in tableNames) {
            map[tableName] = generateTableStruct(tableName)
        }
        map
    }

    fun getTableRegistry(tableName: String): TableStruct? {
        return tableRegistries[tableName]
    }
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

typealias DataClassMap = Map<String, Any>

data class IdWithType(val id: Int, val type: KCallable<*>)

/**
 * @param resultSet 数据源，该函数会读取整个resultSet直到处理完毕
 * @param T 需要转换为的数据类型
 * @return List<Map<String,Any>>
 */
inline fun <reified T> toDataClassMap(resultSet: ResultSet): List<DataClassMap> {
    // 解析传入的类型 T
    val fields: List<IdWithType> = T::class.declaredMembers.filter {
        try {
            resultSet.findColumn(it.name)
            true
        } catch (e: SQLException) {
            false
        }
    }.map { IdWithType(resultSet.findColumn(it.name), it) }
    val maps = mutableListOf<Map<String, Any>>()
    while (resultSet.next()) {
        val map = mutableMapOf<String, Any>()
        for (field in fields) {
            map[field.type.name] = resultSet.getObject(field.id, field.type.returnType.javaType as Class<*>)
        }
        maps.add(map)
    }
    return maps
}

inline fun <reified T> toDataClassMap(tableName: String): List<T> {
    var res: List<T> = listOf()

    executeQuery("select * from $tableName") {
        res = toDataClassMap<T>(it).map { map ->
            T::class.constructors.first().call(map)
        }
    }
    return res
}

fun checkDatabseDataClassComplete() {
    var notComplete = false
    for (tablename in Database.tableNames) {
        try {
            Class.forName("cn.llonvne.learnJavaWeb.database.$tablename")
        } catch (e: ClassNotFoundException) {
            generator(tablename)
            notComplete = true
        }
    }
    if (notComplete) {
        println("DatabaseUpdater: 数据库类已被重新生成，请重启以加载数据类")
        exitProcess(-1)
    }
}
