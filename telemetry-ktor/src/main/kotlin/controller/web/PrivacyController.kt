package cn.cangnova.controller.web

import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

/**
 * 隐私政策控制器，处理隐私政策相关的路由
 */
object PrivacyController {
    
    /**
     * 注册隐私政策路由
     */
    fun Route.privacyRoutes() {
        // 隐私政策页面
        get("/privacy-policy") {
            val model = mapOf(
                "lastUpdated" to "2025-6-12",
                "currentYear" to LocalDate.now().year.toString()
            )
            call.respond(FreeMarkerContent("privacy_policy.ftl", model))
        }
    }
} 