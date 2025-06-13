package cn.cangnova

import cn.cangnova.controller.web.AdminController.adminRoutes
import cn.cangnova.controller.api.AdminUserController.adminUserRoutes
import cn.cangnova.controller.web.PrivacyController.privacyRoutes
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
        
        // 公共页面路由 (无需认证)
        publicRoutes()
    }
}

/**
 * 根路径路由配置
 */
private fun Routing.rootRoutes() {
    // Swagger UI 路由
    swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

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
 * 公共页面路由配置 (无需认证)
 */
private fun Routing.publicRoutes() {
    // 隐私政策路由
    privacyRoutes()
}

/**
 * 静态资源路由配置
 */
private fun Routing.staticRoutes() {
    static("/static") {
        resources("static")
    }
}
