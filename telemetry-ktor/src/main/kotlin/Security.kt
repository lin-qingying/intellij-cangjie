package cn.cangnova

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

/**
 * 配置应用程序的安全性
 */
fun Application.configureSecurity() {
    authentication {
        // 用户会话认证
        session<UserSession>("user-session") {
            validate { session ->
                // 简单验证会话是否存在
                if (session.username.isNotBlank()) {
                    UserIdPrincipal(session.username)
                } else {
                    null
                }
            }
            challenge {
                call.sessions.clear<UserSession>()
                call.respondRedirect("/login")
            }
        }
    }
}
