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

import cn.cangnova.database.MySQLConfig
import cn.cangnova.model.TelemetryEvent
import cn.cangnova.model.TelemetryMetadata
import cn.cangnova.model.TelemetryPayload
import cn.cangnova.repository.TelemetryRepository
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*

/**
 * 基于SQL的遥测数据仓库，负责存储遥测数据
 * 适用于MySQL和PostgreSQL
 */
object SQLTelemetryRepository : TelemetryRepository {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 保存遥测数据
     * @param payload 遥测数据载荷
     * @return 保存的事件数量
     */
    override fun saveTelemetryData(payload: TelemetryPayload): Int {
        return try {
            transaction {
                // 保存元数据
                val metadata = payload.metadata
                val metadataId = MySQLConfig.TelemetryMetadata.insert {
                    it[id] = metadata._id
                    it[pluginVersion] = metadata.pluginVersion
                    it[ideVersion] = metadata.ideVersion
                    it[ideBuild] = metadata.ideBuild
                    it[os] = metadata.os
                    it[systemId] = metadata.systemId
                    it[osVersion] = metadata.osVersion
                    it[javaVersion] = metadata.javaVersion
                    it[receivedTimestamp] = Instant.parse(metadata.receivedTimestamp)
                    it[clientTimestamp] = metadata.timestamp
                } get MySQLConfig.TelemetryMetadata.id

                // 保存事件
                var count = 0
                payload.events.forEach { event ->
                    try {
                        MySQLConfig.TelemetryEvents.insert {
                            it[id] = event._id
                            it[eventId] = event.id
                            it[category] = event.category
                            it[name] = event.name
                            it[value] = event.value
                            it[timestamp] = Instant.parse(event.timestamp)
                            it[properties] = json.encodeToString(event.properties)
                            it[MySQLConfig.TelemetryEvents.metadataId] = metadataId
                        }
                        count++
                    } catch (e: Exception) {
                        logger.error(e) { "Error saving event ${event.id}: ${e.message}" }
                    }
                }

                count
            }
        } catch (e: Exception) {
            logger.error(e) { "Error saving telemetry data: ${e.message}" }
            0
        }
    }

