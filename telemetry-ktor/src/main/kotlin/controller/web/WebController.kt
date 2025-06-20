package cn.cangnova.controller.web

import cn.cangnova.UserSession
import cn.cangnova.model.AdminUser
import cn.cangnova.model.CreateUserRequest
import cn.cangnova.model.UpdateUserRequest
import cn.cangnova.repository.factory.AdminUserRepositoryFactory
import cn.cangnova.repository.factory.SystemSettingsRepositoryFactory
import cn.cangnova.repository.factory.TelemetryRepositoryFactory
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: String?): String {
    if (timestamp == null) return ""
    
    return try {
        // 尝试不同的时间戳格式
        if (timestamp.contains("-") && !timestamp.contains("T")) {
            // 如果已经是格式化的时间（不含T），直接使用
            timestamp
        } else if (timestamp.all { it.isDigit() }) {
            // 如果是毫秒时间戳
            val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.toLong()), ZoneId.systemDefault())
            dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else if (timestamp.contains("T") && (timestamp.contains("Z") || timestamp.contains("+"))) {
            // ISO-8601格式 (例如: 2025-06-20T08:27:35.577Z)
            val instant = Instant.parse(timestamp)
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            // 尝试解析其他ISO格式
            val instant = Instant.parse(timestamp)
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }
    } catch (e: Exception) {
        // 如果解析失败，返回原始时间戳
        timestamp
    }
}

/**
 * 格式化Long类型的时间戳（适用于元数据）
 */
private fun formatTimestamp(timestamp: Long): String {
    return try {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    } catch (e: Exception) {
        // 如果解析失败，返回原始时间戳
        timestamp.toString()
    }
}

/**
 * 注册Web路由
 */
fun Route.webRoutes() {
    route("/web") {
        // 根路由（登录、登出等）
        rootWebRoutes()

        // 需要认证的路由
        authenticate("user-session") {
            // 仪表盘
            dashboardRoutes()

            // 事件管理
            eventsRoutes()

            // 元数据管理
            metadataRoutes()

            // 报表
            reportsRoutes()

            // 系统设置
            settingsRoutes()

            // 用户管理
            usersRoutes()
        }
    }
}

/**
 * 获取当前已认证用户信息
 */
private suspend fun ApplicationCall.getCurrentUser(): AdminUser? {
    val username = principal<UserIdPrincipal>()?.name ?: return null
    return AdminUserRepositoryFactory.getRepository().findByUsername(username)
}

/**
 * 创建基本模型数据
 */
private suspend fun ApplicationCall.createBaseModel(
    activeTab: String,
    additionalData: Map<String, Any?> = emptyMap()
): Map<String, Any?> {
    val currentUser = getCurrentUser()
    val baseModel = mutableMapOf<String, Any>(
        "activeTab" to activeTab
    )

    if (currentUser != null) {
        baseModel["user"] = currentUser
    }

    return baseModel + additionalData
}

/**
 * 标准错误处理响应
 */
private suspend fun ApplicationCall.respondWithError(
    templatePath: String,
    errorMessage: String,
    activeTab: String = ""
) {
    val errorModel = mapOf(
        "error" to errorMessage,
        "activeTab" to activeTab
    )
    val currentUser = getCurrentUser()
    if (currentUser != null) {
        respond(FreeMarkerContent(templatePath, errorModel + mapOf("user" to currentUser)))
    } else {
        respond(FreeMarkerContent(templatePath, errorModel))
    }
}

/**
 * 根Web路由
 */
private fun Route.rootWebRoutes() {
    // 根路径 - 重定向到仪表盘或登录页面
    get {
        val session = call.sessions.get<UserSession>()
        if (session != null) {
            call.respondRedirect("/web/dashboard")
        } else {
            call.respondRedirect("/web/login")
        }
    }

    // 登录页面
    get("/login") {
        val error = call.parameters["error"]
        call.respond(FreeMarkerContent("web/login.ftl", mapOf("error" to error)))
    }

    // 登录处理
    post("/login") {
        try {
            val formParameters = call.receiveParameters()
            val username = formParameters["username"] ?: ""
            val password = formParameters["password"] ?: ""

            val repository = AdminUserRepositoryFactory.getRepository()
            val user = repository.validateCredentials(username, password)

            if (user != null) {
                // 更新最后登录时间
                repository.updateLastLoginTime(username)
                // 设置会话
                call.sessions.set(UserSession(username))
                call.respondRedirect("/web/dashboard")
            } else {
                call.respondRedirect("/web/login?error=invalid_credentials")
            }
        } catch (e: Exception) {
            logger.error(e) { "登录失败: ${e.message}" }
            call.respondRedirect("/web/login?error=server_error")
        }
    }

    // 登出处理
    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/web/login")
    }
}

