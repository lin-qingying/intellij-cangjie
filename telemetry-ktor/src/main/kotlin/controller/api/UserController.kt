package cn.cangnova.controller.api

import cn.cangnova.JwtConfig
import cn.cangnova.UserSession
import cn.cangnova.getJwtConfig
import cn.cangnova.model.*
import cn.cangnova.repository.factory.AdminUserRepositoryFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import mu.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

// 使用工厂获取仓库实例
private val repository = AdminUserRepositoryFactory.getRepository()

/**
 * 令牌刷新响应
 */
@Serializable
data class TokenRefreshResponse(
    val success: Boolean,
    val token: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val message: String? = null
)

/**
 * 用户认证 API 路由
 */
fun Route.authApiRoutes() {
    // 登录API
    post("/login") {
        try {
            val request = call.receive<LoginRequest>()

            val user = repository.validateCredentials(request.username, request.password)

            if (user != null) {
                // 更新最后登录时间
                repository.updateLastLoginTime(user.username)

                // 获取JWT配置
                val jwtConfig = call.application.getJwtConfig()
                
                // 生成JWT令牌
                val token = jwtConfig.generateToken(user.username, user.role)
                val refreshToken = jwtConfig.generateRefreshToken(user.username)

                // 设置会话（向后兼容）
                call.sessions.set(UserSession(user.username))

                // 创建响应对象
                val response = mapOf(
                    "success" to true,
                    "message" to "登录成功",
                    "user" to AdminUserDto.fromAdminUser(user),
                    "token" to token,
                    "refreshToken" to refreshToken,
                    "expiresIn" to 3600
                )

                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "success" to false,
                        "message" to "用户名或密码错误"
                    )
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "登录失败: ${e.message}" }
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "success" to false,
                    "message" to "登录失败: ${e.message}"
                )
            )
        }
    }

    // 登出API
    post("/logout") {
        try {
            call.sessions.clear<UserSession>()
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "success" to true,
                    "message" to "已成功登出"
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "登出失败: ${e.message}" }
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "success" to false,
                    "message" to "登出失败: ${e.message}"
                )
            )
        }
    }
    
    // 刷新令牌API
    post("/refresh") {
        try {
            val request = call.receive<RefreshTokenRequest>()
            val refreshToken = request.refreshToken
            
            // 验证刷新令牌
            val jwtConfig = call.application.getJwtConfig()
            
            try {
                val jwt = jwtConfig.verifier.verify(refreshToken)
                val username = jwt.getClaim("username").asString()
                
                // 检查用户是否存在
                val user = repository.findByUsername(username)
                
                if (user != null) {
                    // 生成新的访问令牌
                    val newToken = jwtConfig.generateToken(user.username, user.role)
                    val newRefreshToken = jwtConfig.generateRefreshToken(user.username)
                    
                    call.respond(
                        HttpStatusCode.OK,
                        TokenRefreshResponse(
                            success = true,
                            token = newToken,
                            refreshToken = newRefreshToken,
                            expiresIn = 3600
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        TokenRefreshResponse(
                            success = false,
                            message = "无效的刷新令牌"
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "刷新令牌验证失败: ${e.message}" }
                call.respond(
                    HttpStatusCode.Unauthorized,
                    TokenRefreshResponse(
                        success = false,
                        message = "无效的刷新令牌"
                    )
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "刷新令牌失败: ${e.message}" }
            call.respond(
                HttpStatusCode.InternalServerError,
                TokenRefreshResponse(
                    success = false,
                    message = "刷新令牌失败"
                )
            )
        }
    }

    // 获取当前用户信息
    authenticate("jwt-auth") {
        get("/me") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val username = principal?.payload?.getClaim("username")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val user = repository.findByUsername(username) ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(AdminUserDto.fromAdminUser(user))
            } catch (e: Exception) {
                logger.error(e) { "获取当前用户信息失败: ${e.message}" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "获取当前用户信息失败")
                )
            }
        }
    }
}

/**
 * 用户管理 API 路由
 */
fun Route.usersApiRoutes() {
    // 需要用户认证
    authenticate("jwt-auth") {
        // 获取所有用户
        get {
            try {
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20

                val users = repository.getAllUsers(page, pageSize)
                val totalCount = repository.getUserCount()

                call.respond(
                    mapOf(
                        "users" to users.map { AdminUserDto.fromAdminUser(it) },
                        "totalCount" to totalCount,
                        "page" to page,
                        "pageSize" to pageSize
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "获取用户列表失败: ${e.message}" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "获取用户列表失败"))
            }
        }

        // 创建用户
        post {
            try {
                val request = call.receive<CreateUserRequest>()

                // 检查用户名是否已存在
                if (repository.usernameExists(request.username)) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "用户名已存在"))
                    return@post
                }

                val user = repository.createUser(request)
                call.respond(HttpStatusCode.Created, AdminUserDto.fromAdminUser(user))
            } catch (e: Exception) {
                logger.error(e) { "创建用户失败: ${e.message}" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "创建用户失败"))
            }
        }

        // 获取单个用户
        get("/{username}") {
            try {
                val username = call.parameters["username"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "用户名不能为空")
                )

                val user = repository.findByUsername(username) ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "用户不存在")
                )

                call.respond(AdminUserDto.fromAdminUser(user))
            } catch (e: Exception) {
                logger.error(e) { "获取用户失败: ${e.message}" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "获取用户失败"))
            }
        }

        // 更新用户
        put("/{username}") {
            try {
                val username = call.parameters["username"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "用户名不能为空")
                )
                val request = call.receive<UpdateUserRequest>()

                val updatedUser = repository.updateUser(username, request) ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "用户不存在")
                )

                call.respond(AdminUserDto.fromAdminUser(updatedUser))
            } catch (e: Exception) {
                logger.error(e) { "更新用户失败: ${e.message}" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "更新用户失败"))
            }
        }

        // 删除用户
        delete("/{username}") {
            try {
                val username = call.parameters["username"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "用户名不能为空")
                )

                // 不允许删除root用户
                if (username == "root") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "不能删除管理员账户"))
                    return@delete
                }

                val deleted = repository.deleteUser(username)

                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "用户不存在"))
                }
            } catch (e: Exception) {
                logger.error(e) { "删除用户失败: ${e.message}" }
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "删除用户失败"))
            }
        }
    }
} 