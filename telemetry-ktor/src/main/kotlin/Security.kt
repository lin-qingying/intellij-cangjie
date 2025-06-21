package cn.cangnova

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

/**
 * 配置应用程序的安全性
 */
fun Application.configureSecurity() {
    // 获取JWT配置
    val jwtConfig = getJwtConfig()
    
    authentication {
        // JWT认证
        jwt("jwt-auth") {
            verifier(jwtConfig.verifier)
            realm = "CangJie Telemetry API"
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
