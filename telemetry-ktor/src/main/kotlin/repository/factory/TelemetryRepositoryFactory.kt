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
import cn.cangnova.repository.TelemetryRepository
import cn.cangnova.repository.impl.mongo.MongoTelemetryRepository
import cn.cangnova.repository.impl.sql.SQLTelemetryRepository
import mu.KotlinLogging

/**
 * 遥测仓库工厂类，根据数据库类型创建对应的仓库实例
 */
object TelemetryRepositoryFactory {
    private val logger = KotlinLogging.logger {}
    
    /**
     * 获取遥测仓库实例
     * @return 遥测仓库实例
     */
    fun getRepository(): TelemetryRepository {
        try {
            val dbType = DatabaseFactory.getConfig().getType()
            logger.info { "Creating repository for database type: ${dbType.name}" }
            
            return when (dbType) {
                DatabaseType.MONGODB -> MongoTelemetryRepository
                DatabaseType.MYSQL, DatabaseType.POSTGRESQL -> SQLTelemetryRepository
            }
        } catch (e: Exception) {
            logger.error(e) { "获取数据库类型失败，使用MongoDB作为默认数据库: ${e.message}" }
            return MongoTelemetryRepository
        }
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