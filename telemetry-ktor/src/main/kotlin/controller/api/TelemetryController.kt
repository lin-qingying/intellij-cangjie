package cn.cangnova.controller.api

import cn.cangnova.model.TelemetryPayload
import cn.cangnova.model.TelemetryResponse
import cn.cangnova.repository.factory.TelemetryRepositoryFactory
import mu.KotlinLogging
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val logger = KotlinLogging.logger {}

/**
 * 遥测控制器，处理遥测相关的API请求
 * 注意：基本的遥测提交端点不需要JWT认证，因为它们是从插件直接调用的
 */
fun Route.telemetryRoutes() {
    // 接收遥测数据 - 无需认证
    post {
        try {
            val payload = call.receive<TelemetryPayload>()
            
            // 使用仓库工厂获取仓库实例
            val repository = TelemetryRepositoryFactory.getRepository()
            val count = repository.saveTelemetryData(payload)
            
            // 返回成功响应
            call.respond(
                HttpStatusCode.OK,
                TelemetryResponse(
                    success = true,
                    message = "Successfully received telemetry data",
                    count = count
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error processing telemetry data: ${e.message}" }
            
            // 返回错误响应
            call.respond(
                HttpStatusCode.BadRequest,
                TelemetryResponse(
                    success = false,
                    message = "Error processing telemetry data: ${e.message}",
                    count = 0
                )
            )
        }
    }
    
    // 获取遥测状态 - 无需认证
    get {
        call.respond(
            HttpStatusCode.OK,
            TelemetryResponse(
                success = true,
                message = "Telemetry service is running",
                count = 0
            )
        )
    }
    
    // 数据查询API - 需要JWT认证
    route("/events") {
        authenticate("jwt-auth") {
            // 获取最近的遥测事件
            get {
                try {
                    val limit = call.parameters["limit"]?.toIntOrNull() ?: 100
                    
                    // 使用仓库工厂获取仓库实例
                    val repository = TelemetryRepositoryFactory.getRepository()
                    val events = repository.getRecentEvents(limit)
                    
                    call.respond(HttpStatusCode.OK, events)
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving events: ${e.message}" }
                    call.respond(HttpStatusCode.InternalServerError, "Error retrieving events")
                }
            }
        }
    }
    
    route("/metadata") {
        authenticate("jwt-auth") {
            // 获取最近的遥测元数据
            get {
                try {
                    val limit = call.parameters["limit"]?.toIntOrNull() ?: 100
                    
                    // 使用仓库工厂获取仓库实例
                    val repository = TelemetryRepositoryFactory.getRepository()
                    val metadata = repository.getRecentMetadata(limit)
                    
                    call.respond(HttpStatusCode.OK, metadata)
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving metadata: ${e.message}" }
                    call.respond(HttpStatusCode.InternalServerError, "Error retrieving metadata")
                }
            }
        }
    }
} 