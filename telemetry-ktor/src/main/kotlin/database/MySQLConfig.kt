package cn.cangnova.database

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

/**
 * MySQL配置类，负责初始化和管理MySQL连接
 */
class MySQLConfig(private val config: Config) : DatabaseConfig {
    private val logger = KotlinLogging.logger {}
    private var dataSource: HikariDataSource? = null
    
    // 数据表定义
    object TelemetryEvents : Table() {
        val id = varchar("id", 255)
        val eventId = varchar("event_id", 255)
        val category = varchar("category", 100)
        val name = varchar("name", 255)
        val value = text("value")
        val timestamp = timestamp("timestamp")
        val properties = text("properties")
        val metadataId = varchar("metadata_id", 255)
        
        override val primaryKey = PrimaryKey(id)
    }
    
    object TelemetryMetadata : Table() {
        val id = varchar("id", 255)
        val pluginVersion = varchar("plugin_version", 50)
        val ideVersion = varchar("ide_version", 50)
        val ideBuild = varchar("ide_build", 50)
        val os = varchar("os", 50)
        val osVersion = varchar("os_version", 50)
        val javaVersion = varchar("java_version", 50)
        val receivedTimestamp = timestamp("received_timestamp")
        val clientTimestamp = long("client_timestamp")
        val systemId =    varchar("id", 255)
        override val primaryKey = PrimaryKey(id)
    }
    
    /**
     * 初始化MySQL连接
     */
    override fun init() {
        try {
            // 从配置文件读取设置
            val jdbcUrl = config.getString("database.mysql.url")
            val driverClassName = config.getString("database.mysql.driver")
            val username = config.getString("database.mysql.user")
            val password = config.getString("database.mysql.password")
            
            // 读取连接池配置
            val maxPoolSize = config.getInt("database.mysql.pool.max_size")
            val minIdle = config.getInt("database.mysql.pool.min_size")
            val idleTimeout = config.getLong("database.mysql.pool.idle_timeout_ms")
            val maxLifetime = config.getLong("database.mysql.pool.max_lifetime_ms")
            
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
                SchemaUtils.create(TelemetryEvents, TelemetryMetadata)
            }
            
            logger.info { "MySQL连接已初始化: $jdbcUrl" }
        } catch (e: Exception) {
            logger.error(e) { "MySQL初始化失败: ${e.message}" }
            throw e
        }
    }
    
    /**
     * 关闭MySQL连接
     */
    override fun close() {
        dataSource?.close()
        logger.info { "MySQL连接已关闭" }
    }
    
    /**
     * 获取数据库类型
     */
    override fun getType(): DatabaseType {
        return DatabaseType.MYSQL
    }
    
    /**
     * 获取数据源
     */
    fun getDataSource(): HikariDataSource {
        return dataSource ?: throw IllegalStateException("MySQL data source not initialized")
    }
} 