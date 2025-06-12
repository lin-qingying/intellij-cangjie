package cn.cangnova.controller.api

import cn.cangnova.AdminSession
import cn.cangnova.model.*
import cn.cangnova.repository.factory.AdminUserRepositoryFactory
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import mu.KotlinLogging

/**
 * 管理员用户控制器，处理用户管理API
 */
object AdminUserController {
    private val logger = KotlinLogging.logger {}

    // 使用工厂获取仓库实例
    private val repository = AdminUserRepositoryFactory.getRepository()

    /**
     * 初始化仓库
     */
    suspend fun initialize() {
        AdminUserRepositoryFactory.initialize()
    }

    /**
     * 注册用户管理路由
     */
    fun Route.adminUserRoutes() {
        // 用户管理 API
        userManagementApi()

        // 用户认证 API
        authenticationApi()
    }

    /**
     * 用户管理 API 路由
     */
    private fun Route.userManagementApi() {
        route("/users") {
            // 需要管理员认证
            authenticate("admin-session") {
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

                        // 不允许删除admin用户
                        if (username == "admin") {
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

        // 获取当前用户信息
        authenticate("admin-session") {
            get("/me") {
                try {
                    val principal =
                        call.principal<UserIdPrincipal>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val username = principal.name

                    val user = repository.findByUsername(username) ?: return@get call.respond(HttpStatusCode.NotFound)

                    call.respond(AdminUserDto.fromAdminUser(user))
                } catch (e: Exception) {
                    logger.error(e) { "获取当前用户信息失败: ${e.message}" }
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "获取当前用户信息失败"))
                }
            }
        }
    }

    /**
     * 用户认证 API 路由
     */
    private fun Route.authenticationApi() {
        // 登录API
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()

                val user = repository.validateCredentials(request.username, request.password)

                if (user != null) {
                    // 更新最后登录时间
                    repository.updateLastLoginTime(user.username)

                    // 设置会话
                    call.sessions.set(AdminSession(user.username))

                    call.respond(
                        LoginResponse(
                            success = true,
                            message = "登录成功",
                            user = AdminUserDto.fromAdminUser(user)
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        LoginResponse(
                            success = false,
                            message = "用户名或密码错误"
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "登录失败: ${e.message}" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    LoginResponse(
                        success = false,
                        message = "登录失败: ${e.message}"
                    )
                )
            }
        }

        // 退出登录
        post("/logout") {
            call.sessions.clear<AdminSession>()
            call.respond(mapOf("success" to true))
        }
    }
} 