    /**
     * 获取最近的遥测事件
     * @param limit 限制返回的事件数量
     * @return 遥测事件列表
     */
    override fun getRecentEvents(limit: Int): List<TelemetryEvent> {
        return try {
            transaction {
                MySQLConfig.TelemetryEvents
                    .selectAll()
                    .orderBy(MySQLConfig.TelemetryEvents.timestamp to SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        TelemetryEvent(
                            _id = row[MySQLConfig.TelemetryEvents.id],
                            id = row[MySQLConfig.TelemetryEvents.eventId],
                            category = row[MySQLConfig.TelemetryEvents.category],
                            name = row[MySQLConfig.TelemetryEvents.name],
                            value = row[MySQLConfig.TelemetryEvents.value],
                            timestamp = row[MySQLConfig.TelemetryEvents.timestamp].toString(),
                            properties = try {
                                json.decodeFromString<Map<String, String>>(row[MySQLConfig.TelemetryEvents.properties])
                            } catch (e: Exception) {
                                emptyMap()
                            },
                            metadataId = row[MySQLConfig.TelemetryEvents.metadataId]
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving recent events: ${e.message}" }
            emptyList()
        }
    }

    /**
     * 获取最近的遥测元数据
     * @param limit 限制返回的元数据数量
     * @return 遥测元数据列表
     */
    override fun getRecentMetadata(limit: Int): List<TelemetryMetadata> {
        return try {
            transaction {
                MySQLConfig.TelemetryMetadata
                    .selectAll()
                    .orderBy(MySQLConfig.TelemetryMetadata.receivedTimestamp to SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        TelemetryMetadata(
                            _id = row[MySQLConfig.TelemetryMetadata.id],
                            systemId = row[MySQLConfig.TelemetryMetadata.systemId],
                            pluginVersion = row[MySQLConfig.TelemetryMetadata.pluginVersion],
                            ideVersion = row[MySQLConfig.TelemetryMetadata.ideVersion],
                            ideBuild = row[MySQLConfig.TelemetryMetadata.ideBuild],
                            os = row[MySQLConfig.TelemetryMetadata.os],
                            osVersion = row[MySQLConfig.TelemetryMetadata.osVersion],
                            javaVersion = row[MySQLConfig.TelemetryMetadata.javaVersion],
                            timestamp = row[MySQLConfig.TelemetryMetadata.clientTimestamp],
                            receivedTimestamp = row[MySQLConfig.TelemetryMetadata.receivedTimestamp].toString()
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving recent metadata: ${e.message}" }
            emptyList()
        }
    }

    /**
     * 获取遥测事件总数
     * @param category 可选的事件类别过滤
     * @return 事件总数
     */
    override fun getTotalEventsCount(category: String?): Int {
        return try {
            transaction {
                if (category != null) {
                    MySQLConfig.TelemetryEvents
                        .selectAll().where { MySQLConfig.TelemetryEvents.category eq category }
                        .count()
                } else {
                    MySQLConfig.TelemetryEvents.selectAll().count()
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting total events count: ${e.message}" }
            0
        }.toInt()
    }

    /**
     * 获取遥测元数据总数
     * @return 元数据总数
     */
    override fun getTotalMetadataCount(): Int {
        return try {
            transaction {
                MySQLConfig.TelemetryMetadata.selectAll().count()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting total metadata count: ${e.message}" }
            0
        }.toInt()
    }

    /**
     * 获取分页的遥测事件
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @param category 可选的事件类别过滤
     * @return 遥测事件列表
     */
    override fun getEvents(page: Int, pageSize: Int, category: String?): List<TelemetryEvent> {
        return try {
            transaction {
                val offset = (page - 1) * pageSize

                val query = if (category != null) {
                    MySQLConfig.TelemetryEvents
                        .selectAll().where { MySQLConfig.TelemetryEvents.category eq category }
                } else {
                    MySQLConfig.TelemetryEvents.selectAll()
                }

                query.orderBy(MySQLConfig.TelemetryEvents.timestamp to SortOrder.DESC)
                    .limit(pageSize, offset.toLong())
                    .map { row ->
                        TelemetryEvent(
                            _id = row[MySQLConfig.TelemetryEvents.id],
                            id = row[MySQLConfig.TelemetryEvents.eventId],
                            category = row[MySQLConfig.TelemetryEvents.category],
                            name = row[MySQLConfig.TelemetryEvents.name],
                            value = row[MySQLConfig.TelemetryEvents.value],
                            timestamp = row[MySQLConfig.TelemetryEvents.timestamp].toString(),
                            properties = try {
                                json.decodeFromString<Map<String, String>>(row[MySQLConfig.TelemetryEvents.properties])
                            } catch (e: Exception) {
                                emptyMap()
                            },
                            metadataId = row[MySQLConfig.TelemetryEvents.metadataId]
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving paginated events: ${e.message}" }
            emptyList()
        }
    }

    /**
     * 获取分页的遥测元数据
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @return 遥测元数据列表
     */
    override fun getMetadata(page: Int, pageSize: Int): List<TelemetryMetadata> {
        return try {
            transaction {
                val offset = (page - 1) * pageSize

                MySQLConfig.TelemetryMetadata
                    .selectAll()
                    .orderBy(MySQLConfig.TelemetryMetadata.receivedTimestamp to SortOrder.DESC)
                    .limit(pageSize, offset.toLong())
                    .map { row ->
                        TelemetryMetadata(
                            _id = row[MySQLConfig.TelemetryMetadata.id],
                            systemId = "unknown",
                            pluginVersion = row[MySQLConfig.TelemetryMetadata.pluginVersion],
                            ideVersion = row[MySQLConfig.TelemetryMetadata.ideVersion],
                            ideBuild = row[MySQLConfig.TelemetryMetadata.ideBuild],
                            os = row[MySQLConfig.TelemetryMetadata.os],
                            osVersion = row[MySQLConfig.TelemetryMetadata.osVersion],
                            javaVersion = row[MySQLConfig.TelemetryMetadata.javaVersion],
                            timestamp = row[MySQLConfig.TelemetryMetadata.clientTimestamp],
                            receivedTimestamp = row[MySQLConfig.TelemetryMetadata.receivedTimestamp].toString()
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving paginated metadata: ${e.message}" }
            emptyList()
        }
    }

    /**
     * 获取所有事件类别
     * @return 事件类别列表
     */
    override fun getEventCategories(): List<String> {
        return try {
            transaction {
                MySQLConfig.TelemetryEvents
                    .select(MySQLConfig.TelemetryEvents.category)

                    .map { it[MySQLConfig.TelemetryEvents.category] }
                    .distinct()
                    .sorted()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event categories: ${e.message}" }
            emptyList()
        }
    }

    /**
     * 获取指定类别的事件名称列表
     * @param category 可选的事件类别过滤
     * @return 事件名称列表
     */
    override fun getEventNames(category: String?): List<String> {
        return try {
            transaction {
                val query = if (!category.isNullOrBlank()) {
                    MySQLConfig.TelemetryEvents
                        .select(MySQLConfig.TelemetryEvents.name)
                       .where { MySQLConfig.TelemetryEvents.category eq category }
                } else {
                    MySQLConfig.TelemetryEvents
                        .select(MySQLConfig.TelemetryEvents.name)

                }

                query.map { it[MySQLConfig.TelemetryEvents.name] }
                    .distinct()
                    .sorted()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event names: ${e.message}" }
            emptyList()
        }
    }

    /**
     * 获取按照筛选条件过滤的事件
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @param category 可选的事件类别过滤
     * @param name 可选的事件名称过滤
     * @param startDate 可选的开始日期过滤（格式：yyyy-MM-dd）
     * @param endDate 可选的结束日期过滤（格式：yyyy-MM-dd）
     * @param metadataId 可选的元数据ID过滤
     * @return 遥测事件列表
     */
    override fun getFilteredEvents(
        page: Int,
        pageSize: Int,
        category: String?,
        name: String?,
        startDate: String?,
        endDate: String?,
        metadataId: String?
    ): List<TelemetryEvent> {
        return try {
            transaction {
                val offset = (page - 1) * pageSize

                // 构建查询条件
                var query = MySQLConfig.TelemetryEvents.selectAll()

                // 应用过滤条件
                if (!metadataId.isNullOrBlank()) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.metadataId eq metadataId }
                }
                
                if (!category.isNullOrBlank()) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.category eq category }
                }

                if (!name.isNullOrBlank()) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.name eq name }
                }

                // 日期过滤
                val start = parseDate(startDate)
                if (start != null) {
                    val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    query = query.andWhere { MySQLConfig.TelemetryEvents.timestamp greaterEq startInstant }
                }

                val end = parseDate(endDate)
                if (end != null) {
                    val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    query = query.andWhere { MySQLConfig.TelemetryEvents.timestamp less endInstant }
                }

                // 排序和分页
                query.orderBy(MySQLConfig.TelemetryEvents.timestamp to SortOrder.DESC)
                    .limit(pageSize, offset.toLong())
                    .map { row ->
                        TelemetryEvent(
                            _id = row[MySQLConfig.TelemetryEvents.id],
                            id = row[MySQLConfig.TelemetryEvents.eventId],
                            category = row[MySQLConfig.TelemetryEvents.category],
                            name = row[MySQLConfig.TelemetryEvents.name],
                            value = row[MySQLConfig.TelemetryEvents.value],
                            timestamp = row[MySQLConfig.TelemetryEvents.timestamp].toString(),
                            properties = try {
                                json.decodeFromString<Map<String, String>>(row[MySQLConfig.TelemetryEvents.properties])
                            } catch (e: Exception) {
                                emptyMap()
                            },
                            metadataId = row[MySQLConfig.TelemetryEvents.metadataId]
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving filtered events: ${e.message}" }
            emptyList()
        }
    }

    /**
     * 获取按照筛选条件过滤的事件总数
     * @param category 可选的事件类别过滤
     * @param name 可选的事件名称过滤
     * @param startDate 可选的开始日期过滤（格式：yyyy-MM-dd）
     * @param endDate 可选的结束日期过滤（格式：yyyy-MM-dd）
     * @param metadataId 可选的元数据ID过滤
     * @return 事件总数
     */
    override fun getFilteredEventsCount(
        category: String?,
        name: String?,
        startDate: String?,
        endDate: String?,
        metadataId: String?
    ): Int {
        return try {
            transaction {
                // 构建查询条件
                var query = MySQLConfig.TelemetryEvents.selectAll()

                // 应用过滤条件
                if (!metadataId.isNullOrBlank()) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.metadataId eq metadataId }
                }
                
                if (!category.isNullOrBlank()) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.category eq category }
                }

                if (!name.isNullOrBlank()) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.name eq name }
                }

                // 日期过滤
                val start = parseDate(startDate)
                if (start != null) {
                    val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    query = query.andWhere { MySQLConfig.TelemetryEvents.timestamp greaterEq startInstant }
                }

                val end = parseDate(endDate)
                if (end != null) {
                    val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    query = query.andWhere { MySQLConfig.TelemetryEvents.timestamp less endInstant }
                }

                query.count().toInt()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting filtered events count: ${e.message}" }
            0
        }
    }

    /**
     * 获取按类别统计的事件数量
     * @param category 可选的事件类别过滤
     * @param timeRange 可选的天数限制
     * @return 类别和对应的事件数量
     */
    override fun getEventCountByCategory(category: String?, timeRange: Int): Map<String, Int> {
        val result = mutableMapOf<String, Int>()

        try {
            transaction {
                // 获取所有类别
                val categories = if (category != null) {
                    listOf(category)
                } else {
                    getEventCategories()
                }

                // 如果指定了天数限制，创建时间过滤器
                val cutoffTime = if (timeRange > 0) {
                    Instant.now().minus(timeRange.toLong(), ChronoUnit.DAYS)
                } else {
                    null
                }

                // 为每个类别统计事件数量
                categories.forEach { cat ->
                    var query = MySQLConfig.TelemetryEvents
                        .select(MySQLConfig.TelemetryEvents.id.count())
                        .where { MySQLConfig.TelemetryEvents.category eq cat }

                    // 应用时间过滤
                    if (cutoffTime != null) {
                        query = query.andWhere { MySQLConfig.TelemetryEvents.timestamp greaterEq cutoffTime }
                    }

                    val count = query.single()[MySQLConfig.TelemetryEvents.id.count()].toInt()
                    result[cat] = count
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event count by category: ${e.message}" }
        }

        return result
    }

    /**
     * 获取按天统计的事件数量
     * @param timeRange 天数
     * @param category 可选的事件类别过滤
     * @return 日期和对应的事件数量
     */
    override fun getEventCountByDay(timeRange: Int, category: String?): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        try {
            // 初始化结果Map，包含所有日期
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(timeRange.toLong())

            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                result[currentDate.format(dateFormatter)] = 0
                currentDate = currentDate.plusDays(1)
            }

            transaction {
                // 构建查询
                var query = MySQLConfig.TelemetryEvents
                    .select(MySQLConfig.TelemetryEvents.timestamp, MySQLConfig.TelemetryEvents.id.count())

                
                // 应用类别过滤
                if (category != null) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.category eq category }
                }
                
                // 应用日期范围过滤
                val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                
                query = query.andWhere { 
                    MySQLConfig.TelemetryEvents.timestamp greaterEq startInstant and 
                    (MySQLConfig.TelemetryEvents.timestamp less endInstant)
                }

                // 执行查询并处理结果
                query.forEach { row ->
                    val timestamp = row[MySQLConfig.TelemetryEvents.timestamp]
                    val date = LocalDate.ofInstant(timestamp, ZoneId.systemDefault())
                    val dateStr = date.format(dateFormatter)

                    // 如果日期在范围内，增加计数
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        result[dateStr] = (result[dateStr] ?: 0) + 1
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event count by day: ${e.message}" }
        }

        return result
    }

    /**
     * 获取按周统计的事件数量
     * @param timeRange 天数范围
     * @param category 可选的事件类别过滤
     * @return 周和对应的事件数量
     */
    override fun getEventCountByWeek(timeRange: Int, category: String?): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val weekFields = WeekFields.of(Locale.getDefault())

        try {
            // 初始化结果Map，包含所有周
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(timeRange.toLong())

            // 初始化周映射
            val weekMap = mutableMapOf<String, LocalDate>() // 周标识 -> 周的第一天
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val yearWeek = "${currentDate.year}-W${
                    currentDate.get(weekFields.weekOfWeekBasedYear()).toString().padStart(2, '0')
                }"
                if (!weekMap.containsKey(yearWeek)) {
                    weekMap[yearWeek] = currentDate.with(weekFields.dayOfWeek(), 1)
                }
                currentDate = currentDate.plusDays(1)
            }

            // 初始化结果
            weekMap.keys.forEach { week ->
                result[week] = 0
            }

            transaction {
                // 构建查询
                var query = MySQLConfig.TelemetryEvents
                    .select(MySQLConfig.TelemetryEvents.timestamp)

                
                // 应用类别过滤
                if (category != null) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.category eq category }
                }
                
                // 应用日期范围过滤
                val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                
                query = query.andWhere { 
                    MySQLConfig.TelemetryEvents.timestamp greaterEq startInstant and 
                    (MySQLConfig.TelemetryEvents.timestamp less endInstant)
                }

                // 执行查询并处理结果
                query.forEach { row ->
                    val timestamp = row[MySQLConfig.TelemetryEvents.timestamp]
                    val date = LocalDate.ofInstant(timestamp, ZoneId.systemDefault())

                    // 如果日期在范围内
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        val yearWeek = "${date.year}-W${
                            date.get(weekFields.weekOfWeekBasedYear()).toString().padStart(2, '0')
                        }"
                        result[yearWeek] = (result[yearWeek] ?: 0) + 1
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event count by week: ${e.message}" }
        }

        return result
    }

    /**
     * 获取按月统计的事件数量
     * @param timeRange 天数范围
     * @param category 可选的事件类别过滤
     * @return 月和对应的事件数量
     */
    override fun getEventCountByMonth(timeRange: Int, category: String?): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

        try {
            // 初始化结果Map，包含所有月份
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(timeRange.toLong())

            // 初始化月份映射
            val monthMap = mutableMapOf<String, LocalDate>() // 月标识 -> 月的第一天
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val yearMonth = currentDate.format(monthFormatter)
                if (!monthMap.containsKey(yearMonth)) {
                    monthMap[yearMonth] = currentDate.withDayOfMonth(1)
                }
                currentDate = currentDate.plusDays(1)
            }

            // 初始化结果
            monthMap.keys.forEach { month ->
                result[month] = 0
            }

            transaction {
                // 构建查询
                var query = MySQLConfig.TelemetryEvents
                    .select(MySQLConfig.TelemetryEvents.timestamp)

                
                // 应用类别过滤
                if (category != null) {
                    query = query.andWhere { MySQLConfig.TelemetryEvents.category eq category }
                }
                
                // 应用日期范围过滤
                val startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                
                query = query.andWhere { 
                    MySQLConfig.TelemetryEvents.timestamp greaterEq startInstant and 
                    (MySQLConfig.TelemetryEvents.timestamp less endInstant)
                }

                // 执行查询并处理结果
                query.forEach { row ->
                    val timestamp = row[MySQLConfig.TelemetryEvents.timestamp]
                    val date = LocalDate.ofInstant(timestamp, ZoneId.systemDefault())

                    // 如果日期在范围内
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        val yearMonth = date.format(monthFormatter)
                        result[yearMonth] = (result[yearMonth] ?: 0) + 1
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event count by month: ${e.message}" }
        }

        return result
    }

    /**
     * 获取独立用户数量
     * @param timeRange 天数范围
     * @return 独立用户数量
     */
    override fun getUniqueUsersCount(timeRange: Int): Int {
        return try {
            transaction {
                val cutoffTime = Instant.now().minusSeconds(timeRange * 24L * 60L * 60L)

                MySQLConfig.TelemetryMetadata
                    .select(MySQLConfig.TelemetryMetadata.id)
                    .where { MySQLConfig.TelemetryMetadata.clientTimestamp greaterEq cutoffTime.toEpochMilli() }
                    .withDistinct()
                    .count()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting unique users count: ${e.message}" }
            0
        }.toInt()
    }

    /**
     * 根据ID获取元数据
     * @param id 元数据ID
     * @return 元数据对象，如果不存在则返回null
     */
    override fun getMetadataById(id: String): TelemetryMetadata? {
        return try {
            transaction {
                MySQLConfig.TelemetryMetadata
                    .select { MySQLConfig.TelemetryMetadata.id eq id }
                    .limit(1)
                    .map { row ->
                        TelemetryMetadata(
                            _id = row[MySQLConfig.TelemetryMetadata.id],
                            systemId = row[MySQLConfig.TelemetryMetadata.systemId],
                            pluginVersion = row[MySQLConfig.TelemetryMetadata.pluginVersion],
                            ideVersion = row[MySQLConfig.TelemetryMetadata.ideVersion],
                            ideBuild = row[MySQLConfig.TelemetryMetadata.ideBuild],
                            os = row[MySQLConfig.TelemetryMetadata.os],
                            osVersion = row[MySQLConfig.TelemetryMetadata.osVersion],
                            javaVersion = row[MySQLConfig.TelemetryMetadata.javaVersion],
                            timestamp = row[MySQLConfig.TelemetryMetadata.clientTimestamp],
                            receivedTimestamp = row[MySQLConfig.TelemetryMetadata.receivedTimestamp].toString()
                        )
                    }
                    .firstOrNull()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving metadata by ID: ${e.message}" }
            null
        }
    }

    /**
     * 清理过期数据
     * @param retentionDays 保留天数，超过此天数的数据将被删除
     * @return 删除的记录数
     */
    override fun cleanupOldData(retentionDays: Int): Int {
        return try {
            transaction {
                // 计算截止时间
                val cutoffTime = Instant.now().minusSeconds(retentionDays * 24L * 60L * 60L)

                // 查找需要删除的元数据IDs
                val oldMetadataIds = MySQLConfig.TelemetryMetadata
                    .select(MySQLConfig.TelemetryMetadata.id)
                    .where { MySQLConfig.TelemetryMetadata.clientTimestamp less cutoffTime.toEpochMilli() }
                    .map { it[MySQLConfig.TelemetryMetadata.id] }

                var totalDeleted = 0

                // 删除关联的事件
                if (oldMetadataIds.isNotEmpty()) {
                    val eventDeleteCount = MySQLConfig.TelemetryEvents
                        .deleteWhere { MySQLConfig.TelemetryEvents.metadataId inList oldMetadataIds }

                    totalDeleted += eventDeleteCount
                    logger.info { "删除了 $eventDeleteCount 条过期事件数据" }
                }

                // 删除过期的元数据
                val metadataDeleteCount = MySQLConfig.TelemetryMetadata
                    .deleteWhere { MySQLConfig.TelemetryMetadata.clientTimestamp less cutoffTime.toEpochMilli() }

                totalDeleted += metadataDeleteCount
                logger.info { "删除了 $metadataDeleteCount 条过期元数据" }

                totalDeleted
            }
        } catch (e: Exception) {
            logger.error(e) { "清理过期数据失败: ${e.message}" }
            0
        }
    }

    /**
     * 删除单个事件
     * @param id 事件ID
     * @return 是否删除成功
     */
    override fun deleteEvent(id: String): Boolean {
        return try {
            transaction {
                val deletedCount = MySQLConfig.TelemetryEvents
                    .deleteWhere { MySQLConfig.TelemetryEvents.eventId eq id }
                deletedCount > 0
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting event with ID $id: ${e.message}" }
            false
        }
    }
    
    /**
     * 批量删除事件
     * @param ids 事件ID列表
     * @return 成功删除的事件数量
     */
    override fun bulkDeleteEvents(ids: List<String>): Int {
        if (ids.isEmpty()) return 0
        
        return try {
            transaction {
                MySQLConfig.TelemetryEvents.deleteWhere { MySQLConfig.TelemetryEvents.id.inList(ids) }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error bulk deleting events: ${e.message}" }
            0
        }
    }

    /**
     * 根据ID获取事件
     * @param id 事件ID
     * @return 事件对象，如果不存在则返回null
     */
    override fun getEventById(id: String): TelemetryEvent? {
        return try {
            transaction {
                MySQLConfig.TelemetryEvents
                    .select { MySQLConfig.TelemetryEvents.eventId eq id }
                    .limit(1)
                    .map { row ->
                        TelemetryEvent(
                            _id = row[MySQLConfig.TelemetryEvents.id],
                            id = row[MySQLConfig.TelemetryEvents.eventId],
                            category = row[MySQLConfig.TelemetryEvents.category],
                            name = row[MySQLConfig.TelemetryEvents.name],
                            value = row[MySQLConfig.TelemetryEvents.value],
                            timestamp = row[MySQLConfig.TelemetryEvents.timestamp].toString(),
                            properties = try {
                                json.decodeFromString<Map<String, String>>(row[MySQLConfig.TelemetryEvents.properties])
                            } catch (e: Exception) {
                                emptyMap()
                            },
                            metadataId = row[MySQLConfig.TelemetryEvents.metadataId]
                        )
                    }
                    .firstOrNull()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event by ID: ${e.message}" }
            null
        }
    }
} 