package cn.cangnova.controller.web

import cn.cangnova.UserSession
import cn.cangnova.repository.factory.AdminUserRepositoryFactory
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking

/**
 * 配置Web认证
 */
fun Application.configureWebAuth() {
    // 使用工厂获取仓库实例
    val repository = AdminUserRepositoryFactory.getRepository()
    
    install(Authentication) {
        // 表单登录认证
        form("web-auth") {
            userParamName = "username"
            passwordParamName = "password"
            validate { credentials ->
                runBlocking {
                    val user = repository.validateCredentials(credentials.name, credentials.password)
                    if (user != null) {
                        UserIdPrincipal(user.username)
                    } else null
                }
            }
            challenge {
                call.respondRedirect("/web/login?error=invalid_credentials")
            }
        }
        
        // 会话认证
        session<UserSession>("user-session") {
            validate { session ->
                runBlocking {
                    val user = repository.findByUsername(session.username)
                    if (user != null && user.enabled) {
                        UserIdPrincipal(user.username)
                    } else null
                }
            }
            challenge {
                call.respondRedirect("/web/login")
            }
        }
    }
} 