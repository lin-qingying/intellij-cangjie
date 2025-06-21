package cn.cangnova

import cn.cangnova.controller.api.analyticsApiRoutes
import cn.cangnova.controller.api.dashboardApiRoutes
import cn.cangnova.controller.api.eventsApiRoutes
import cn.cangnova.controller.api.metadataApiRoutes
import cn.cangnova.controller.api.reportsApiRoutes
import cn.cangnova.controller.api.authApiRoutes
import cn.cangnova.controller.api.usersApiRoutes
import cn.cangnova.controller.api.telemetryRoutes
import cn.cangnova.controller.web.webRoutes
import cn.cangnova.controller.web.privacyRoutes
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
        val session = call.sessions.get<UserSession>()
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
    // Web 界面路由
    webRoutes()
}

/**
 * API 路由配置 (JSON 响应)
 */
private fun Routing.apiRoutes() {
    route("/api") {
        // 遥测数据收集 API
        route("/telemetry") {
            telemetryRoutes()
        }

        // 数据分析和管理 API
        route("/v1") {
            // 用户认证和管理
            route("/auth") {
                authApiRoutes()
            }

            // 用户管理
            route("/users") {
                usersApiRoutes()
            }

            // 仪表盘数据
            route("/dashboard") {
                dashboardApiRoutes()
            }

            // 事件数据
            route("/events") {
                eventsApiRoutes()
            }

            // 元数据
            route("/metadata") {
                metadataApiRoutes()
            }

            // 报表数据
            route("/reports") {
                reportsApiRoutes()
            }
            // 数据分析
            route("/analytics") {
                analyticsApiRoutes()
            }
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
