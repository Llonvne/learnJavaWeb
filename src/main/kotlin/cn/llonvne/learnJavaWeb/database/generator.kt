package cn.llonvne.learnJavaWeb.database

import java.io.File

data class Filed(val name: String, val cls: Class<*>)
data class TableStruct(val tableName: String, val fileds: MutableList<Filed>)

fun generator(tableName: String) {
    val classTemplate = StringBuilder(
        """package cn.llonvne.learnJavaWeb.database
class """
    )

    classTemplate.append("${tableName}(map:Map<String,Any>){\n")
    for (filed in Database.getTableRegistry(tableName)!!.fileds) {
        classTemplate.append("val ${filed.name}:${filed.cls.simpleName} by map \n")
    }

    classTemplate.append("}\n")
    File("src/main/kotlin/cn/llonvne/learnJavaWeb/database/$tableName.kt").writeText(classTemplate.toString())
}

fun generateTableStruct(tableName: String): TableStruct {
    val table = TableStruct(tableName, mutableListOf())
    executeQuery("select * from $tableName") {
        for (i in 1..it.metaData.columnCount) {
            table.fileds.add(Filed(it.metaData.getColumnLabel(i), Class.forName(it.metaData.getColumnClassName(i))))
        }
    }
    return table
}
