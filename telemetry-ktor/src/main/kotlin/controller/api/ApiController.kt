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
import java.time.LocalDate

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
                val metadataId = call.parameters["metadataId"]

                val repository = TelemetryRepositoryFactory.getRepository()
                
                // 使用统一的方法获取事件列表，传入所有过滤参数
                val events = repository.getFilteredEvents(
                    page, 
                    pageSize, 
                    category, 
                    name, 
                    startDate, 
                    endDate, 
                    metadataId
                )
                
                // 使用统一的方法获取事件总数，传入所有过滤参数
                val totalCount = repository.getFilteredEventsCount(
                    category, 
                    name, 
                    startDate, 
                    endDate, 
                    metadataId
                )
                
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
                val eventsCount = repository.getFilteredEventsCount(
                    category = null,
                    name = null,
                    startDate = null,
                    endDate = null,
                    metadataId = id
                )
                
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
                
                // 获取事件总数
                val totalEvents = repository.getFilteredEventsCount(
                    category = category,
                    startDate = LocalDate.now().minusDays(timeRange.toLong()).toString(),
                    endDate = LocalDate.now().toString()
                )
                
                // 获取唯一用户数
                val uniqueUsers = repository.getUniqueUsersCount(timeRange)
                
                // 计算日均事件数
                val dailyAverage = if (timeRange > 0) {
                    totalEvents.toDouble() / timeRange
                } else {
                    0.0
                }
                
                // 获取事件类别统计
                val eventsByCategory = repository.getEventCountByCategory(category, timeRange)
                
                // 获取最活跃的类别
                val topCategory = if (eventsByCategory.isNotEmpty()) {
                    eventsByCategory.maxByOrNull { it.value }?.key ?: ""
                } else {
                    ""
                }
                
                // 获取所有可用类别
                val categories = repository.getEventCategories()
                
                // 获取时间序列数据
                val eventsByTime = when (groupBy) {
                    "day" -> repository.getEventCountByDay(timeRange, category)
                    "week" -> repository.getEventCountByWeek(timeRange, category)
                    "month" -> repository.getEventCountByMonth(timeRange, category)
                    else -> repository.getEventCountByDay(timeRange, category)
                }
                
                // 计算趋势变化百分比 (简单实现：比较当前时间段和前一个时间段的事件数)
                val previousTimeRange = timeRange * 2
                val previousEvents = repository.getFilteredEventsCount(
                    category = category,
                    startDate = LocalDate.now().minusDays(previousTimeRange.toLong()).toString(),
                    endDate = LocalDate.now().minusDays(timeRange.toLong()).toString()
                )
                
                val trendPercentage = if (previousEvents > 0) {
                    ((totalEvents - previousEvents).toDouble() / previousEvents * 100).toInt()
                } else if (totalEvents > 0) {
                    100 // 如果之前没有事件，但现在有，则增长100%
                } else {
                    0 // 如果都没有事件，则增长0%
                }

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            put("timeRange", timeRange)
                            put("category", category ?: JsonNull)
                            put("groupBy", groupBy)
                            put("totalEvents", totalEvents)
                            put("uniqueUsers", uniqueUsers)
                            put("dailyAverage", dailyAverage)
                            put("topCategory", topCategory)
                            put("trendPercentage", trendPercentage)
                            put("categories", JsonArray(categories.map { JsonPrimitive(it) }))
                            
                            // 事件类别分布
                            put("eventsByCategory", buildJsonObject {
                                eventsByCategory.forEach { (category, count) ->
                                    put(category, count)
                                }
                            })
                            
                            // 事件时间分布
                            put("eventsByTime", buildJsonObject {
                                eventsByTime.forEach { (time, count) ->
                                    put(time, count)
                                }
                            })
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
 * 数据分析API路由
 */
