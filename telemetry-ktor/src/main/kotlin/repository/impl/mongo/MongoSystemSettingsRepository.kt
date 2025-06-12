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

import cn.cangnova.model.SystemSettings
import cn.cangnova.database.DatabaseFactory
import cn.cangnova.database.MongoDBConfig
import cn.cangnova.repository.SystemSettingsRepository
import com.mongodb.kotlin.client.MongoCollection
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import org.bson.types.ObjectId
import mu.KotlinLogging

/**
 * MongoDB系统设置仓库实现
 */
object MongoSystemSettingsRepository : SystemSettingsRepository {
    private val logger = KotlinLogging.logger {}
    
    // 默认设置ID，使用固定ID便于查找
    private const val defaultSettingsId = "settings"
    
    /**
     * 获取系统设置集合
     */
    private fun getSettingsCollection(): MongoCollection<Document> {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            return mongoConfig.getSettingsCollection()
        } catch (e: Exception) {
            logger.error(e) { "获取系统设置集合失败: ${e.message}" }
            throw e
        }
    }
    
    /**
     * 获取系统设置
     * @return 系统设置对象
     */
    override suspend fun getSettings(): SystemSettings {
        return try {
            // 尝试获取现有设置
            val collection = getSettingsCollection()
            val doc = collection.find(Document("_id", defaultSettingsId)).firstOrNull()
            
            // 如果不存在，创建默认设置
            if (doc != null) {
                documentToSystemSettings(doc)
            } else {
                val defaultSettings = createDefaultSettings()
                saveSettings(defaultSettings)
                defaultSettings
            }
        } catch (e: Exception) {
            logger.error(e) { "获取系统设置失败: ${e.message}" }
            createDefaultSettings()
        }
    }
    
    /**
     * 更新系统设置
     * @param settings 要更新的系统设置对象
     * @return 更新后的系统设置对象
     */
    override suspend fun updateSettings(settings: SystemSettings): SystemSettings {
        return try {
            // 创建一个新的设置对象，确保ID不变
            val updatedSettings = settings.copy(
                _id = defaultSettingsId,
                lastUpdated = System.currentTimeMillis()
            )
            
            // 保存设置
            saveSettings(updatedSettings)
            
            updatedSettings
        } catch (e: Exception) {
            logger.error(e) { "更新系统设置失败: ${e.message}" }
            settings
        }
    }
    
    /**
     * 重置系统设置为默认值
     * @return 重置后的系统设置对象
     */
    override suspend fun resetSettings(): SystemSettings {
        return try {
            val defaultSettings = createDefaultSettings()
            
            // 保存默认设置
            saveSettings(defaultSettings)
            
            defaultSettings
        } catch (e: Exception) {
            logger.error(e) { "重置系统设置失败: ${e.message}" }
            createDefaultSettings()
        }
    }
    
    /**
     * 保存系统设置到数据库
     */
    private fun saveSettings(settings: SystemSettings) {
        val collection = getSettingsCollection()
        val doc = systemSettingsToDocument(settings)
        
        collection.replaceOne(
            Document("_id", settings._id),
            doc,
            ReplaceOptions().upsert(true)
        )
    }
    
    /**
     * 将Document对象转换为SystemSettings对象
     */
    private fun documentToSystemSettings(doc: Document): SystemSettings {
        return SystemSettings(
            _id = doc.getString("_id") ?: ObjectId().toString(),
            dataRetentionDays = doc.getInteger("dataRetentionDays", 365),
            cleanupSchedule = doc.getString("cleanupSchedule") ?: "weekly",
            enableAutoCleanup = doc.getBoolean("enableAutoCleanup") ?: false,
            backupSchedule = doc.getString("backupSchedule") ?: "weekly",
            backupRetention = doc.getInteger("backupRetention", 5),
            enableAutoBackup = doc.getBoolean("enableAutoBackup") ?: false,
            eventBatchSize = doc.getInteger("eventBatchSize", 100),
            connectionPoolSize = doc.getInteger("connectionPoolSize", 10),
            logLevel = doc.getString("logLevel") ?: "INFO",
            logRetention = doc.getInteger("logRetention", 30),
            enableCache = doc.getBoolean("enableCache") ?: true,
            cacheSize = doc.getInteger("cacheSize", 256),
            cacheExpiry = doc.getInteger("cacheExpiry", 30),
            lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
        )
    }
    
    /**
     * 将SystemSettings对象转换为Document对象
     */
    private fun systemSettingsToDocument(settings: SystemSettings): Document {
        return Document()
            .append("_id", settings._id)
            .append("dataRetentionDays", settings.dataRetentionDays)
            .append("cleanupSchedule", settings.cleanupSchedule)
            .append("enableAutoCleanup", settings.enableAutoCleanup)
            .append("backupSchedule", settings.backupSchedule)
            .append("backupRetention", settings.backupRetention)
            .append("enableAutoBackup", settings.enableAutoBackup)
            .append("eventBatchSize", settings.eventBatchSize)
            .append("connectionPoolSize", settings.connectionPoolSize)
            .append("logLevel", settings.logLevel)
            .append("logRetention", settings.logRetention)
            .append("enableCache", settings.enableCache)
            .append("cacheSize", settings.cacheSize)
            .append("cacheExpiry", settings.cacheExpiry)
            .append("lastUpdated", settings.lastUpdated)
    }
    
    /**
     * 创建默认设置
     */
    private fun createDefaultSettings(): SystemSettings {
        return SystemSettings(
            _id = defaultSettingsId,
            dataRetentionDays = 365,
            cleanupSchedule = "weekly",
            enableAutoCleanup = false,
            backupSchedule = "weekly",
            backupRetention = 5,
            enableAutoBackup = false,
            eventBatchSize = 100,
            connectionPoolSize = 10,
            logLevel = "INFO",
            logRetention = 30,
            enableCache = true,
            cacheSize = 256,
            cacheExpiry = 30,
            lastUpdated = System.currentTimeMillis()
        )
    }
} 