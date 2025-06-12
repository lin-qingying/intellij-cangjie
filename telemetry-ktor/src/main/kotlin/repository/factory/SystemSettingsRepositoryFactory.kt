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

package cn.cangnova.repository.factory

import cn.cangnova.database.DatabaseFactory
import cn.cangnova.database.DatabaseType
import cn.cangnova.repository.SystemSettingsRepository
import cn.cangnova.repository.impl.mongo.MongoSystemSettingsRepository
import cn.cangnova.repository.impl.sql.SQLSystemSettingsRepository
import mu.KotlinLogging

/**
 * 系统设置仓库工厂类
 */
object SystemSettingsRepositoryFactory {
    private val logger = KotlinLogging.logger {}
    private var repository: SystemSettingsRepository? = null
    
    /**
     * 获取系统设置仓库实例
     */
    fun getRepository(): SystemSettingsRepository {
        if (repository == null) {
            try {
                val dbType = DatabaseFactory.getConfig().getType()
                logger.info { "Creating settings repository for database type: ${dbType.name}" }
                
                repository = when (dbType) {
                    DatabaseType.MONGODB -> MongoSystemSettingsRepository
                    DatabaseType.MYSQL, DatabaseType.POSTGRESQL -> {
                        // 使用SQL实现
                        SQLSystemSettingsRepository.initialize()
                        SQLSystemSettingsRepository
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "获取数据库类型失败，使用MongoDB作为默认数据库: ${e.message}" }
                repository = MongoSystemSettingsRepository
            }
        }
        
        return repository!!
    }
    
    /**
     * 获取当前使用的数据库类型
     * @return 数据库类型名称
     */
    fun getDatabaseType(): String {
        return try {
            DatabaseFactory.getConfig().getType().name
        } catch (e: Exception) {
            "MONGODB (默认)"
        }
    }
} 