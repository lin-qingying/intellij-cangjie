package cn.cangnova.controller.api

import cn.cangnova.model.ApiResponse
import cn.cangnova.model.BulkDeleteRequest
import cn.cangnova.repository.factory.TelemetryRepositoryFactory
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.UIManager.put

/**
 * 仪表盘API路由
 */
fun Route.dashboardApiRoutes() {
    // 需要JWT认证
    authenticate("jwt-auth") {
        get {
            try {
                val repository = TelemetryRepositoryFactory.getRepository()
                val totalEvents = repository.getTotalEventsCount()
                val totalMetadata = repository.getTotalMetadataCount()
                val recentMetadata = repository.getRecentMetadata(5)

                // 格式化元数据
                val formattedMetadata = recentMetadata.map { metadata ->
                    val timestamp = Instant.ofEpochMilli(metadata.timestamp)
                    val dateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
                    val formattedTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                    mapOf(
                        "id" to metadata._id,
                        "systemId" to metadata.systemId,
                        "pluginVersion" to metadata.pluginVersion,
                        "ideVersion" to metadata.ideVersion,
                        "ideBuild" to metadata.ideBuild,
                        "os" to metadata.os,
                        "osVersion" to metadata.osVersion,
                        "javaVersion" to metadata.javaVersion,
                        "timestamp" to metadata.timestamp,
                        "formattedTime" to formattedTime
                    )
                }

                // 返回仪表盘数据
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            put("totalEvents", totalEvents)
                            put("totalMetadata", totalMetadata)
                            put("recentMetadata", JsonArray(formattedMetadata.map { metadata ->
                                buildJsonObject {
                                    metadata.forEach { (key, value) ->
                                        when (value) {
                                            is String -> put(key, value)
                                            is Number -> put(key, value)
                                            is Boolean -> put(key, value)
                                            else -> put(key, value.toString())
                                        }
                                    }
                                }
                            }))
                        },
                        message = "Dashboard data retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to retrieve dashboard data: ${e.message}"
                    )
                )
            }
        }
    }
}

/**
 * 事件API路由
 */
