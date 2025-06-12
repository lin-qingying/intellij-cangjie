/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.repository.impl.sql

import cn.cangnova.database.DatabaseFactory
import cn.cangnova.model.AdminUser
import cn.cangnova.model.CreateUserRequest
import cn.cangnova.model.UpdateUserRequest
import cn.cangnova.repository.AdminUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.*

/**
 * SQL实现的管理员用户仓库，同时支持MySQL和PostgreSQL
 */
class SQLAdminUserRepository : AdminUserRepository {
    private val logger = KotlinLogging.logger {}
    private val dbType = DatabaseFactory.getConfig().getType()

    // 定义用户表
    object AdminUsers : Table("admin_users") {
        val id = varchar("id", 255)
        val username = varchar("username", 100).uniqueIndex()
        val passwordHash = varchar("password_hash", 255)
        val displayName = varchar("display_name", 100)
        val role = varchar("role", 50)
        val email = varchar("email", 255)
        val createdAt = long("created_at")
        val lastLoginAt = long("last_login_at").nullable()
        val enabled = bool("enabled")

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * 初始化仓库，创建默认管理员用户
     */
    suspend fun initialize() {
        // 确保表已创建
        transaction {
            SchemaUtils.create(AdminUsers)
        }

        // 检查是否存在管理员用户，如果不存在则创建默认管理员
        if (getUserCount() == 0) {
            createUser(
                CreateUserRequest(
                    username = "admin",
                    password = "admin",
                    displayName = "Administrator",
                    role = "ADMIN",
                    email = "admin@example.com"
                )
            )
        }
    }

    override suspend fun findByUsername(username: String): AdminUser? = withContext(Dispatchers.IO) {
        return@withContext transaction {
            AdminUsers.selectAll().where { AdminUsers.username eq username }
                .map { resultRowToAdminUser(it) }
                .singleOrNull()
        }
    }

    override suspend fun createUser(request: CreateUserRequest): AdminUser = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toEpochMilli()

        transaction {
            AdminUsers.insert {
                it[AdminUsers.id] = id
                it[username] = request.username
                it[passwordHash] = hashPassword(request.password)
                it[displayName] = request.displayName
                it[role] = request.role
                it[email] = request.email
                it[createdAt] = now
                it[lastLoginAt] = null
                it[enabled] = true
            }
        }

        return@withContext AdminUser(
            _id = id,
            username = request.username,
            passwordHash = hashPassword(request.password),
            displayName = request.displayName,
            role = request.role,
            email = request.email,
            createdAt = now,
            lastLoginAt = null,
            enabled = true
        )
    }

    override suspend fun updateUser(username: String, request: UpdateUserRequest): AdminUser? =
        withContext(Dispatchers.IO) {
            val user = findByUsername(username) ?: return@withContext null

            transaction {
                val updateStatement = AdminUsers.update({ AdminUsers.username eq username }) {
                    request.displayName?.let { displayName -> it[AdminUsers.displayName] = displayName }
                    request.role?.let { role -> it[AdminUsers.role] = role }
                    request.email?.let { email -> it[AdminUsers.email] = email }
                    request.password?.let { password -> it[passwordHash] = hashPassword(password) }
                    request.enabled?.let { enabled -> it[AdminUsers.enabled] = enabled }
                }

                if (updateStatement == 0) {
                    return@transaction null
                }
            }

            return@withContext findByUsername(username)
        }

    override suspend fun deleteUser(username: String): Boolean = withContext(Dispatchers.IO) {
        val deletedCount = transaction {
            AdminUsers.deleteWhere { AdminUsers.username eq username }
        }

        return@withContext deletedCount > 0
    }

    override suspend fun getAllUsers(page: Int, pageSize: Int): List<AdminUser> = withContext(Dispatchers.IO) {
        return@withContext transaction {
            AdminUsers.selectAll()
                .orderBy(AdminUsers.createdAt to SortOrder.DESC)
                .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                .map { resultRowToAdminUser(it) }
        }
    }

    override suspend fun getUserCount(): Int = withContext(Dispatchers.IO) {
        return@withContext transaction {
            AdminUsers.selectAll().count().toInt()
        }
    }

    override suspend fun validateCredentials(username: String, password: String): AdminUser? =
        withContext(Dispatchers.IO) {
            val user = findByUsername(username) ?: return@withContext null

            if (!user.enabled) {
                return@withContext null
            }

            return@withContext if (verifyPassword(password, user.passwordHash)) user else null
        }

    override suspend fun updateLastLoginTime(username: String): Boolean = withContext(Dispatchers.IO) {
        val now = Instant.now().toEpochMilli()

        val updatedRows = transaction {
            AdminUsers.update({ AdminUsers.username eq username }) {
                it[lastLoginAt] = now
            }
        }

        return@withContext updatedRows > 0
    }

    override suspend fun usernameExists(username: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext transaction {
            AdminUsers.selectAll().where { AdminUsers.username eq username }.count() > 0
        }
    }

    override fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    override fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }

    /**
     * 将数据库结果行转换为AdminUser对象
     */
    private fun resultRowToAdminUser(row: ResultRow): AdminUser {
        return AdminUser(
            _id = row[AdminUsers.id],
            username = row[AdminUsers.username],
            passwordHash = row[AdminUsers.passwordHash],
            displayName = row[AdminUsers.displayName],
            role = row[AdminUsers.role],
            email = row[AdminUsers.email],
            createdAt = row[AdminUsers.createdAt],
            lastLoginAt = row[AdminUsers.lastLoginAt],
            enabled = row[AdminUsers.enabled]
        )
    }

    companion object {
        private var instance: SQLAdminUserRepository? = null

        fun getInstance(): SQLAdminUserRepository {
            if (instance == null) {
                instance = SQLAdminUserRepository()
            }
            return instance!!
        }
    }
} 