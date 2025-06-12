package cn.cangnova.database

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * PostgreSQL配置类，负责初始化和管理PostgreSQL连接
 */
class PostgreSQLConfig(private val config: Config) : DatabaseConfig {
    private val logger = KotlinLogging.logger {}
    private var dataSource: HikariDataSource? = null

    /**
     * 初始化PostgreSQL连接
     */
    override fun init() {
        try {
            // 从配置文件读取设置
            val jdbcUrl = config.getString("database.postgresql.url")
            val driverClassName = config.getString("database.postgresql.driver")
            val username = config.getString("database.postgresql.user")
            val password = config.getString("database.postgresql.password")

            // 读取连接池配置
            val maxPoolSize = config.getInt("database.postgresql.pool.max_size")
            val minIdle = config.getInt("database.postgresql.pool.min_size")
            val idleTimeout = config.getLong("database.postgresql.pool.idle_timeout_ms")
            val maxLifetime = config.getLong("database.postgresql.pool.max_lifetime_ms")

            // 配置HikariCP连接池
            val hikariConfig = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.driverClassName = driverClassName
                this.username = username
                this.password = password
                this.maximumPoolSize = maxPoolSize
                this.minimumIdle = minIdle
                this.idleTimeout = idleTimeout
                this.maxLifetime = maxLifetime
                this.isAutoCommit = false
                this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                this.validate()
            }

            // 创建数据源
            dataSource = HikariDataSource(hikariConfig)

            // 初始化Exposed
            Database.connect(dataSource!!)

            // 创建表
            transaction {
                // 使用与MySQL相同的表定义
                SchemaUtils.create(MySQLConfig.TelemetryEvents, MySQLConfig.TelemetryMetadata)
            }

            logger.info { "PostgreSQL连接已初始化: $jdbcUrl" }
        } catch (e: Exception) {
            logger.error(e) { "PostgreSQL初始化失败: ${e.message}" }
            throw e
        }
    }

    /**
     * 关闭PostgreSQL连接
     */
    override fun close() {
        dataSource?.close()
        logger.info { "PostgreSQL连接已关闭" }
    }

    /**
     * 获取数据库类型
     */
    override fun getType(): DatabaseType {
        return DatabaseType.POSTGRESQL
    }

    /**
     * 获取数据源
     */
    fun getDataSource(): HikariDataSource {
        return dataSource ?: throw IllegalStateException("PostgreSQL data source not initialized")
    }
} 