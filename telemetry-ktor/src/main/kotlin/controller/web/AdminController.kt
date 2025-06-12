package cn.cangnova.controller.web

import cn.cangnova.AdminSession
import cn.cangnova.model.CreateUserRequest
import cn.cangnova.model.TelemetryMetadata
import cn.cangnova.model.UpdateUserRequest
import cn.cangnova.repository.factory.TelemetryRepositoryFactory
import cn.cangnova.repository.factory.AdminUserRepositoryFactory
import cn.cangnova.repository.factory.SystemSettingsRepositoryFactory
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 管理后台控制器，处理管理后台相关的路由
 */
object AdminController {
    
    /**
     * 注册管理后台路由
     */
    fun Route.adminRoutes() {
        route("/web") {
            // 根路径处理
            rootAdminRoutes()
            
            // 认证相关路由
            authRoutes()
            
            // 需要认证的路由
            authenticate("admin-session") {
                // 仪表盘路由
                dashboardRoutes()
                
                // 数据管理路由
                dataManagementRoutes()
                
                // 系统设置路由
                settingsRoutes()
                
                // 用户管理路由
                userManagementRoutes()
            }
        }
    }
    
    /**
     * 管理后台根路径路由
     */
    private fun Route.rootAdminRoutes() {
        get {
            // 检查用户是否已登录
            val session = call.sessions.get<AdminSession>()
            if (session != null) {
                // 已登录，重定向到仪表盘
                call.respondRedirect("/web/dashboard")
            } else {
                // 未登录，重定向到登录页面
                call.respondRedirect("/web/login")
            }
        }
    }
    
    /**
     * 认证相关路由
     */
    private fun Route.authRoutes() {
        // 登录页面
        get("/login") {
            val error = call.parameters["error"]
            call.respond(FreeMarkerContent("admin/login.ftl", mapOf("error" to error)))
        }
        
        // 处理登录请求
        post("/login") {
            val formParameters = call.receiveParameters()
            val username = formParameters["username"] ?: ""
            val password = formParameters["password"] ?: ""
            
            val repository = AdminUserRepositoryFactory.getRepository()
            val user = repository.validateCredentials(username, password)
            
            if (user != null) {
                // 登录成功，创建会话
                repository.updateLastLoginTime(username)
                call.sessions.set(AdminSession(username))
                call.respondRedirect("/web/dashboard")
            } else {
                // 登录失败
                call.respondRedirect("/web/login?error=invalid_credentials")
            }
        }
        
        // 登出
        get("/logout") {
            call.sessions.clear<AdminSession>()
            call.respondRedirect("/web/login")
        }
    }
    
    /**
     * 仪表盘路由
     */
    private fun Route.dashboardRoutes() {
        // 仪表盘
        get("/dashboard") {
            val principal = call.principal<UserIdPrincipal>()
            val username = principal?.name ?: "Unknown"
            val userRepository = AdminUserRepositoryFactory.getRepository()
            val user = userRepository.findByUsername(username)
            
            // 获取统计数据
            val repository = TelemetryRepositoryFactory.getRepository()
            val totalEvents = repository.getTotalEventsCount()
            val totalMetadata = repository.getTotalMetadataCount()
            val recentMetadata = repository.getRecentMetadata(5)
            
            // 准备视图模型
            val model = mapOf(
                "user" to user,
                "totalEvents" to totalEvents,
                "totalMetadata" to totalMetadata,
                "recentMetadata" to recentMetadata.map { formatMetadata(it) }
            )
            
            call.respond(FreeMarkerContent("admin/dashboard.ftl", model))
        }
    }
    
