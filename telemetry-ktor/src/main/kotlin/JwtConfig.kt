package cn.cangnova

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

/**
 * JWT配置类，用于处理JWT令牌的生成和验证
 */
class JwtConfig(private val secret: String, private val issuer: String, private val audience: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    
    // 用于验证JWT令牌的验证器
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()
    
    /**
     * 生成JWT令牌
     * @param username 用户名
     * @param role 用户角色
     * @param expiresInMinutes 令牌过期时间（分钟）
     * @return JWT令牌字符串
     */
    fun generateToken(username: String, role: String, expiresInMinutes: Long = 60): String {
        return JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("username", username)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInMinutes * 60 * 1000))
            .sign(algorithm)
    }
    
    /**
     * 生成刷新令牌
     * @param username 用户名
     * @param expiresInDays 令牌过期时间（天）
     * @return 刷新令牌字符串
     */
    fun generateRefreshToken(username: String, expiresInDays: Long = 7): String {
        return JWT.create()
            .withSubject("Refresh")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInDays * 24 * 60 * 60 * 1000))
            .sign(algorithm)
    }
}

/**
 * 从应用程序配置中获取JWT配置
 */
fun Application.getJwtConfig(): JwtConfig {
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    
    return JwtConfig(secret, issuer, audience)
}

/**
 * 配置JWT认证
 */
fun Application.configureJwtAuth() {
    val jwtConfig = getJwtConfig()
    
    authentication {
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