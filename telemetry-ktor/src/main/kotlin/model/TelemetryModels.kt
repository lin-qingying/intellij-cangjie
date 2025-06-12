package cn.cangnova.model

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant

/**
 * 遥测事件，表示一个可以被收集和发送的遥测数据
 */
@Serializable
data class TelemetryEvent(
    /**
     * MongoDB文档ID
     */
    @BsonId
    val _id: String = ObjectId().toString(),
    
    /**
     * 事件的唯一标识符
     */
    val id: String,
    
    /**
     * 事件的类别
     */
    val category: String,
    
    /**
     * 事件的名称
     */
    val name: String,
    
    /**
     * 事件的值
     */
    val value: String,
    
    /**
     * 事件的创建时间
     */
    val timestamp: String,
    
    /**
     * 事件的附加属性
     */
    val properties: Map<String, String> = emptyMap(),
    
    /**
     * 关联的元数据ID
     */
    var metadataId: String? = null
)

/**
 * 元数据，包含关于发送遥测数据的环境信息
 */
@Serializable
data class TelemetryMetadata(
    /**
     * MongoDB文档ID
     */
    @BsonId
    val _id: String = ObjectId().toString(),
    
    /**
     * 系统唯一标识符，用于区分不同用户
     */
    val systemId: String,
    
    /**
     * 插件版本
     */
    val pluginVersion: String,
    
    /**
     * IDE版本
     */
    val ideVersion: String,
    
    /**
     * IDE构建号
     */
    val ideBuild: String,
    
    /**
     * 操作系统
     */
    val os: String,
    
    /**
     * 操作系统版本
     */
    val osVersion: String,
    
    /**
     * Java版本
     */
    val javaVersion: String,
    
    /**
     * 时间戳
     */
    val timestamp: Long,
    
    /**
     * 服务器接收时间
     */
    val receivedTimestamp: String = Instant.now().toString()
)

/**
 * 遥测数据载荷，包含元数据和事件
 */
@Serializable
data class TelemetryPayload(
    /**
     * 元数据
     */
    val metadata: TelemetryMetadata,
    
    /**
     * 事件列表
     */
    val events: List<TelemetryEvent>
)

/**
 * 遥测响应
 */
@Serializable
data class TelemetryResponse(
    /**
     * 是否成功
     */
    val success: Boolean,
    
    /**
     * 消息
     */
    val message: String,
    
    /**
     * 处理的事件数量
     */
    val count: Int
) 