    /**
     * 数据管理路由
     */
    private fun Route.dataManagementRoutes() {
        // 事件列表
        get("/events") {
            try {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 50
                val category = call.parameters["category"]
                val name = call.parameters["name"]
                val startDate = call.parameters["startDate"]
                val endDate = call.parameters["endDate"]
                
                val repository = TelemetryRepositoryFactory.getRepository()
                // 修改为使用扩展的方法，传递所有筛选参数
                val events = repository.getFilteredEvents(page, pageSize, category, name, startDate, endDate)
                val totalCount = repository.getFilteredEventsCount(category, name, startDate, endDate)
                val totalPages = (totalCount + pageSize - 1) / pageSize
                val categories = repository.getEventCategories()
                val eventNames = repository.getEventNames(category)
                
                // 获取所有事件关联的元数据ID
                val metadataIds = events.mapNotNull { it.metadataId }.distinct()
                
                // 创建元数据映射，用于在前端显示
                val metadataMap = mutableMapOf<String, Map<String, String>>()
                
                // 为每个事件直接获取元数据
                events.forEach { event ->
                    if (event.metadataId != null) {
                        try {
                            // 直接调用 getMetadataById 方法获取元数据
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
                            } else {
                                // 如果找不到元数据，使用默认值
                                metadataMap[event.metadataId!!] = mapOf(
                                    "pluginVersion" to "未知",
                                    "ideVersion" to "未知",
                                    "os" to "未知",
                                    "osVersion" to "",
                                    "javaVersion" to "未知",
                                    "systemId" to "未知",
                                    "ideBuild" to "未知"

                                )
                            }
                        } catch (e: Exception) {
                            // 如果查询失败，使用默认值
                            metadataMap[event.metadataId!!] = mapOf(
                                "pluginVersion" to "未知",
                                "ideVersion" to "未知",
                                "os" to "未知",
                                "osVersion" to "",
                                "javaVersion" to "未知",
                                "systemId" to "未知"
                            )
                        }
                    }
                }
                
                val model = mapOf(
                    "events" to events,
                    "currentPage" to page,
                    "totalPages" to totalPages,
                    "pageSize" to pageSize,
                    "category" to category,
                    "categories" to categories,
                    "totalEvents" to totalCount,
                    "selectedEventName" to name,
                    "eventNames" to eventNames,
                    "startDate" to startDate,
                    "endDate" to endDate,
                    "metadataMap" to metadataMap
                )
                
                call.respond(FreeMarkerContent("admin/events.ftl", model))
            } catch (e: Exception) {
                call.respond(FreeMarkerContent("admin/events.ftl", mapOf(
                    "error" to "获取事件数据失败: ${e.message}"
                )))
            }
        }
        
        // 导出事件数据
        get("/events/export") {
            try {
                val format = call.parameters["format"] ?: "csv"
                val category = call.parameters["category"]
                val name = call.parameters["name"]
                val startDate = call.parameters["startDate"]
                val endDate = call.parameters["endDate"]
                
                val repository = TelemetryRepositoryFactory.getRepository()
                // 获取最多1000条事件用于导出，同样使用筛选条件
                val events = repository.getFilteredEvents(1, 1000, category, name, startDate, endDate)
                
                when (format.lowercase()) {
                    "json" -> {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName, "events_data.json"
                            ).toString()
                        )
                        
                        // 将事件数据转换为JSON格式
                        val eventsJson = events.map { event ->
                            """
                            {
                                "id": "${event.id}",
                                "category": "${event.category}",
                                "name": "${event.name}",
                                "value": "${event.value}",
                                "timestamp": "${event.timestamp}",
                                "properties": ${
                                    event.properties.entries.joinToString(
                                        prefix = "{", postfix = "}",
                                        transform = { "\"${it.key}\": \"${it.value}\"" }
                                    )
                                }
                            }
                            """.trimIndent()
                        }
                        
                        call.respondText(
                            contentType = ContentType.Application.Json,
                            text = """
                            {
                                "events": [
                                    ${eventsJson.joinToString(",\n")}
                                ],
                                "totalCount": ${events.size},
                                "category": ${if (category != null) "\"$category\"" else "null"},
                                "exportedAt": "${Instant.now().toString()}"
                            }
                            """.trimIndent()
                        )
                    }
                    "csv" -> {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName, "events_data.csv"
                            ).toString()
                        )
                        
                        val csvContent = buildString {
                            // 添加CSV标题行
                            appendLine("ID,类别,名称,值,时间戳,属性")
                            
                            // 添加事件数据行
                            events.forEach { event ->
                                val propertiesStr = event.properties.entries.joinToString(";") { "${it.key}=${it.value}" }
                                appendLine("${event.id},${event.category},${event.name},${event.value},${event.timestamp},\"${propertiesStr}\"")
                            }
                        }
                        
