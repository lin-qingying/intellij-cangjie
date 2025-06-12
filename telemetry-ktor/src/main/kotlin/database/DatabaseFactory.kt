package cn.cangnova.database

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KotlinLogging
import kotlin.reflect.KClass

/**
 * 数据库类型枚举
 */
enum class DatabaseType {
    MONGODB,
    MYSQL,
    POSTGRESQL;
    
    companion object {
        fun fromString(value: String): DatabaseType {
            return when (value.lowercase()) {
                "mongodb" -> MONGODB
                "mysql" -> MYSQL
                "postgresql" -> POSTGRESQL
                else -> throw IllegalArgumentException("Unsupported database type: $value")
            }
        }
    }
}

/**
 * 数据库配置接口
 */
interface DatabaseConfig {
    /**
     * 初始化数据库连接
     */
    fun init()
    
    /**
     * 关闭数据库连接
     */
    fun close()
    
    /**
     * 获取数据库类型
     */
    fun getType(): DatabaseType


}

/**
 * 数据库配置工厂类
 */
object DatabaseFactory {
    private val logger = KotlinLogging.logger {}
    private var databaseConfig: DatabaseConfig? = null
    
    /**
     * 初始化数据库配置
     */
    fun init() {
        val config = ConfigFactory.load()
        val dbType = try {
            DatabaseType.fromString(config.getString("database.type"))
        } catch (e: Exception) {
            logger.warn { "Failed to get database type from config, using MongoDB as default: ${e.message}" }
            DatabaseType.MONGODB
        }
        
        databaseConfig = when (dbType) {
            DatabaseType.MONGODB -> MongoDBConfig(config)
            DatabaseType.MYSQL -> MySQLConfig(config)
            DatabaseType.POSTGRESQL -> PostgreSQLConfig(config)
        }
        
        try {
            databaseConfig?.init()
            logger.info { "Database initialized: ${dbType.name}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize database: ${e.message}" }
            throw e
        }
    }
    
    /**
     * 关闭数据库连接
     */
    fun close() {
        try {
            databaseConfig?.close()
            logger.info { "Database connection closed" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to close database connection: ${e.message}" }
        }
    }
    
    /**
     * 获取数据库配置
     */
    fun getConfig(): DatabaseConfig {
        return databaseConfig ?: throw IllegalStateException("Database not initialized")
    }
    
    /**
     * 获取特定类型的数据库配置
     */
    inline fun <reified T : DatabaseConfig> getTypedConfig(): T {
        val config = getConfig()
        return if (config is T) {
            config
        } else {
            throw IllegalStateException("Database config is not of type ${T::class.simpleName}")
        }
    }
} 