/**
 * 仪表盘路由
 */
private fun Route.dashboardRoutes() {
    get("/dashboard") {
        try {
            val telemetryRepository = TelemetryRepositoryFactory.getRepository()

            // 获取统计数据
            val totalEvents = telemetryRepository.getTotalEventsCount()
            val totalMetadata = telemetryRepository.getTotalMetadataCount()
            val recentMetadata = telemetryRepository.getRecentMetadata(5)

            // 格式化元数据时间戳
            val formattedMetadata = recentMetadata.map { metadata ->
                val timestamp = metadata.timestamp
                val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
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

            val model = call.createBaseModel(
                "dashboard", mapOf(
                    "totalEvents" to totalEvents,
                    "totalMetadata" to totalMetadata,
                    "recentMetadata" to formattedMetadata
                )
            )

            call.respond(FreeMarkerContent("web/dashboard.ftl", model))
        } catch (e: Exception) {
            logger.error(e) { "获取仪表盘数据失败: ${e.message}" }
            call.respondWithError("web/dashboard.ftl", "获取仪表盘数据失败: ${e.message}", "dashboard")
        }
    }
}

/**
 * 事件管理路由
 */
private fun Route.eventsRoutes() {
    route("/events") {
        get {
            try {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20
                val category = call.parameters["category"]
                val name = call.parameters["name"]
                val startDate = call.parameters["startDate"]
                val endDate = call.parameters["endDate"]
                val metadataId = call.parameters["metadataId"]

                val repository = TelemetryRepositoryFactory.getRepository()
                val events = if (metadataId != null) {
                    // 如果有metadataId，优先按照metadataId过滤
                    repository.getEventsByMetadataId(metadataId, page, pageSize)
                } else {
                    repository.getFilteredEvents(page, pageSize, category, name, startDate, endDate)
                }
                
                val totalCount = if (metadataId != null) {
                    repository.getEventsByMetadataIdCount(metadataId)
                } else {
                    repository.getFilteredEventsCount(category, name, startDate, endDate)
                }
                val totalPages = (totalCount + pageSize - 1) / pageSize
                val categories = repository.getEventCategories()
                val eventNames = repository.getEventNames(category)

                // 格式化时间戳
                val formattedTimestamps = events.associate { event ->
                    val timestamp = event.timestamp
                    val formattedTime = formatTimestamp(timestamp)
                    event.id to formattedTime
                }

                val model = call.createBaseModel(
                    "events", mapOf(
                        "events" to events,
                        "currentPage" to page,
                        "totalPages" to totalPages,
                        "pageSize" to pageSize,
                        "totalCount" to totalCount,
                        "categories" to categories,
                        "eventNames" to eventNames,
                        "selectedCategory" to (category ?: ""),
                        "selectedName" to (name ?: ""),
                        "startDate" to (startDate ?: ""),
                        "endDate" to (endDate ?: ""),
                        "metadataId" to (metadataId ?: ""),
                        "formattedTimestamps" to formattedTimestamps
                    )
                )

                call.respond(FreeMarkerContent("web/events.ftl", model))
            } catch (e: Exception) {
                logger.error(e) { "获取事件数据失败: ${e.message}" }
                call.respondWithError("web/events.ftl", "获取事件数据失败: ${e.message}", "events")
            }
        }

        // 添加事件详情页面
        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("事件ID不能为空")
                val repository = TelemetryRepositoryFactory.getRepository()

                // 这里获取事件详情
                val event = repository.getEventById(id) ?: throw IllegalArgumentException("未找到该事件")

                // 格式化时间
                val timestamp = event.timestamp
                val formattedTime = formatTimestamp(timestamp)

                // 获取关联的元数据
                val metadata = event.metadataId?.let { metadataId ->
                    try {
                        val md = repository.getMetadataById(metadataId)
                        md?.let {
                            mapOf(
                                "id" to it._id,
                                "systemId" to it.systemId,
                                "pluginVersion" to it.pluginVersion,
                                "ideVersion" to it.ideVersion,
                                "ideBuild" to it.ideBuild,
                                "os" to it.os,
                                "osVersion" to it.osVersion,
                                "javaVersion" to it.javaVersion,
                                "timestamp" to it.timestamp
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                // 将事件属性转换为JSON
                val propertiesJson = try {
                    Json.encodeToString(event.properties)
                } catch (e: Exception) {
                    "{}"
                }

                // 根据事件类别设置颜色
                val categoryColor = when (event.category.lowercase()) {
                    "action" -> "success"
                    "error" -> "danger"
                    "performance" -> "warning"
                    "system" -> "info"
                    else -> "primary"
                }

                // 映射事件数据，保持字段名一致性
                val eventMap = mapOf(
                    "id" to event.id,
                    "_id" to event._id,  // 保留原始 _id 以兼容
                    "category" to event.category,
                    "name" to event.name,
                    "value" to event.value,
                    "timestamp" to event.timestamp,
                    "properties" to event.properties,
                    "metadataId" to event.metadataId
                )

                val model = call.createBaseModel(
                    "events", mapOf(
                        "event" to eventMap,
                        "metadata" to metadata,
                        "formattedTime" to formattedTime,
                        "propertiesJson" to propertiesJson,
                        "categoryColor" to categoryColor
                    )
                )

                call.respond(FreeMarkerContent("web/event_details.ftl", model))
            } catch (e: Exception) {
                logger.error(e) { "获取事件详情失败: ${e.message}" }
                call.respondWithError("web/events.ftl", "获取事件详情失败: ${e.message}", "events")
            }
        }
    }
}

/**
 * 元数据管理路由
 */
private fun Route.metadataRoutes() {
    route("/metadata") {
        get {
            try {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20

                val repository = TelemetryRepositoryFactory.getRepository()
                val metadata = repository.getMetadata(page, pageSize)
                val totalCount = repository.getTotalMetadataCount()
                val totalPages = (totalCount + pageSize - 1) / pageSize

                // 格式化时间戳
                val formattedMetadata = metadata.map { meta ->
                    val timestamp = meta.timestamp
                    val formattedTime = formatTimestamp(timestamp)

                    mapOf(
                        "id" to meta._id,
                        "systemId" to meta.systemId,
                        "pluginVersion" to meta.pluginVersion,
                        "ideVersion" to meta.ideVersion,
                        "ideBuild" to meta.ideBuild,
                        "os" to meta.os,
                        "osVersion" to meta.osVersion,
                        "javaVersion" to meta.javaVersion,
                        "timestamp" to meta.timestamp,
                        "formattedTime" to formattedTime
                    )
                }

                val model = call.createBaseModel(
                    "metadata", mapOf(
                        "metadata" to formattedMetadata,
                        "currentPage" to page,
                        "totalPages" to totalPages,
                        "pageSize" to pageSize,
                        "totalCount" to totalCount
                    )
                )

                call.respond(FreeMarkerContent("web/metadata.ftl", model))
            } catch (e: Exception) {
                logger.error(e) { "获取元数据失败: ${e.message}" }
                call.respondWithError("web/metadata.ftl", "获取元数据失败: ${e.message}", "metadata")
            }
        }

        // 添加元数据详情页面
        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("元数据ID不能为空")
                val repository = TelemetryRepositoryFactory.getRepository()
                val metadata = repository.getMetadataById(id) ?: throw IllegalArgumentException("未找到该元数据")

                // 格式化时间
                val timestamp = metadata.timestamp
                val formattedTime = formatTimestamp(timestamp)

                val model = call.createBaseModel(
                    "metadata", mapOf(
                        "metadata" to mapOf(
                            "id" to metadata._id,
                            "systemId" to metadata.systemId,
                            "pluginVersion" to metadata.pluginVersion,
                            "ideVersion" to metadata.ideVersion,
                            "ideBuild" to metadata.ideBuild,
                            "os" to metadata.os,
                            "osVersion" to metadata.osVersion,
                            "javaVersion" to metadata.javaVersion,
                            "timestamp" to metadata.timestamp
                        ),
                        "formattedTime" to formattedTime
                    )
                )

                call.respond(FreeMarkerContent("web/metadata_details.ftl", model))
            } catch (e: Exception) {
                logger.error(e) { "获取元数据详情失败: ${e.message}" }
                call.respondWithError("web/metadata.ftl", "获取元数据详情失败: ${e.message}", "metadata")
            }
        }
    }
}

/**
 * 报表路由
 */
private fun Route.reportsRoutes() {
    route("/reports") {
        get {
            try {
                val timeRange = call.parameters["timeRange"]?.toIntOrNull() ?: 30 // 默认30天
                val category = call.parameters["category"]
                val groupBy = call.parameters["groupBy"] ?: "day"

                val repository = TelemetryRepositoryFactory.getRepository()

                // 根据groupBy参数调用不同的统计方法
                val reportData = when (groupBy) {
                    "day" -> repository.getEventCountByDay(timeRange, category)
                    "week" -> repository.getEventCountByWeek(timeRange, category)
                    "month" -> repository.getEventCountByMonth(timeRange, category)
                    "category" -> repository.getEventCountByCategory(category, timeRange)
                    else -> repository.getEventCountByDay(timeRange, category)
                }
                
                // 获取类别分布数据和事件统计
                val eventsByCategory = repository.getEventCountByCategory(category, timeRange)
                
                // 获取事件趋势数据，根据groupBy选择不同的数据
                val eventsByDay = when (groupBy) {
                    "day" -> repository.getEventCountByDay(timeRange, category)
                    "week" -> repository.getEventCountByWeek(timeRange, category)
                    "month" -> repository.getEventCountByMonth(timeRange, category)
                    else -> repository.getEventCountByDay(timeRange, category)
                }
                
                // 计算总事件数
                val totalEvents = eventsByCategory.values.sum()
                
                // 获取独立用户数
                val uniqueUsers = repository.getUniqueUsersCount(timeRange)
                
                // 计算日均事件数
                val dailyAverage = if (timeRange > 0 && totalEvents > 0) {
                    (totalEvents.toDouble() / timeRange).toInt()
                } else {
                    0
                }
                
                // 获取最常见的事件类别
                val topCategory = if (eventsByCategory.isNotEmpty()) {
                    eventsByCategory.entries.maxByOrNull { it.value }?.key ?: "-"
                } else {
                    "-"
                }

                val categories = repository.getEventCategories()

                val model = call.createBaseModel(
                    "reports", mapOf(
                        "reportData" to reportData,
                        "eventsByCategory" to eventsByCategory,
                        "eventsByDay" to eventsByDay,
                        "totalEvents" to totalEvents,
                        "uniqueUsers" to uniqueUsers,
                        "dailyAverage" to dailyAverage,
                        "topCategory" to topCategory,
                        "timeRange" to timeRange,
                        "selectedCategory" to (category ?: ""),
                        "groupBy" to groupBy,
                        "categories" to categories
                    )
                )

                call.respond(FreeMarkerContent("web/reports.ftl", model))
            } catch (e: Exception) {
                logger.error(e) { "获取报表数据失败: ${e.message}" }
                call.respondWithError("web/reports.ftl", "获取报表数据失败: ${e.message}", "reports")
            }
        }
    }
}

/**
 * 系统设置路由
 */
private fun Route.settingsRoutes() {
    route("/settings") {
        get {
            try {
                val repository = SystemSettingsRepositoryFactory.getRepository()
                val settings = repository.getSettings()

                // 获取系统信息
                val osName = System.getProperty("os.name")
                val osVersion = System.getProperty("os.version")
                val javaVersion = System.getProperty("java.version")
                val availableProcessors = Runtime.getRuntime().availableProcessors()
                val maxMemory = Runtime.getRuntime().maxMemory()
                val totalMemory = Runtime.getRuntime().totalMemory()
                val freeMemory = Runtime.getRuntime().freeMemory()
                
                // 格式化内存信息为MB
                val maxMemoryMB = (maxMemory / (1024 * 1024)).toString()
                val totalMemoryMB = (totalMemory / (1024 * 1024)).toString()
                val freeMemoryMB = (freeMemory / (1024 * 1024)).toString()
                
                // 数据库类型
                val databaseType = "MongoDB" // 或从配置中获取

                val model = call.createBaseModel(
                    "settings", mapOf(
                        "settings" to settings,
                        "osName" to osName,
                        "osVersion" to osVersion,
                        "javaVersion" to javaVersion,
                        "availableProcessors" to availableProcessors,
                        "maxMemoryMB" to maxMemoryMB,
                        "totalMemoryMB" to totalMemoryMB,
                        "freeMemoryMB" to freeMemoryMB,
                        "databaseType" to databaseType
                    )
                )

                call.respond(FreeMarkerContent("web/settings.ftl", model))
            } catch (e: Exception) {
                logger.error(e) { "获取系统设置失败: ${e.message}" }
                call.respondWithError("web/settings.ftl", "获取系统设置失败: ${e.message}", "settings")
            }
        }

        // 更新系统设置
        post {
            try {
                val formParameters = call.receiveParameters()
                val repository = SystemSettingsRepositoryFactory.getRepository()

                // 从表单中获取设置并更新
                val dataRetentionDays = formParameters["dataRetentionDays"]?.toIntOrNull() ?: 365
                val apiKeyEnabled = formParameters["apiKeyEnabled"] == "on"
                val anonymousReportingEnabled = formParameters["anonymousReportingEnabled"] == "on"

                // 更新设置
                repository.updateSettings(
                    dataRetentionDays = dataRetentionDays,
                    apiKeyEnabled = apiKeyEnabled,
                    anonymousReportingEnabled = anonymousReportingEnabled
                )

                call.respondRedirect("/web/settings?updated=true")
            } catch (e: Exception) {
                logger.error(e) { "更新系统设置失败: ${e.message}" }
                call.respondWithError("web/settings.ftl", "更新系统设置失败: ${e.message}", "settings")
            }
        }
    }
}

/**
 * 用户管理路由
 */
private fun Route.usersRoutes() {
    route("/users") {
        get {
            try {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20

                val repository = AdminUserRepositoryFactory.getRepository()
                val users = repository.getAllUsers(page, pageSize)
                val totalCount = repository.getUserCount()
                val totalPages = (totalCount + pageSize - 1) / pageSize

                val model = call.createBaseModel(
                    "users", mapOf(
                        "users" to users,
                        "currentPage" to page,
                        "totalPages" to totalPages,
                        "pageSize" to pageSize,
                        "totalCount" to totalCount
                    )
                )

                call.respond(FreeMarkerContent("web/users.ftl", model))
            } catch (e: Exception) {
                logger.error(e) { "获取用户列表失败: ${e.message}" }
                call.respondWithError("web/users.ftl", "获取用户列表失败: ${e.message}", "users")
            }
        }

        // 新增用户页面
        get("/new") {
            val model = call.createBaseModel("users", emptyMap())
            call.respond(FreeMarkerContent("web/user_new.ftl", model))
        }

        // 创建新用户
        post("/new") {
            try {
                val formParameters = call.receiveParameters()
                val username = formParameters["username"] ?: ""
                val password = formParameters["password"] ?: ""
                val displayName = formParameters["displayName"] ?: ""
                val role = formParameters["role"] ?: "USER"
                val email = formParameters["email"] ?: ""

                val repository = AdminUserRepositoryFactory.getRepository()

                // 检查用户名是否已存在
                if (repository.usernameExists(username)) {
                    call.respondWithError("web/user_new.ftl", "用户名已存在", "users")
                    return@post
                }

                // 创建用户
                val createRequest = CreateUserRequest(
                    username = username,
                    password = password,
                    displayName = displayName,
                    role = role,
                    email = email
                )

                repository.createUser(createRequest)
                call.respondRedirect("/web/users?created=true")
            } catch (e: Exception) {
                logger.error(e) { "创建用户失败: ${e.message}" }
                call.respondWithError("web/user_new.ftl", "创建用户失败: ${e.message}", "users")
            }
        }
        
        // 添加与表单匹配的创建用户端点
        post("/create") {
            try {
                val formParameters = call.receiveParameters()
                val username = formParameters["username"] ?: ""
                val password = formParameters["password"] ?: ""
                val displayName = formParameters["displayName"] ?: ""
                val role = formParameters["role"] ?: "USER"
                val email = formParameters["email"] ?: ""

                val repository = AdminUserRepositoryFactory.getRepository()

                // 检查用户名是否已存在
                if (repository.usernameExists(username)) {
                    call.respondWithError("web/user_new.ftl", "用户名已存在", "users")
                    return@post
                }

                // 创建用户
                val createRequest = CreateUserRequest(
                    username = username,
                    password = password,
                    displayName = displayName,
                    role = role,
                    email = email
                )

                repository.createUser(createRequest)
                call.respondRedirect("/web/users?created=true")
            } catch (e: Exception) {
                logger.error(e) { "创建用户失败: ${e.message}" }
                call.respondWithError("web/user_new.ftl", "创建用户失败: ${e.message}", "users")
            }
        }

        // 编辑用户页面
        get("/{username}/edit") {
            try {
                val username = call.parameters["username"] ?: throw IllegalArgumentException("用户名不能为空")
                val repository = AdminUserRepositoryFactory.getRepository()
                val user = repository.findByUsername(username) ?: throw IllegalArgumentException("未找到该用户")

                val model = call.createBaseModel(
                    "users", mapOf(
                        "editUser" to user
                    )
                )

                call.respond(FreeMarkerContent("web/user_edit.ftl", model))
            } catch (e: Exception) {
                logger.error(e) { "获取用户编辑页面失败: ${e.message}" }
                call.respondWithError("web/users.ftl", "获取用户编辑页面失败: ${e.message}", "users")
            }
        }

        // 更新用户
        post("/{username}/edit") {
            try {
                val username = call.parameters["username"] ?: throw IllegalArgumentException("用户名不能为空")
                val formParameters = call.receiveParameters()

                val displayName = formParameters["displayName"]
                val role = formParameters["role"]
                val email = formParameters["email"]
                val password = formParameters["password"]?.takeIf { it.isNotBlank() }
                val enabled = formParameters["enabled"] == "on"

                val repository = AdminUserRepositoryFactory.getRepository()

                // 创建更新请求
                val updateRequest = UpdateUserRequest(
                    displayName = displayName,
                    role = role,
                    email = email,
                    password = password,
                    enabled = enabled
                )

                // 更新用户
                repository.updateUser(username, updateRequest)
                call.respondRedirect("/web/users?updated=true")
            } catch (e: Exception) {
                logger.error(e) { "更新用户失败: ${e.message}" }
                val username = call.parameters["username"] ?: "unknown"
                call.respondWithError("web/user_edit.ftl", "更新用户失败: ${e.message}", "users")
            }
        }
        
        // 添加与表单匹配的更新用户端点
        post("/update") {
            try {
                val formParameters = call.receiveParameters()
                val username = formParameters["username"] ?: throw IllegalArgumentException("用户名不能为空")
                
                val displayName = formParameters["displayName"]
                val role = formParameters["role"]
                val email = formParameters["email"]
                val password = formParameters["password"]?.takeIf { it.isNotBlank() }
                val enabled = formParameters["enabled"] == "on"

                val repository = AdminUserRepositoryFactory.getRepository()

                // 创建更新请求
                val updateRequest = UpdateUserRequest(
                    displayName = displayName,
                    role = role,
                    email = email,
                    password = password,
                    enabled = enabled
                )

                // 更新用户
                repository.updateUser(username, updateRequest)
                call.respondRedirect("/web/users?updated=true")
            } catch (e: Exception) {
                logger.error(e) { "更新用户失败: ${e.message}" }
                call.respondWithError("web/user_edit.ftl", "更新用户失败: ${e.message}", "users")
            }
        }

        // 删除用户
        post("/{username}/delete") {
            try {
                val username = call.parameters["username"] ?: throw IllegalArgumentException("用户名不能为空")

                // 不允许删除root用户
                if (username == "root" || username == "admin") {
                    call.respondWithError("web/users.ftl", "不能删除系统管理员账户", "users")
                    return@post
                }

                val repository = AdminUserRepositoryFactory.getRepository()
                val deleted = repository.deleteUser(username)

                if (deleted) {
                    call.respondRedirect("/web/users?deleted=true")
                } else {
                    call.respondWithError("web/users.ftl", "删除用户失败", "users")
                }
            } catch (e: Exception) {
                logger.error(e) { "删除用户失败: ${e.message}" }
                call.respondWithError("web/users.ftl", "删除用户失败: ${e.message}", "users")
            }
        }
        
        // 添加与表单匹配的删除用户端点
        post("/delete") {
            try {
                val formParameters = call.receiveParameters()
                val username = formParameters["username"] ?: throw IllegalArgumentException("用户名不能为空")
                
                // 不允许删除root用户
                if (username == "root" || username == "admin") {
                    call.respondWithError("web/users.ftl", "不能删除系统管理员账户", "users")
                    return@post
                }
                
                val repository = AdminUserRepositoryFactory.getRepository()
                val deleted = repository.deleteUser(username)
                
                if (deleted) {
                    call.respondRedirect("/web/users?deleted=true")
                } else {
                    call.respondWithError("web/users.ftl", "删除用户失败", "users")
                }
            } catch (e: Exception) {
                logger.error(e) { "删除用户失败: ${e.message}" }
                call.respondWithError("web/users.ftl", "删除用户失败: ${e.message}", "users")
            }
        }
    }
} 