package cn.cangnova.database

import io.ktor.server.application.*
import io.ktor.server.config.*
import mu.KotlinLogging

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
    private lateinit var config: ApplicationConfig

    /**
     * 初始化数据库配置
     */
    fun init(application: Application) {
        try {
            logger.info { "从应用程序环境获取配置" }
            config = application.environment.config

            // 检查配置是否包含 database 部分
            try {
                config.config("database")
                logger.info { "找到 database 配置部分" }
            } catch (e: Exception) {
                logger.error { "配置文件中未找到 'database' 键，配置可能未正确加载: ${e.message}" }
                throw IllegalStateException("配置文件中未找到 'database' 键")
            }

            logger.info { "成功加载配置文件" }

            // 打印配置内容以便调试
            logger.info { "配置内容:" }
            printConfig(config)

            val dbType = try {
                val typeStr = config.property("database.type").getString()
                logger.info { "数据库类型配置: $typeStr" }
                DatabaseType.fromString(typeStr)
            } catch (e: Exception) {
                logger.warn { "无法从配置获取数据库类型，使用MongoDB作为默认值: ${e.message}" }
                DatabaseType.MONGODB
            }

            databaseConfig = when (dbType) {
                DatabaseType.MONGODB -> {
                    logger.info { "初始化MongoDB配置" }
                    MongoDBConfig(config)
                }

                DatabaseType.MYSQL -> {
                    logger.info { "初始化MySQL配置" }
                    MySQLConfig(config)
                }

                DatabaseType.POSTGRESQL -> {
                    logger.info { "初始化PostgreSQL配置" }
                    PostgreSQLConfig(config)
                }
            }

            try {
                databaseConfig?.init()
                logger.info { "数据库初始化成功: ${dbType.name}" }
            } catch (e: Exception) {
                logger.error(e) { "数据库初始化失败: ${e.message}" }
                throw e
            }
        } catch (e: Exception) {
            logger.error(e) { "配置加载或数据库初始化过程中出错: ${e.message}" }
            throw e
        }
    }

    /**
     * 打印配置内容
     */
    private fun printConfig(config: ApplicationConfig) {
        try {
            logger.info { "配置根路径键:" }
            config.keys().forEach { key ->
                logger.info { "- $key" }
                try {
                    val subConfig = config.config(key)
                    subConfig.keys().forEach { subKey ->
                        logger.info { "  - $subKey" }
                    }
                } catch (e: Exception) {
                    // 不是一个配置对象，可能是一个值
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "打印配置内容时出错: ${e.message}" }
        }
    }

    /**
     * 关闭数据库连接
     */
    fun close() {
        try {
            databaseConfig?.close()
            logger.info { "数据库连接已关闭" }
        } catch (e: Exception) {
            logger.error(e) { "关闭数据库连接失败: ${e.message}" }
        }
    }

    /**
     * 获取数据库配置
     */
    fun getConfig(): DatabaseConfig {
        return databaseConfig ?: throw IllegalStateException("数据库未初始化")
    }

    /**
     * 获取特定类型的数据库配置
     */
    inline fun <reified T : DatabaseConfig> getTypedConfig(): T {
        val config = getConfig()
        return if (config is T) {
            config
        } else {
            throw IllegalStateException("数据库配置不是类型 ${T::class.simpleName}")
        }
    }
} 