fun Route.eventsApiRoutes() {
    // 需要JWT认证
    authenticate("jwt-auth") {
        // 获取事件列表
        get {
            try {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 50
                val category = call.parameters["category"]
                val name = call.parameters["name"]
                val startDate = call.parameters["startDate"]
                val endDate = call.parameters["endDate"]
                val groupBy = call.parameters["groupBy"]

                val repository = TelemetryRepositoryFactory.getRepository()
                val events = repository.getFilteredEvents(page, pageSize, category, name, startDate, endDate)
                val totalCount = repository.getFilteredEventsCount(category, name, startDate, endDate)
                val totalPages = (totalCount + pageSize - 1) / pageSize
                val categories = repository.getEventCategories()
                val eventNames = repository.getEventNames(category)

                // 获取所有事件关联的元数据
                val metadataIds = events.mapNotNull { it.metadataId }.distinct()
                val metadataMap = mutableMapOf<String, Map<String, String>>()

                // 创建格式化时间戳映射
                val formattedTimestamps = mutableMapOf<String, String>()

                // 为每个事件处理元数据和格式化时间戳
                events.forEach { event ->
                    // 格式化时间戳
                    formattedTimestamps[event.id] = formatIsoTimestamp(event.timestamp)

                    if (event.metadataId != null) {
                        try {
                            val metadata = repository.getMetadataById(event.metadataId!!)
                            if (metadata != null) {
                                metadataMap[event.metadataId!!] = mapOf(
                                    "pluginVersion" to metadata.pluginVersion,
                                    "ideVersion" to metadata.ideVersion,
                                    "os" to metadata.os,
                                    "osVersion" to metadata.osVersion,
                                    "javaVersion" to metadata.javaVersion,
                                    "systemId" to metadata.systemId,
                                    "ideBuild" to metadata.ideBuild
                                )
                            }
                        } catch (e: Exception) {
                            // 如果查询失败，使用默认值或忽略
                        }
                    }
                }

                // 返回事件数据
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            put("events", JsonArray(events.map { event ->
                                buildJsonObject {
                                    put("_id", event._id)
                                    put("id", event.id)
                                    put("category", event.category)
                                    put("name", event.name)
                                    put("value", event.value)
                                    put("timestamp", event.timestamp)
                                    put("properties", buildJsonObject {
                                        event.properties.forEach { (k, v) ->
                                            put(k, v)
                                        }
                                    })
                                    if (event.metadataId != null) {
                                        put("metadataId", event.metadataId)
                                    } else {
                                        put("metadataId", JsonNull)
                                    }
                                }
                            }))
                            put("currentPage", page)
                            put("totalPages", totalPages)
                            put("pageSize", pageSize)
                            put("totalCount", totalCount)
                            put("categories", JsonArray(categories.map { JsonPrimitive(it) }))
                            put("eventNames", JsonArray(eventNames.map { JsonPrimitive(it) }))
                            put("metadataMap", buildJsonObject {
                                metadataMap.forEach { (id, metadata) ->
                                    put(id, buildJsonObject {
                                        metadata.forEach { (k, v) ->
                                            put(k, v)
                                        }
                                    })
                                }
                            })
                            put("formattedTimestamps", buildJsonObject {
                                formattedTimestamps.forEach { (id, timestamp) ->
                                    put(id, timestamp)
                                }
                            })
                        },
                        message = "Events retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to retrieve events: ${e.message}"
                    )
                )
            }
        }

        // 获取单个事件详情
        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing event ID")
                val repository = TelemetryRepositoryFactory.getRepository()
                val event = repository.getEventById(id) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Event not found"
                    )
                )

                // 格式化时间戳
                val formattedTime = formatIsoTimestamp(event.timestamp)

                // 获取关联的元数据（如果有）
                val metadata = if (event.metadataId != null) {
                    repository.getMetadataById(event.metadataId!!)
                } else {
                    null
                }

                // 构建响应数据
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            put("event", buildJsonObject {
                                put("_id", event._id)
                                put("id", event.id)
                                put("category", event.category)
                                put("name", event.name)
                                put("value", event.value)
                                put("timestamp", event.timestamp)
                                put("properties", buildJsonObject {
                                    event.properties.forEach { (k, v) ->
                                        put(k, v)
                                    }
                                })
                                if (event.metadataId != null) {
                                    put("metadataId", event.metadataId)
                                } else {
                                    put("metadataId", JsonNull)
                                }
                            })
                            put("formattedTime", formattedTime)

                            if (metadata != null) {
                                put("metadata", buildJsonObject {
                                    put("_id", metadata._id)
                                    put("systemId", metadata.systemId)
                                    put("pluginVersion", metadata.pluginVersion)
                                    put("ideVersion", metadata.ideVersion)
                                    put("ideBuild", metadata.ideBuild)
                                    put("os", metadata.os)
                                    put("osVersion", metadata.osVersion)
                                    put("javaVersion", metadata.javaVersion)
                                    put("timestamp", metadata.timestamp)
                                })
                            }
                        },
                        message = "Event details retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to retrieve event details: ${e.message}"
                    )
                )
            }
        }

        // 删除单个事件
        delete("/{id}") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing event ID")
                val repository = TelemetryRepositoryFactory.getRepository()
                val deleted = repository.deleteEvent(id)

                if (deleted) {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = null,
                            message = "Event deleted successfully"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse(
                            success = false,
                            data = null,
                            message = "Event not found"
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to delete event: ${e.message}"
                    )
                )
            }
        }

        // 批量删除事件
        post("/bulk-delete") {
            try {
                val request = call.receive<BulkDeleteRequest>()
                val repository = TelemetryRepositoryFactory.getRepository()
                val deletedCount = repository.bulkDeleteEvents(request.ids)

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            put("deletedCount", deletedCount)
                        },
                        message = "Events deleted successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to delete events: ${e.message}"
                    )
                )
            }
        }
    }
}

/**
 * 元数据API路由
 */
