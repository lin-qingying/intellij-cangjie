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

import cn.cangnova.model.SystemSettings
import cn.cangnova.repository.SystemSettingsRepository
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 基于SQL的系统设置仓库实现
 * 适用于MySQL和PostgreSQL
 */
object SQLSystemSettingsRepository : SystemSettingsRepository {
    private val logger = KotlinLogging.logger {}
    
    // 默认设置ID
    private const val defaultSettingsId = "settings"
    
    /**
     * 系统设置表定义
     */
    object SystemSettingsTable : Table("system_settings") {
        val id = varchar("id", 255)
        val dataRetentionDays = integer("data_retention_days").default(365)
        val cleanupSchedule = varchar("cleanup_schedule", 50).default("weekly")
        val enableAutoCleanup = bool("enable_auto_cleanup").default(false)
        val backupSchedule = varchar("backup_schedule", 50).default("weekly")
        val backupRetention = integer("backup_retention").default(5)
        val enableAutoBackup = bool("enable_auto_backup").default(false)
        val eventBatchSize = integer("event_batch_size").default(100)
        val connectionPoolSize = integer("connection_pool_size").default(10)
        val logLevel = varchar("log_level", 20).default("INFO")
        val logRetention = integer("log_retention").default(30)
        val enableCache = bool("enable_cache").default(true)
        val cacheSize = integer("cache_size").default(256)
        val cacheExpiry = integer("cache_expiry").default(30)
        val lastUpdated = long("last_updated")
        
        override val primaryKey = PrimaryKey(id)
        
        /**
         * 初始化表结构
         */
        fun initTable() {
            transaction {
                SchemaUtils.create(this@SystemSettingsTable)
                
                // 检查是否存在默认设置，如果不存在则创建
                val count = SystemSettingsTable.selectAll().where { SystemSettingsTable.id eq defaultSettingsId }.count().toInt()
                if (count == 0) {
                    SystemSettingsTable.insert {
                        it[id] = defaultSettingsId
                        it[dataRetentionDays] = 365
                        it[cleanupSchedule] = "weekly"
                        it[enableAutoCleanup] = false
                        it[backupSchedule] = "weekly"
                        it[backupRetention] = 5
                        it[enableAutoBackup] = false
                        it[eventBatchSize] = 100
                        it[connectionPoolSize] = 10
                        it[logLevel] = "INFO"
                        it[logRetention] = 30
                        it[enableCache] = true
                        it[cacheSize] = 256
                        it[cacheExpiry] = 30
                        it[lastUpdated] = System.currentTimeMillis()
                    }
                }
            }
        }
    }
    
    /**
     * 初始化仓库
     */
    fun initialize() {
        try {
            SystemSettingsTable.initTable()
            logger.info { "SQL系统设置表初始化完成" }
        } catch (e: Exception) {
            logger.error(e) { "SQL系统设置表初始化失败: ${e.message}" }
        }
    }
    
    /**
     * 获取系统设置
     * @return 系统设置对象
     */
    override suspend fun getSettings(): SystemSettings {
        return try {
            transaction {
                val row = SystemSettingsTable.selectAll().where { SystemSettingsTable.id eq defaultSettingsId }.firstOrNull()
                
                if (row != null) {
                    rowToSystemSettings(row)
                } else {
                    // 如果没有找到设置，创建默认设置
                    val defaultSettings = createDefaultSettings()
                    saveSettings(defaultSettings)
                    defaultSettings
                }
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
     * 将设置保存到数据库
     */
    private fun saveSettings(settings: SystemSettings) {
        transaction {
            SystemSettingsTable.update({ SystemSettingsTable.id eq settings._id }) {
                it[dataRetentionDays] = settings.dataRetentionDays
                it[cleanupSchedule] = settings.cleanupSchedule
                it[enableAutoCleanup] = settings.enableAutoCleanup
                it[backupSchedule] = settings.backupSchedule
                it[backupRetention] = settings.backupRetention
                it[enableAutoBackup] = settings.enableAutoBackup
                it[eventBatchSize] = settings.eventBatchSize
                it[connectionPoolSize] = settings.connectionPoolSize
                it[logLevel] = settings.logLevel
                it[logRetention] = settings.logRetention
                it[enableCache] = settings.enableCache
                it[cacheSize] = settings.cacheSize
                it[cacheExpiry] = settings.cacheExpiry
                it[lastUpdated] = settings.lastUpdated
            }
        }
    }
    
    /**
     * 将数据库行转换为SystemSettings对象
     */
    private fun rowToSystemSettings(row: ResultRow): SystemSettings {
        return SystemSettings(
            _id = row[SystemSettingsTable.id],
            dataRetentionDays = row[SystemSettingsTable.dataRetentionDays],
            cleanupSchedule = row[SystemSettingsTable.cleanupSchedule],
            enableAutoCleanup = row[SystemSettingsTable.enableAutoCleanup],
            backupSchedule = row[SystemSettingsTable.backupSchedule],
            backupRetention = row[SystemSettingsTable.backupRetention],
            enableAutoBackup = row[SystemSettingsTable.enableAutoBackup],
            eventBatchSize = row[SystemSettingsTable.eventBatchSize],
            connectionPoolSize = row[SystemSettingsTable.connectionPoolSize],
            logLevel = row[SystemSettingsTable.logLevel],
            logRetention = row[SystemSettingsTable.logRetention],
            enableCache = row[SystemSettingsTable.enableCache],
            cacheSize = row[SystemSettingsTable.cacheSize],
            cacheExpiry = row[SystemSettingsTable.cacheExpiry],
            lastUpdated = row[SystemSettingsTable.lastUpdated]
        )
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