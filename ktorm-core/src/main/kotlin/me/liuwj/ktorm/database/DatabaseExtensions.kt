package me.liuwj.ktorm.database

import me.liuwj.ktorm.expression.SqlExpression
import me.liuwj.ktorm.schema.SqlType
import org.slf4j.Logger
import java.sql.Connection
import java.sql.PreparedStatement

/**
 * Shortcut for Database.global.useConnection
 */
inline fun <T> useConnection(block: (Connection) -> T): T {
    return Database.global.useConnection(block)
}

/**
 * Shortcut for Database.global.transactional
 */
fun <T> transactional(block: () -> T): T {
    return Database.global.transactional(block)
}

/**
 * Format current sql expression to sql string, and create a prepared statement via global database's connection
 */
inline fun <T> SqlExpression.prepareStatement(
    autoGeneratedKeys: Boolean = false,
    block: (PreparedStatement, Logger) -> T
): T {
    val database = Database.global
    val logger = database.logger

    val (sql, args) = database.formatExpression(this)

    if (logger.isDebugEnabled) {
        logger.debug("SQL: $sql")
        logger.debug("Parameters: " + args.map { "${it.value}(${it.sqlType.typeName})" })
    }

    database.useConnection { conn ->
        val statement = if (autoGeneratedKeys) {
            conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
        } else {
            conn.prepareStatement(sql)
        }

        statement.use {
            args.forEachIndexed { i, expr ->
                @Suppress("UNCHECKED_CAST")
                val sqlType = expr.sqlType as SqlType<Any>
                sqlType.setParameter(statement, i + 1, expr.value)
            }

            return block(statement, logger)
        }
    }
}