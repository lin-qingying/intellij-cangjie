package cn.cangnova

import cn.cangnova.controller.web.AdminController.adminRoutes
import cn.cangnova.controller.api.AdminUserController.adminUserRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import cn.cangnova.controller.api.TelemetryController.telemetryRoutes
import io.ktor.server.http.content.*
import io.ktor.server.sessions.*

/**
 * 配置应用程序的路由
 */
fun Application.configureRouting() {
    routing {
        // 根路径处理
        rootRoutes()
        
        // Web 界面路由 (FreeMarker 模板)
        webRoutes()
        
        // API 路由 (JSON 响应)
        apiRoutes()
    }
}

/**
 * 根路径路由配置
 */
private fun Routing.rootRoutes() {
    // Swagger UI 路由
    swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    
    // 静态资源
    staticRoutes()
}

/**
 * Web 界面路由配置 (FreeMarker 模板)
 */
private fun Routing.webRoutes() {
    // 管理后台 Web 界面路由
    adminRoutes()
}

/**
 * API 路由配置 (JSON 响应)
 */
private fun Routing.apiRoutes() {
    route("/api") {
        // 遥测数据 API
        telemetryRoutes()
        
        // 管理员用户 API
        route("/admin") {
            adminUserRoutes()
        }
    }
}

/**
 * 静态资源路由配置
 */
private fun Routing.staticRoutes() {
    static("/static") {
        resources("static")
    }
}
