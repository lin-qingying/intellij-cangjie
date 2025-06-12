package cn.cangnova.model

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

/**
 * 系统设置数据模型
 */
@Serializable
data class SystemSettings(
    /**
     * MongoDB文档ID
     */
    @BsonId
    val _id: String = ObjectId().toString(),
    
    /**
     * 数据保留天数
     */
    val dataRetentionDays: Int = 365,
    
    /**
     * 清理计划 (daily, weekly, monthly)
     */
    val cleanupSchedule: String = "weekly",
    
    /**
     * 是否启用自动清理
     */
    val enableAutoCleanup: Boolean = false,
    
    /**
     * 备份计划 (daily, weekly, monthly)
     */
    val backupSchedule: String = "weekly",
    
    /**
     * 备份保留数量
     */
    val backupRetention: Int = 5,
    
    /**
     * 是否启用自动备份
     */
    val enableAutoBackup: Boolean = false,
    
    /**
     * 事件批处理大小
     */
    val eventBatchSize: Int = 100,
    
    /**
     * 连接池大小
     */
    val connectionPoolSize: Int = 10,
    
    /**
     * 日志级别 (ERROR, WARN, INFO, DEBUG, TRACE)
     */
    val logLevel: String = "INFO",
    
    /**
     * 日志保留天数
     */
    val logRetention: Int = 30,
    
    /**
     * 是否启用缓存
     */
    val enableCache: Boolean = true,
    
    /**
     * 缓存大小 (MB)
     */
    val cacheSize: Int = 256,
    
    /**
     * 缓存过期时间 (分钟)
     */
    val cacheExpiry: Int = 30,
    
    /**
     * 最后更新时间
     */
    val lastUpdated: Long = System.currentTimeMillis()
) 