package cn.cangnova

import cn.cangnova.controller.web.configureAdminAuth
import cn.cangnova.controller.api.AdminUserController
import cn.cangnova.database.DatabaseFactory
import cn.cangnova.repository.factory.SystemSettingsRepositoryFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.freemarker.*
import io.ktor.server.sessions.*
import freemarker.cache.ClassTemplateLoader
import io.ktor.http.*
import io.ktor.server.resources.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

fun Application.module() {
    // 初始化数据库
    DatabaseFactory.init(this)
    
    // 初始化管理员用户仓库
    launch {
        AdminUserController.initialize()
    }
    
    // 初始化系统设置仓库
    launch {
        SystemSettingsRepositoryFactory.getRepository()
    }
    
    // 配置内容协商
    install(ContentNegotiation) {
        json()
    }
    
    // 配置CORS
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }
    
    // 配置FreeMarker模板引擎
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    
    // 配置会话
    install(Sessions) {
        cookie<AdminSession>("ADMIN_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600 // 1小时
            cookie.secure = false // 开发环境可以设为false，生产环境应设为true
            cookie.httpOnly = true
        }
    }
    
    // 配置资源路由
    install(Resources)
    
    // 配置自动HEAD响应
    install(AutoHeadResponse)
    
    // 配置状态页面
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respondText(text = "404: Page Not Found", status = HttpStatusCode.NotFound)
        }
    }
    
    // 配置认证
    configureAdminAuth()
    
    // 配置路由
    configureRouting()
    
    // 在应用程序关闭时清理资源
    environment.monitor.subscribe(ApplicationStopped) {
        DatabaseFactory.close()
    }
}

// 管理员会话数据类
@Serializable
data class AdminSession(val username: String)
