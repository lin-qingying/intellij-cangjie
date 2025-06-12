package cn.cangnova.controller.web

import cn.cangnova.AdminSession
import cn.cangnova.repository.factory.AdminUserRepositoryFactory
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking

/**
 * 管理员用户信息
 */
data class AdminUser(
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val role: String
)

/**
 * 管理员用户仓库
 */
//object AdminUserRepository {
//    // 在实际应用中，应从数据库获取用户信息
//    // 这里简单使用内存中的用户列表
//    private val users = mutableMapOf<String, AdminUser>()
//
//    init {
//        // 添加默认管理员用户，密码为"admin"
//        val defaultAdmin = AdminUser(
//            username = "admin",
//            // 在实际应用中应使用更安全的密码哈希算法
//            passwordHash = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", // SHA-256("admin")
//            displayName = "Administrator",
//            role = "ADMIN"
//        )
//        users[defaultAdmin.username] = defaultAdmin
//    }
//
//    fun findUserByUsername(username: String): AdminUser? {
//        return users[username]
//    }
//
//    fun validateUser(username: String, password: String): Boolean {
//        val user = findUserByUsername(username) ?: return false
//        // 在实际应用中应使用更安全的密码验证方法
//        val passwordHash = sha256(password)
//        return user.passwordHash == passwordHash
//    }
//
//    private fun sha256(str: String): String {
//        val bytes = str.toByteArray()
//        val md = java.security.MessageDigest.getInstance("SHA-256")
//        val digest = md.digest(bytes)
//        return digest.fold("") { str, it -> str + "%02x".format(it) }
//    }
//}

/**
 * 配置管理员认证
 */
fun Application.configureAdminAuth() {
    // 使用工厂获取仓库实例
    val repository = AdminUserRepositoryFactory.getRepository()
    
    install(Authentication) {
        // 表单登录认证
        form("admin-auth") {
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
        session<AdminSession>("admin-session") {
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