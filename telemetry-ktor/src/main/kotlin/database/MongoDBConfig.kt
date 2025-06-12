package cn.cangnova.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.connection.ConnectionPoolSettings
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoCollection
import com.mongodb.kotlin.client.MongoDatabase
import io.ktor.server.config.*
import mu.KotlinLogging
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import java.util.concurrent.TimeUnit

/**
 * MongoDB配置类，负责初始化和管理MongoDB连接
 */
class MongoDBConfig(private val config: ApplicationConfig) : DatabaseConfig {
    private val logger = KotlinLogging.logger {}
    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase

    // 集合名称常量
    companion object {
        const val EVENTS_COLLECTION = "telemetry_events"
        const val METADATA_COLLECTION = "telemetry_metadata"
        const val ADMIN_USERS_COLLECTION = "admin_users"
        const val SETTINGS_COLLECTION = "system_settings"
    }

    /**
     * 初始化MongoDB连接
     */
    override fun init() {
        try {
            // 从配置文件读取设置
            logger.info { "开始读取MongoDB配置" }
            val connectionString = config.property("database.mongodb.connection_string").getString()
            logger.info { "MongoDB连接字符串: $connectionString" }
            
            val databaseName = config.property("database.mongodb.database_name").getString()
            logger.info { "MongoDB数据库名称: $databaseName" }
            
            // 读取认证信息（如果配置了）
            val useAuth = config.propertyOrNull("database.mongodb.auth.enabled")?.getString()?.toBoolean() ?: false
            logger.info { "MongoDB认证启用状态: $useAuth" }
            
            val username = if (useAuth) config.property("database.mongodb.auth.username").getString() else null
            val password = if (useAuth) "******" else null
            // 使用配置的 admin 作为认证数据库
            val authDatabase = "admin"
            
            if (useAuth) {
                logger.info { "MongoDB认证用户名: $username, 认证数据库: $authDatabase" }
            }
            
            // 读取连接池配置
            logger.info { "读取MongoDB连接池配置" }
            val maxPoolSize = config.property("database.mongodb.pool.max_size").getString().toInt()
            val minPoolSize = config.property("database.mongodb.pool.min_size").getString().toInt()
            val maxWaitTimeMs = config.property("database.mongodb.pool.max_wait_time_ms").getString().toLong()
            val maxConnectionLifetimeMs = config.property("database.mongodb.pool.max_connection_lifetime_ms").getString().toLong()
            
            logger.info { "MongoDB连接池配置: maxSize=$maxPoolSize, minSize=$minPoolSize, maxWaitTime=${maxWaitTimeMs}ms, maxLifetime=${maxConnectionLifetimeMs}ms" }
            
            // 配置连接池
            val poolSettings = ConnectionPoolSettings.builder()
                .maxSize(maxPoolSize)
                .minSize(minPoolSize)
                .maxWaitTime(maxWaitTimeMs, TimeUnit.MILLISECONDS)
                .maxConnectionLifeTime(maxConnectionLifetimeMs, TimeUnit.MILLISECONDS)
                .build()
            
            // 配置POJO编解码器，使MongoDB能够直接映射Kotlin对象
            logger.info { "配置MongoDB编解码器" }
            val pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build()
            val codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(pojoCodecProvider)
            )
            
            // 创建客户端设置
            logger.info { "创建MongoDB客户端设置" }
            val clientSettingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .applyToConnectionPoolSettings { builder -> builder.applySettings(poolSettings) }
            
            // 如果配置了认证信息，添加认证
            if (useAuth && username != null && password != null) {
                logger.info { "配置MongoDB认证" }
                val credential = MongoCredential.createCredential(
                    username,
                    authDatabase,
                    config.property("database.mongodb.auth.password").getString().toCharArray()
                )
                clientSettingsBuilder.credential(credential)
                logger.info { "MongoDB认证已配置: 用户名=$username, 认证数据库=$authDatabase" }
            }
            
            // 创建客户端和获取数据库
            logger.info { "创建MongoDB客户端" }
            client = MongoClient.create(clientSettingsBuilder.build())
            database = client.getDatabase(databaseName)
            
            // 确保集合存在
            logger.info { "检查并创建必要的集合" }
            ensureCollections()
            
            logger.info { "MongoDB连接已初始化: $connectionString, 数据库: $databaseName" }
        } catch (e: Exception) {
            logger.error(e) { "MongoDB初始化失败: ${e.message}" }
            throw e
        }
    }
    
    /**
     * 确保所需的集合存在
     */
    private fun ensureCollections() {
        val collectionNames = database.listCollectionNames().toList()
        
        if (EVENTS_COLLECTION !in collectionNames) {
            database.createCollection(EVENTS_COLLECTION)
            logger.info { "创建集合: $EVENTS_COLLECTION" }
        }
        
        if (METADATA_COLLECTION !in collectionNames) {
            database.createCollection(METADATA_COLLECTION)
            logger.info { "创建集合: $METADATA_COLLECTION" }
        }
        
        if (ADMIN_USERS_COLLECTION !in collectionNames) {
            database.createCollection(ADMIN_USERS_COLLECTION)
            logger.info { "创建集合: $ADMIN_USERS_COLLECTION" }
        }
        
        if (SETTINGS_COLLECTION !in collectionNames) {
            database.createCollection(SETTINGS_COLLECTION)
            logger.info { "创建集合: $SETTINGS_COLLECTION" }
        }
    }
    
    /**
     * 获取事件集合
     */
    fun getEventsCollection(): MongoCollection<Document> {
        if (!::database.isInitialized) {
            throw IllegalStateException("MongoDB database not initialized. Call init() first.")
        }
        return database.getCollection(EVENTS_COLLECTION)
    }
    
    /**
     * 获取元数据集合
     */
    fun getMetadataCollection(): MongoCollection<Document> {
        if (!::database.isInitialized) {
            throw IllegalStateException("MongoDB database not initialized. Call init() first.")
        }
        return database.getCollection(METADATA_COLLECTION)
    }
    
    /**
     * 获取管理员用户集合
     */
    fun getAdminUsersCollection(): MongoCollection<Document> {
        if (!::database.isInitialized) {
            throw IllegalStateException("MongoDB database not initialized. Call init() first.")
        }
        return database.getCollection(ADMIN_USERS_COLLECTION)
    }
    
    /**
     * 获取系统设置集合
     */
    fun getSettingsCollection(): MongoCollection<Document> {
        if (!::database.isInitialized) {
            throw IllegalStateException("MongoDB database not initialized. Call init() first.")
        }
        return database.getCollection(SETTINGS_COLLECTION)
    }
    
    /**
     * 关闭MongoDB连接
     */
    override fun close() {
        if (::client.isInitialized) {
            client.close()
            logger.info { "MongoDB连接已关闭" }
        }
    }
    
    /**
     * 获取数据库类型
     */
    override fun getType(): DatabaseType {
        return DatabaseType.MONGODB
    }
} 