fun Route.analyticsApiRoutes() {
    // 需要JWT认证
    authenticate("jwt-auth") {
        get {
            try {
                val timeRange = call.parameters["timeRange"]?.toIntOrNull() ?: 30 // 默认30天
                val category = call.parameters["category"]
                val analysisType = call.parameters["analysisType"] ?: "trend" // 默认趋势分析

                val repository = TelemetryRepositoryFactory.getRepository()
                
                // 获取基础数据
                val totalEvents = repository.getFilteredEventsCount(
                    category = category,
                    startDate = LocalDate.now().minusDays(timeRange.toLong()).toString(),
                    endDate = LocalDate.now().toString()
                )
                
                val uniqueUsers = repository.getUniqueUsersCount(timeRange)
                val eventsByCategory = repository.getEventCountByCategory(category, timeRange)
                val categories = repository.getEventCategories()
                
                // 获取时间序列数据
                val dailyData = repository.getEventCountByDay(timeRange, category)
                val weeklyData = repository.getEventCountByWeek(timeRange, category)
                val monthlyData = repository.getEventCountByMonth(timeRange, category)
                
                // 计算趋势和增长率
                val previousTimeRange = timeRange * 2
                val previousEvents = repository.getFilteredEventsCount(
                    category = category,
                    startDate = LocalDate.now().minusDays(previousTimeRange.toLong()).toString(),
                    endDate = LocalDate.now().minusDays(timeRange.toLong()).toString()
                )
                
                val growthRate = if (previousEvents > 0) {
                    ((totalEvents - previousEvents).toDouble() / previousEvents * 100)
                } else if (totalEvents > 0) {
                    100.0
                } else {
                    0.0
                }
                
                // 计算一些高级分析指标
                val dailyAverage = if (timeRange > 0) totalEvents.toDouble() / timeRange else 0.0
                val topCategory = eventsByCategory.maxByOrNull { it.value }?.key ?: ""
                
                // 计算活跃度分数 (简单实现：基于事件总数和用户数的加权平均)
                val activityScore = if (uniqueUsers > 0) {
                    (totalEvents.toDouble() * 0.7 + uniqueUsers.toDouble() * 0.3) / timeRange * 10
                } else {
                    0.0
                }
                
                // 计算用户参与度 (简单实现：每个用户平均事件数)
                val engagementRate = if (uniqueUsers > 0) {
                    totalEvents.toDouble() / uniqueUsers
                } else {
                    0.0
                }
                
                // 构建响应数据
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = buildJsonObject {
                            // 基础信息
                            put("timeRange", timeRange)
                            put("category", category ?: JsonNull)
                            put("analysisType", analysisType)
                            
                            // 关键指标
                            put("totalEvents", totalEvents)
                            put("uniqueUsers", uniqueUsers)
                            put("dailyAverage", dailyAverage)
                            put("growthRate", growthRate)
                            put("activityScore", activityScore)
                            put("engagementRate", engagementRate)
                            put("topCategory", topCategory)
                            
                            // 类别数据
                            put("categories", JsonArray(categories.map { JsonPrimitive(it) }))
                            put("eventsByCategory", buildJsonObject {
                                eventsByCategory.forEach { (category, count) ->
                                    put(category, count)
                                }
                            })
                            
                            // 时间序列数据
                            put("dailyData", buildJsonObject {
                                dailyData.forEach { (date, count) ->
                                    put(date, count)
                                }
                            })
                            
                            put("weeklyData", buildJsonObject {
                                weeklyData.forEach { (week, count) ->
                                    put(week, count)
                                }
                            })
                            
                            put("monthlyData", buildJsonObject {
                                monthlyData.forEach { (month, count) ->
                                    put(month, count)
                                }
                            })
                            
                            // 根据分析类型提供不同的附加数据
                            when (analysisType) {
                                "trend" -> {
                                    // 趋势分析数据已包含在基础数据中
                                }
                                "comparison" -> {
                                    // 比较分析：计算前后两个时间段的对比数据
                                    val currentPeriodEvents = totalEvents
                                    val previousPeriodEvents = previousEvents
                                    
                                    put("comparisonData", buildJsonObject {
                                        put("currentPeriod", buildJsonObject {
                                            put("events", currentPeriodEvents)
                                            put("users", uniqueUsers)
                                        })
                                        put("previousPeriod", buildJsonObject {
                                            put("events", previousPeriodEvents)
                                            put("users", repository.getUniqueUsersCount(timeRange * 2) - uniqueUsers)
                                        })
                                        put("change", buildJsonObject {
                                            put("events", currentPeriodEvents - previousPeriodEvents)
                                            put("eventsPercentage", growthRate)
                                        })
                                    })
                                }
                                "forecast" -> {
                                    // 简单预测：基于平均增长率预测未来趋势
                                    val forecastData = mutableMapOf<String, Int>()
                                    val dailyList = dailyData.entries.sortedBy { it.key }
                                    
                                    if (dailyList.isNotEmpty()) {
                                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                        val lastDate = LocalDate.parse(dailyList.last().key)
                                        val avgGrowth = growthRate / 100 / timeRange // 平均每天增长率
                                        
                                        // 预测未来7天
                                        var lastValue = dailyList.last().value.toDouble()
                                        for (i in 1..7) {
                                            val nextDate = lastDate.plusDays(i.toLong())
                                            val predictedValue = lastValue * (1 + avgGrowth)
                                            forecastData[nextDate.format(dateFormatter)] = predictedValue.toInt()
                                            lastValue = predictedValue
                                        }
                                    }
                                    
                                    put("forecastData", buildJsonObject {
                                        forecastData.forEach { (date, count) ->
                                            put(date, count)
                                        }
                                    })
                                }
                                "correlation" -> {
                                    // 相关性分析：计算不同类别之间的相关性
                                    val correlationData = mutableMapOf<String, Double>()
                                    
                                    // 简单实现：随机生成相关性数据
                                    // 实际应用中应该使用真实数据计算
                                    categories.forEach { cat ->
                                        if (cat != category && cat != topCategory) {
                                            correlationData[cat] = (Math.random() * 2 - 1) // -1到1之间的随机值
                                        }
                                    }
                                    
                                    put("correlationData", buildJsonObject {
                                        correlationData.forEach { (category, correlation) ->
                                            put(category, correlation)
                                        }
                                    })
                                }
                            }
                        },
                        message = "Analytics data retrieved successfully"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        data = null,
                        message = "Failed to retrieve analytics data: ${e.message}"
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