                        call.respondText(
                            contentType = ContentType.Text.CSV,
                            text = csvContent
                        )
                    }
                    else -> {
                        call.respond(HttpStatusCode.BadRequest, "不支持的导出格式")
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "导出事件数据失败: ${e.message}")
            }
        }
        
        // 元数据列表
        get("/metadata") {
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 50
            
            val repository = TelemetryRepositoryFactory.getRepository()
            val metadata = repository.getMetadata(page, pageSize)
            val totalCount = repository.getTotalMetadataCount()
            val totalPages = (totalCount + pageSize - 1) / pageSize
            
            val model = mapOf(
                "metadata" to metadata.map { formatMetadata(it) },
                "currentPage" to page,
                "totalPages" to totalPages,
                "pageSize" to pageSize
            )
            
            call.respond(FreeMarkerContent("admin/metadata.ftl", model))
        }
        
        // 统计报表
        get("/reports") {
            try {
                // 获取筛选参数
                val timeRange = call.parameters["timeRange"]?.toIntOrNull() ?: 30
                val category = call.parameters["category"]
                val groupBy = call.parameters["groupBy"] ?: "day"
                
                val repository = TelemetryRepositoryFactory.getRepository()
                
                // 获取事件类别分布数据
                val eventsByCategory = repository.getEventCountByCategory(category, timeRange)
                
                // 获取时间序列数据
                val eventsByTime = when (groupBy) {
                    "week" -> repository.getEventCountByWeek(timeRange, category)
                    "month" -> repository.getEventCountByMonth(timeRange, category)
                    else -> repository.getEventCountByDay(timeRange, category)
                }
                
                // 获取摘要统计数据
                val totalEvents = eventsByCategory.values.sum()
                val uniqueUsers = repository.getUniqueUsersCount(timeRange)
                val dailyAverage = if (timeRange > 0) totalEvents / timeRange else 0
                
                // 获取最常见类别
                val topCategory = eventsByCategory.entries
                    .maxByOrNull { it.value }
                    ?.key ?: "-"
                
                // 获取所有事件类别，用于筛选
                val categories = repository.getEventCategories()
                
                val model = mapOf(
                    "eventsByCategory" to eventsByCategory,
                    "eventsByDay" to eventsByTime,
                    "totalEvents" to totalEvents,
                    "uniqueUsers" to uniqueUsers,
                    "dailyAverage" to dailyAverage,
                    "topCategory" to topCategory,
                    "categories" to categories,
                    "timeRange" to timeRange,
                    "selectedCategory" to category,
                    "groupBy" to groupBy
                )
                
                call.respond(FreeMarkerContent("admin/reports.ftl", model))
            } catch (e: Exception) {
                call.respond(FreeMarkerContent("admin/reports.ftl", mapOf(
                    "error" to "获取统计数据失败: ${e.message}"
                )))
            }
        }
        
        // 导出报表数据
        get("/reports/export") {
            try {
                val format = call.parameters["format"] ?: "csv"
                val timeRange = call.parameters["timeRange"]?.toIntOrNull() ?: 30
                val category = call.parameters["category"]
                
                val repository = TelemetryRepositoryFactory.getRepository()
                val eventsByCategory = repository.getEventCountByCategory(category, timeRange)
                val eventsByDay = repository.getEventCountByDay(timeRange, category)
                
                when (format.lowercase()) {
                    "json" -> {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName, "reports_data.json"
                            ).toString()
                        )
                        call.respondText(
                            contentType = ContentType.Application.Json,
                            text = """
                            {
                                "eventsByCategory": ${eventsByCategory.entries.joinToString(
                                    prefix = "{", postfix = "}",
                                    transform = { "\"${it.key}\": ${it.value}" }
                                )},
                                "eventsByDay": ${eventsByDay.entries.joinToString(
                                    prefix = "{", postfix = "}",
                                    transform = { "\"${it.key}\": ${it.value}" }
                                )}
                            }
                            """.trimIndent()
                        )
                    }
                    "csv" -> {
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName, "reports_data.csv"
                            ).toString()
                        )
                        
                        val csvContent = buildString {
                            // 类别数据
                            appendLine("类别,事件数量")
                            eventsByCategory.forEach { (category, count) ->
                                appendLine("$category,$count")
                            }
                            appendLine()
                            
                            // 每日数据
                            appendLine("日期,事件数量")
                            eventsByDay.forEach { (date, count) ->
                                appendLine("$date,$count")
                            }
                        }
                        
                        call.respondText(
                            contentType = ContentType.Text.CSV,
                            text = csvContent
                        )
                    }
                    else -> {
                        call.respond(HttpStatusCode.BadRequest, "不支持的导出格式")
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "导出数据失败: ${e.message}")
            }
        }
    }
    
    /**
     * 系统设置路由
     */
    private fun Route.settingsRoutes() {
        // 系统配置
        get("/settings") {
            val systemSettingsRepository = SystemSettingsRepositoryFactory.getRepository()
            val settings = systemSettingsRepository.getSettings()
            
            val model = mapOf(
                "databaseType" to TelemetryRepositoryFactory.getDatabaseType(),
                "javaVersion" to System.getProperty("java.version"),
                "osName" to System.getProperty("os.name"),
                "osVersion" to System.getProperty("os.version"),
                "maxMemoryMB" to (Runtime.getRuntime().maxMemory() / 1024 / 1024).toString(),
                "totalMemoryMB" to (Runtime.getRuntime().totalMemory() / 1024 / 1024).toString(),
                "freeMemoryMB" to (Runtime.getRuntime().freeMemory() / 1024 / 1024).toString(),
                "availableProcessors" to Runtime.getRuntime().availableProcessors().toString(),
                "settings" to settings,
                "success" to call.parameters["success"],
                "reset" to call.parameters["reset"],
                "error" to call.parameters["error"],
                "cleanupSuccess" to call.parameters["cleanupSuccess"],
                "cleanupCount" to call.parameters["cleanupCount"]
            )
            
            call.respond(FreeMarkerContent("admin/settings.ftl", model))
        }
        
        // 更新系统设置
        post("/settings/update") {
            try {
                val parameters = call.receiveParameters()
                val systemSettingsRepository = SystemSettingsRepositoryFactory.getRepository()
                val currentSettings = systemSettingsRepository.getSettings()
                
                // 从请求参数中获取设置值
                val dataRetentionDays = parameters["dataRetentionDays"]?.toIntOrNull() ?: currentSettings.dataRetentionDays
                val cleanupSchedule = parameters["cleanupSchedule"] ?: currentSettings.cleanupSchedule
                val enableAutoCleanup = parameters["enableAutoCleanup"] == "on"
                val backupSchedule = parameters["backupSchedule"] ?: currentSettings.backupSchedule
                val backupRetention = parameters["backupRetention"]?.toIntOrNull() ?: currentSettings.backupRetention
                val enableAutoBackup = parameters["enableAutoBackup"] == "on"
                val eventBatchSize = parameters["eventBatchSize"]?.toIntOrNull() ?: currentSettings.eventBatchSize
                val connectionPoolSize = parameters["connectionPoolSize"]?.toIntOrNull() ?: currentSettings.connectionPoolSize
                val logLevel = parameters["logLevel"] ?: currentSettings.logLevel
                val logRetention = parameters["logRetention"]?.toIntOrNull() ?: currentSettings.logRetention
                val enableCache = parameters["enableCache"] == "on"
                val cacheSize = parameters["cacheSize"]?.toIntOrNull() ?: currentSettings.cacheSize
                val cacheExpiry = parameters["cacheExpiry"]?.toIntOrNull() ?: currentSettings.cacheExpiry
                
                // 创建更新后的设置对象
                val updatedSettings = currentSettings.copy(
                    dataRetentionDays = dataRetentionDays,
                    cleanupSchedule = cleanupSchedule,
                    enableAutoCleanup = enableAutoCleanup,
                    backupSchedule = backupSchedule,
                    backupRetention = backupRetention,
                    enableAutoBackup = enableAutoBackup,
                    eventBatchSize = eventBatchSize,
                    connectionPoolSize = connectionPoolSize,
                    logLevel = logLevel,
                    logRetention = logRetention,
                    enableCache = enableCache,
                    cacheSize = cacheSize,
                    cacheExpiry = cacheExpiry
                )
                
                // 保存更新后的设置
                systemSettingsRepository.updateSettings(updatedSettings)
                
                // 重定向回设置页面，带上成功消息
                call.respondRedirect("/web/settings?success=true")
            } catch (e: Exception) {
                // 重定向回设置页面，带上错误消息
                call.respondRedirect("/web/settings?error=${e.message}")
            }
        }
        
        // 重置系统设置
        post("/settings/reset") {
            try {
                val systemSettingsRepository = SystemSettingsRepositoryFactory.getRepository()
                systemSettingsRepository.resetSettings()
                
                // 重定向回设置页面，带上成功消息
                call.respondRedirect("/web/settings?reset=true")
            } catch (e: Exception) {
                // 重定向回设置页面，带上错误消息
                call.respondRedirect("/web/settings?error=${e.message}")
            }
        }
        
        // 执行数据清理
        post("/settings/cleanup") {
            try {
                val systemSettingsRepository = SystemSettingsRepositoryFactory.getRepository()
                val settings = systemSettingsRepository.getSettings()
                val telemetryRepository = TelemetryRepositoryFactory.getRepository()
                
                // 执行数据清理操作
                val deletedCount = telemetryRepository.cleanupOldData(settings.dataRetentionDays)
                
                // 重定向回设置页面，带上成功消息和删除的记录数
                call.respondRedirect("/web/settings?cleanupSuccess=true&cleanupCount=$deletedCount")
            } catch (e: Exception) {
                // 重定向回设置页面，带上错误消息
                call.respondRedirect("/web/settings?error=${e.message}")
            }
        }
    }
    
    /**
     * 用户管理路由
     */
    private fun Route.userManagementRoutes() {
        // 用户列表
        get("/users") {
            try {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 10
                val success = call.parameters["success"]
                val error = call.parameters["error"]
                
                val repository = AdminUserRepositoryFactory.getRepository()
                val users = repository.getAllUsers(page, pageSize)
                val totalCount = repository.getUserCount()
                val totalPages = (totalCount + pageSize - 1) / pageSize
                
                val model = mapOf(
                    "users" to users,
                    "currentPage" to page,
                    "totalPages" to totalPages,
                    "pageSize" to pageSize,
                    "totalCount" to totalCount,
                    "success" to success,
                    "error" to error
                )
                
                call.respond(FreeMarkerContent("admin/users.ftl", model))
            } catch (e: Exception) {
                call.respond(FreeMarkerContent("admin/users.ftl", mapOf(
                    "error" to "获取用户列表失败: ${e.message}"
                )))
            }
        }
        
        // 创建用户
        post("/users/create") {
            try {
                val parameters = call.receiveParameters()
                val username = parameters["username"] ?: ""
                val password = parameters["password"] ?: ""
                val displayName = parameters["displayName"] ?: ""
                val role = parameters["role"] ?: "USER"
                val email = parameters["email"] ?: ""
                
                val repository = AdminUserRepositoryFactory.getRepository()
                
                // 检查用户名是否已存在
                if (repository.usernameExists(username)) {
                    call.respondRedirect("/web/users?error=用户名已存在")
                    return@post
                }
                
                // 创建用户请求
                val request = CreateUserRequest(
                    username = username,
                    password = password,
                    displayName = displayName,
                    role = role,
                    email = email
                )
                
                // 创建用户
                repository.createUser(request)
                
                // 重定向回用户列表页面，带上成功消息
                call.respondRedirect("/web/users?success=用户创建成功")
            } catch (e: Exception) {
                // 重定向回用户列表页面，带上错误消息
                call.respondRedirect("/web/users?error=用户创建失败: ${e.message}")
            }
        }
        
        // 更新用户
        post("/users/update") {
            try {
                val parameters = call.receiveParameters()
                val username = parameters["username"] ?: ""
                val displayName = parameters["displayName"]
                val role = parameters["role"]
                val email = parameters["email"]
                val password = parameters["password"]?.takeIf { it.isNotEmpty() }
                val enabled = parameters["enabled"]?.let { it == "on" }
                
                val repository = AdminUserRepositoryFactory.getRepository()
                
                // 创建更新用户请求
                val request = UpdateUserRequest(
                    displayName = displayName,
                    role = role,
                    email = email,
                    password = password,
                    enabled = enabled
                )
                
                // 更新用户
                val updatedUser = repository.updateUser(username, request)
                
                if (updatedUser != null) {
                    // 重定向回用户列表页面，带上成功消息
                    call.respondRedirect("/web/users?success=用户更新成功")
                } else {
                    // 重定向回用户列表页面，带上错误消息
                    call.respondRedirect("/web/users?error=用户不存在")
                }
            } catch (e: Exception) {
                // 重定向回用户列表页面，带上错误消息
                call.respondRedirect("/web/users?error=用户更新失败: ${e.message}")
            }
        }
        
        // 删除用户
        post("/users/delete") {
            try {
                val parameters = call.receiveParameters()
                val username = parameters["username"] ?: ""
                
                // 不允许删除admin用户
                if (username == "admin") {
                    call.respondRedirect("/web/users?error=不能删除管理员账户")
                    return@post
                }
                
                val repository = AdminUserRepositoryFactory.getRepository()
                val deleted = repository.deleteUser(username)
                
                if (deleted) {
                    // 重定向回用户列表页面，带上成功消息
                    call.respondRedirect("/web/users?success=用户删除成功")
                } else {
                    // 重定向回用户列表页面，带上错误消息
                    call.respondRedirect("/web/users?error=用户不存在")
                }
            } catch (e: Exception) {
                // 重定向回用户列表页面，带上错误消息
                call.respondRedirect("/web/users?error=用户删除失败: ${e.message}")
            }
        }
    }
    
    /**
     * 格式化元数据，添加可读的时间
     */
    private fun formatMetadata(metadata: TelemetryMetadata): Map<String, Any> {
        val timestamp = Instant.ofEpochMilli(metadata.timestamp)
        val dateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        val formattedTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        return mapOf(
            "metadata" to metadata,
            "formattedTime" to formattedTime
        )
    }
} 