fun Route.metadataApiRoutes() {
    // 需要JWT认证
    authenticate("jwt-auth") {
        get {
            try {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 50

                val repository = TelemetryRepositoryFactory.getRepository()
                val metadata = repository.getMetadata(page, pageSize)
                val totalCount = repository.getTotalMetadataCount()
                val totalPages = (totalCount + pageSize - 1) / pageSize

                // 创建格式化时间戳映射
                val formattedTimestamps = mutableMapOf<String, String>()

                // 为每个元数据处理格式化时间戳
                metadata.forEach { meta ->
                    formattedTimestamps[meta._id] = formatIsoTimestamp(meta.timestamp.toString())
                }

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            put("metadata", JsonArray(metadata.map { meta ->
                                buildJsonObject {
                                    put("_id", meta._id)
                                    put("systemId", meta.systemId)
                                    put("pluginVersion", meta.pluginVersion)
                                    put("ideVersion", meta.ideVersion)
                                    put("ideBuild", meta.ideBuild)
                                    put("os", meta.os)
                                    put("osVersion", meta.osVersion)
                                    put("javaVersion", meta.javaVersion)
                                    put("timestamp", meta.timestamp)
                                }
                            }))
                            put("currentPage", page)
                            put("totalPages", totalPages)
                            put("pageSize", pageSize)
                            put("totalCount", totalCount)
                            put("formattedTimestamps", buildJsonObject {
                                formattedTimestamps.forEach { (id, timestamp) ->
                                    put(id, timestamp)
                                }
                            })
                        },
                        message = "Metadata retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to retrieve metadata: ${e.message}"
                    )
                )
            }
        }
        
        // 获取单个元数据详情
        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing metadata ID")
                val repository = TelemetryRepositoryFactory.getRepository()
                val metadata = repository.getMetadataById(id) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Metadata not found"
                    )
                )

                // 格式化时间戳
                val formattedTime = formatIsoTimestamp(metadata.timestamp.toString())
                
                // 获取与该元数据关联的事件数量
                val eventsCount = repository.getEventsByMetadataIdCount(id)
                
                // 构建响应数据
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            put("metadata", buildJsonObject {
                                put("_id", metadata._id)
                                put("systemId", metadata.systemId)
                                put("pluginVersion", metadata.pluginVersion)
                                put("ideVersion", metadata.ideVersion)
                                put("ideBuild", metadata.ideBuild)
                                put("os", metadata.os)
                                put("osVersion", metadata.osVersion)
                                put("javaVersion", metadata.javaVersion)
                                put("timestamp", metadata.timestamp)
                            })
                            put("formattedTime", formattedTime)
                            put("eventsCount", eventsCount)
                        },
                        message = "Metadata details retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to retrieve metadata details: ${e.message}"
                    )
                )
            }
        }
    }
}

/**
 * 报表API路由
 */
fun Route.reportsApiRoutes() {
    // 需要JWT认证
    authenticate("jwt-auth") {
        get {
            try {
                val timeRange = call.parameters["timeRange"]?.toIntOrNull() ?: 30 // 默认30天
                val category = call.parameters["category"]
                val groupBy = call.parameters["groupBy"] ?: "day"

                val repository = TelemetryRepositoryFactory.getRepository()
                // 根据不同的groupBy使用不同的方法
                val reportData = mutableListOf<Map<String, Any>>()

                when (groupBy) {
                    "day" -> {
                        val data = repository.getEventCountByDay(timeRange, category)
                        data.forEach { (date, count) ->
                            reportData.add(mapOf("date" to date, "count" to count))
                        }
                    }

                    "week" -> {
                        val data = repository.getEventCountByWeek(timeRange, category)
                        data.forEach { (week, count) ->
                            reportData.add(mapOf("week" to week, "count" to count))
                        }
                    }

                    "category" -> {
                        val data = repository.getEventCountByCategory(category, timeRange)
                        data.forEach { (category, count) ->
                            reportData.add(mapOf("category" to category, "count" to count))
                        }
                    }

                    else -> {
                        val data = repository.getEventCountByDay(timeRange, category)
                        data.forEach { (date, count) ->
                            reportData.add(mapOf("date" to date, "count" to count))
                        }
                    }
                }

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            put("timeRange", timeRange)
                            put("category", category ?: JsonNull)
                            put("groupBy", groupBy)
                            put("data", JsonArray(reportData.map { item ->
                                buildJsonObject {
                                    item.forEach { (key, value) ->
                                        when (value) {
                                            is String -> put(key, value)
                                            is Number -> put(key, value.toInt())
                                            is Boolean -> put(key, value)
                                            else -> put(key, value.toString())
                                        }
                                    }
                                }
                            }))
                        },
                        message = "Report data retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to retrieve report data: ${e.message}"
                    )
                )
            }
        }
    }
}

/**
 * 格式化ISO-8601时间戳
 */
private fun formatIsoTimestamp(timestamp: String): String {
    return try {
        if (timestamp.contains("T") && timestamp.contains("Z")) {
            val instant = Instant.parse(timestamp)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            // 尝试将时间戳解析为Long（毫秒）
            val longTimestamp = timestamp.toLongOrNull()
            if (longTimestamp != null) {
                val instant = Instant.ofEpochMilli(longTimestamp)
                LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            } else {
                timestamp
            }
        }
    } catch (e: Exception) {
        timestamp // 如果解析失败，返回原始时间戳
    }
} 