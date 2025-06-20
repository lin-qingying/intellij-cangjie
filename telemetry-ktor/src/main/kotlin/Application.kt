package cn.cangnova

import cn.cangnova.controller.web.configureWebAuth
import cn.cangnova.database.DatabaseFactory
import cn.cangnova.repository.factory.SystemSettingsRepositoryFactory
import cn.cangnova.serialization.AnySerializer
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

import cn.cangnova.repository.factory.AdminUserRepositoryFactory
import cn.cangnova.repository.factory.TelemetryRepositoryFactory
import kotlinx.coroutines.runBlocking

fun Application.module() {
    // 初始化数据库
    DatabaseFactory.init(this)
    
    // 初始化数据库存储库
    configureDatabase()
    

    
    // 配置内容协商
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            serializersModule = SerializersModule {
                contextual(AnySerializer)
            }
        })
    }
    // 配置CORS
    install(CORS) {
        allowHost("localhost:5173") // React开发服务器默认端口
        allowHost("localhost:4173") // React生产预览服务器端口
        allowHost("localhost:8080") // Backend server port
        allowHost("localhost:3000") // Another common development port
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    
    // 配置FreeMarker模板引擎
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    
    // 配置会话
    install(Sessions) {
        cookie<UserSession>("USER_SESSION") {
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
    configureWebAuth()
    
    // 配置路由
    configureRouting()
    
    // 在应用程序关闭时清理资源
    environment.monitor.subscribe(ApplicationStopped) {
        DatabaseFactory.close()
    }
}

fun Application.configureDatabase() {
    // 初始化数据库连接
    runBlocking {
        // 初始化用户仓库
        AdminUserRepositoryFactory.initialize()
        
        // 初始化遥测仓库
        TelemetryRepositoryFactory.getRepository()
        
        // 初始化系统设置仓库
        SystemSettingsRepositoryFactory.getRepository()
    }
}

/**
 * 用户会话数据类
 */
@Serializable
data class UserSession(val username: String)
