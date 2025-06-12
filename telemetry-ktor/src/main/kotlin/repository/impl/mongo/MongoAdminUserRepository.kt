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

package cn.cangnova.repository.impl.mongo

import cn.cangnova.database.DatabaseFactory
import cn.cangnova.database.MongoDBConfig
import cn.cangnova.model.AdminUser
import cn.cangnova.model.CreateUserRequest
import cn.cangnova.model.UpdateUserRequest
import cn.cangnova.repository.AdminUserRepository
import mu.KotlinLogging
import org.bson.Document
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.UUID

/**
 * MongoDB实现的管理员用户仓库
 */
class MongoAdminUserRepository : AdminUserRepository {
    private val logger = KotlinLogging.logger {}

    /**
     * 初始化仓库，创建默认管理员用户
     */
    suspend fun initialize() {
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

    override suspend fun findByUsername(username: String): AdminUser? {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getAdminUsersCollection()

            val query = Document("username", username)
            val doc = collection.find(query).first()

            return doc.let { documentToAdminUser(it) }
        } catch (e: Exception) {
            logger.error(e) { "查找用户失败: ${e.message}" }
            return null
        }
    }

    override suspend fun createUser(request: CreateUserRequest): AdminUser {
        try {
            val now = Instant.now().toEpochMilli()
            val id = UUID.randomUUID().toString()

            val user = AdminUser(
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

            val doc = Document()
                .append("_id", user._id)
                .append("username", user.username)
                .append("passwordHash", user.passwordHash)
                .append("displayName", user.displayName)
                .append("role", user.role)
                .append("email", user.email)
                .append("createdAt", user.createdAt)
                .append("lastLoginAt", user.lastLoginAt)
                .append("enabled", user.enabled)

            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getAdminUsersCollection()
            collection.insertOne(doc)

            return user
        } catch (e: Exception) {
            logger.error(e) { "创建用户失败: ${e.message}" }
            throw e
        }
    }

    override suspend fun updateUser(username: String, request: UpdateUserRequest): AdminUser? {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getAdminUsersCollection()

            val query = Document("username", username)
            val updates = Document()

            request.displayName?.let { updates.append("displayName", it) }
            request.role?.let { updates.append("role", it) }
            request.email?.let { updates.append("email", it) }
            request.password?.let { updates.append("passwordHash", hashPassword(it)) }
            request.enabled?.let { updates.append("enabled", it) }

            if (updates.isEmpty()) {
                return findByUsername(username)
            }

            val updateDoc = Document("\$set", updates)
            val updateResult = collection.updateOne(query, updateDoc)

            if (updateResult.modifiedCount > 0) {
                return findByUsername(username)
            }

            return null
        } catch (e: Exception) {
            logger.error(e) { "更新用户失败: ${e.message}" }
            return null
        }
    }

    override suspend fun deleteUser(username: String): Boolean {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getAdminUsersCollection()

            val query = Document("username", username)
            val deleteResult = collection.deleteOne(query)

            return deleteResult.deletedCount > 0
        } catch (e: Exception) {
            logger.error(e) { "删除用户失败: ${e.message}" }
            return false
        }
    }

    override suspend fun getAllUsers(page: Int, pageSize: Int): List<AdminUser> {
        val users = mutableListOf<AdminUser>()

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getAdminUsersCollection()

            val skip = (page - 1) * pageSize
            val cursor = collection.find()
                .skip(skip)
                .limit(pageSize)

            cursor.forEach { doc ->
                documentToAdminUser(doc)?.let { users.add(it) }
            }
        } catch (e: Exception) {
            logger.error(e) { "获取用户列表失败: ${e.message}" }
        }

        return users
    }

    override suspend fun getUserCount(): Int {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getAdminUsersCollection()

            return collection.countDocuments().toInt()
        } catch (e: Exception) {
            logger.error(e) { "获取用户数量失败: ${e.message}" }
            return 0
        }
    }

    override suspend fun validateCredentials(username: String, password: String): AdminUser? {
        val user = findByUsername(username) ?: return null

        if (!user.enabled) {
            return null
        }

        return if (verifyPassword(password, user.passwordHash)) user else null
    }

    override suspend fun updateLastLoginTime(username: String): Boolean {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getAdminUsersCollection()

            val query = Document("username", username)
            val updates = Document("\$set", Document("lastLoginAt", Instant.now().toEpochMilli()))

            val updateResult = collection.updateOne(query, updates)

            return updateResult.modifiedCount > 0
        } catch (e: Exception) {
            logger.error(e) { "更新登录时间失败: ${e.message}" }
            return false
        }
    }

    override suspend fun usernameExists(username: String): Boolean {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getAdminUsersCollection()

            val query = Document("username", username)
            return collection.countDocuments(query) > 0
        } catch (e: Exception) {
            logger.error(e) { "检查用户名是否存在失败: ${e.message}" }
            return false
        }
    }

    override fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    override fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }

    /**
     * 将Document转换为AdminUser对象
     */
    private fun documentToAdminUser(doc: Document): AdminUser? {
        return try {
            AdminUser(
                _id = doc.getString("_id"),
                username = doc.getString("username"),
                passwordHash = doc.getString("passwordHash"),
                displayName = doc.getString("displayName"),
                role = doc.getString("role"),
                email = doc.getString("email"),
                createdAt = doc.getLong("createdAt"),
                lastLoginAt = doc.getLong("lastLoginAt"),
                enabled = doc.getBoolean("enabled", true)
            )
        } catch (e: Exception) {
            logger.error(e) { "转换Document到AdminUser失败: ${e.message}" }
            null
        }
    }

    companion object {
        private var instance: MongoAdminUserRepository? = null

        fun getInstance(): MongoAdminUserRepository {
            if (instance == null) {
                instance = MongoAdminUserRepository()
            }
            return instance!!
        }
    }
} 