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
import cn.cangnova.model.TelemetryEvent
import cn.cangnova.model.TelemetryMetadata
import cn.cangnova.model.TelemetryPayload
import cn.cangnova.repository.TelemetryRepository
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import mu.KotlinLogging
import org.bson.Document
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*

/**
 * 基于MongoDB的遥测数据仓库，负责存储遥测数据
 */
object MongoTelemetryRepository : TelemetryRepository {
    private val logger = KotlinLogging.logger {}

    /**
     * 保存遥测数据
     * @param payload 遥测数据载荷
     * @return 保存的事件数量
     */
    override fun saveTelemetryData(payload: TelemetryPayload): Int {
        try {
            // 保存元数据
            val metadata = payload.metadata
            val metadataDoc = Document()
                .append("_id", metadata._id)
                .append("pluginVersion", metadata.pluginVersion)
                .append("ideVersion", metadata.ideVersion)
                .append("ideBuild", metadata.ideBuild)
                .append("os", metadata.os)
                .append("osVersion", metadata.osVersion)
                .append("javaVersion", metadata.javaVersion)
                .append("timestamp", metadata.timestamp)
                .append("systemId", metadata.systemId)

                .append("receivedTimestamp", Instant.now().toString())

            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            mongoConfig.getMetadataCollection().insertOne(metadataDoc)

            // 保存事件
            var count = 0
            payload.events.forEach { event ->
                try {
                    val eventDoc = Document()
                        .append("_id", event._id)
                        .append("id", event.id)
                        .append("category", event.category)
                        .append("name", event.name)
                        .append("value", event.value)
                        .append("timestamp", event.timestamp)
                        .append("properties", Document(event.properties))
                        .append("metadataId", metadata._id)

                    mongoConfig.getEventsCollection().insertOne(eventDoc)
                    count++
                } catch (e: Exception) {
                    // 记录错误但继续处理其他事件
                    logger.error(e) { "Error saving event ${event.id}: ${e.message}" }
                }
            }

            return count
        } catch (e: Exception) {
            logger.error(e) { "Error saving telemetry data: ${e.message}" }
            return 0
        }
    }

    /**
     * 获取最近的遥测事件
     * @param limit 限制返回的事件数量
     * @return 遥测事件列表
     */
    override fun getRecentEvents(limit: Int): List<TelemetryEvent> {
        val events = mutableListOf<TelemetryEvent>()

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()
            val cursor = collection
                .find()
                .sort(Sorts.descending("timestamp"))
                .limit(limit)

            cursor.forEach { doc ->
                val event = TelemetryEvent(
                    _id = doc.getString("_id") ?: "",
                    id = doc.getString("id") ?: "",
                    category = doc.getString("category") ?: "",
                    name = doc.getString("name") ?: "",
                    value = doc.getString("value") ?: "",
                    timestamp = doc.getString("timestamp") ?: "",
                    properties = (doc.get("properties") as? Document)?.mapNotNull { entry ->
                        entry.key to entry.value.toString()
                    }?.toMap() ?: emptyMap(),
                    metadataId = doc.getString("metadataId")
                )
                events.add(event)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving recent events: ${e.message}" }
        }

        return events
    }

    /**
     * 获取最近的遥测元数据
     * @param limit 限制返回的元数据数量
     * @return 遥测元数据列表
     */
    override fun getRecentMetadata(limit: Int): List<TelemetryMetadata> {
        val metadataList = mutableListOf<TelemetryMetadata>()

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getMetadataCollection()
            val cursor = collection
                .find()
                .sort(Sorts.descending("receivedTimestamp"))
                .limit(limit)

            cursor.forEach { doc ->
                val metadata = TelemetryMetadata(
                    _id = doc.getString("_id") ?: "",
                    systemId = doc.getString("systemId") ?: "unknown",
                    pluginVersion = doc.getString("pluginVersion") ?: "",
                    ideVersion = doc.getString("ideVersion") ?: "",
                    ideBuild = doc.getString("ideBuild") ?: "",
                    os = doc.getString("os") ?: "",
                    osVersion = doc.getString("osVersion") ?: "",
                    javaVersion = doc.getString("javaVersion") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    receivedTimestamp = doc.getString("receivedTimestamp") ?: ""
                )
                metadataList.add(metadata)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving recent metadata: ${e.message}" }
        }

        return metadataList
    }

    /**
     * 获取遥测事件总数
     * @param category 可选的事件类别过滤
     * @return 事件总数
     */
    override fun getTotalEventsCount(category: String?): Int {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            return if (category != null) {
                collection.countDocuments(Filters.eq("category", category)).toInt()
            } else {
                collection.countDocuments().toInt()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting total events count: ${e.message}" }
            return 0
        }
    }

    /**
     * 获取遥测元数据总数
     * @return 元数据总数
     */
    override fun getTotalMetadataCount(): Int {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getMetadataCollection()
            return collection.countDocuments().toInt()
        } catch (e: Exception) {
            logger.error(e) { "Error getting total metadata count: ${e.message}" }
            return 0
        }
    }

    /**
     * 获取分页的遥测事件
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @param category 可选的事件类别过滤
     * @return 遥测事件列表
     */
    override fun getEvents(page: Int, pageSize: Int, category: String?): List<TelemetryEvent> {
        val events = mutableListOf<TelemetryEvent>()

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            val skip = (page - 1) * pageSize

            val filter = if (category != null) {
                Filters.eq("category", category)
            } else {
                null
            }

            val cursor = if (filter != null) {
                collection.find(filter)
            } else {
                collection.find()
            }

            cursor.sort(Sorts.descending("timestamp"))
                .skip(skip)
                .limit(pageSize)
                .forEach { doc ->
                    val event = TelemetryEvent(
                        _id = doc.getString("_id") ?: "",
                        id = doc.getString("id") ?: "",
                        category = doc.getString("category") ?: "",
                        name = doc.getString("name") ?: "",
                        value = doc.getString("value") ?: "",
                        timestamp = doc.getString("timestamp") ?: "",
                        properties = (doc.get("properties") as? Document)?.mapNotNull { entry ->
                            entry.key to entry.value.toString()
                        }?.toMap() ?: emptyMap(),
                        metadataId = doc.getString("metadataId")
                    )
                    events.add(event)
                }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving paginated events: ${e.message}" }
        }

        return events
    }

    /**
     * 获取分页的遥测元数据
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @return 遥测元数据列表
     */
    override fun getMetadata(page: Int, pageSize: Int): List<TelemetryMetadata> {
        val metadataList = mutableListOf<TelemetryMetadata>()

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getMetadataCollection()

            val skip = (page - 1) * pageSize

            collection.find()
                .sort(Sorts.descending("receivedTimestamp"))
                .skip(skip)
                .limit(pageSize)
                .forEach { doc ->
                    val metadata = TelemetryMetadata(
                        _id = doc.getString("_id") ?: "",
                        systemId = doc.getString("systemId") ?: "unknown",
                        pluginVersion = doc.getString("pluginVersion") ?: "",
                        ideVersion = doc.getString("ideVersion") ?: "",
                        ideBuild = doc.getString("ideBuild") ?: "",
                        os = doc.getString("os") ?: "",
                        osVersion = doc.getString("osVersion") ?: "",
                        javaVersion = doc.getString("javaVersion") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        receivedTimestamp = doc.getString("receivedTimestamp") ?: ""
                    )
                    metadataList.add(metadata)
                }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving paginated metadata: ${e.message}" }
        }

        return metadataList
    }

    /**
     * 获取所有事件类别
     * @return 事件类别列表
     */
    override fun getEventCategories(): List<String> {
        val categories = mutableListOf<String>()

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            // 使用聚合查询获取唯一的类别
            val cursor = collection.find()
            val categorySet = mutableSetOf<String>()

            cursor.forEach { doc ->
                val category = doc.getString("category")
                if (category != null && category.isNotEmpty()) {
                    categorySet.add(category)
                }
            }

            categories.addAll(categorySet)
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event categories: ${e.message}" }
        }

        return categories
    }

    /**
     * 获取指定类别的事件名称列表
     * @param category 可选的事件类别过滤
     * @return 事件名称列表
     */
    override fun getEventNames(category: String?): List<String> {
        val names = mutableListOf<String>()

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            // 创建过滤器
            val filter = if (category != null && category.isNotEmpty()) {
                Filters.eq("category", category)
            } else {
                null
            }

            // 查询事件
            val cursor = if (filter != null) {
                collection.find(filter)
            } else {
                collection.find()
            }

            val nameSet = mutableSetOf<String>()
            cursor.forEach { doc ->
                val name = doc.getString("name")
                if (name != null && name.isNotEmpty()) {
                    nameSet.add(name)
                }
            }

            names.addAll(nameSet.sorted())
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event names: ${e.message}" }
        }

        return names
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
        val events = mutableListOf<TelemetryEvent>()

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            val skip = (page - 1) * pageSize

            // 构建过滤条件
            val filters = mutableListOf<org.bson.conversions.Bson>()

            // 元数据ID过滤
            if (!metadataId.isNullOrBlank()) {
                filters.add(Filters.eq("metadataId", metadataId))
            }

            // 类别过滤
            if (!category.isNullOrBlank()) {
                filters.add(Filters.eq("category", category))
            }

            // 名称过滤
            if (!name.isNullOrBlank()) {
                filters.add(Filters.eq("name", name))
            }

            // 日期过滤
            val start = parseDate(startDate)
            val end = parseDate(endDate)

            if (start != null || end != null) {
                val dateFilters = mutableListOf<org.bson.conversions.Bson>()

                if (start != null) {
                    val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    dateFilters.add(Filters.gte("timestamp", startInstant.toString()))
                }

                if (end != null) {
                    val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    dateFilters.add(Filters.lt("timestamp", endInstant.toString()))
                }

                if (dateFilters.isNotEmpty()) {
                    filters.add(Filters.and(dateFilters))
                }
            }

            // 应用过滤器
            val cursor = if (filters.isNotEmpty()) {
                collection.find(Filters.and(filters))
            } else {
                collection.find()
            }

            // 排序、分页
            cursor.sort(Sorts.descending("timestamp"))
                .skip(skip)
                .limit(pageSize)
                .forEach { doc ->
                    events.add(documentToEvent(doc))
                }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving filtered events: ${e.message}" }
        }

        return events
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
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            // 构建过滤条件
            val filters = mutableListOf<org.bson.conversions.Bson>()

            // 元数据ID过滤
            if (!metadataId.isNullOrBlank()) {
                filters.add(Filters.eq("metadataId", metadataId))
            }
            
            // 类别过滤
            if (!category.isNullOrBlank()) {
                filters.add(Filters.eq("category", category))
            }

            // 名称过滤
            if (!name.isNullOrBlank()) {
                filters.add(Filters.eq("name", name))
            }

            // 日期过滤
            val start = parseDate(startDate)
            val end = parseDate(endDate)

            if (start != null || end != null) {
                val dateFilters = mutableListOf<org.bson.conversions.Bson>()

                if (start != null) {
                    val startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    dateFilters.add(Filters.gte("timestamp", startInstant.toString()))
                }

                if (end != null) {
                    val endInstant = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    dateFilters.add(Filters.lt("timestamp", endInstant.toString()))
                }

                if (dateFilters.isNotEmpty()) {
                    filters.add(Filters.and(dateFilters))
                }
            }

            // 应用过滤器并计数
            return if (filters.isNotEmpty()) {
                collection.countDocuments(Filters.and(filters)).toInt()
            } else {
                collection.countDocuments().toInt()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting filtered events count: ${e.message}" }
            return 0
        }
    }

    /**
     * 获取按类别统计的事件数量
     * @param category 可选的事件类别过滤
     * @param timeRange 时间范围（天）
     * @return 类别及其对应的事件计数映射
     */
    override fun getEventCountByCategory(category: String?, timeRange: Int): Map<String, Int> {
        val result = mutableMapOf<String, Int>()

        try {
            val categories = if (category != null) listOf(category) else getEventCategories()
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            // 如果指定了天数限制，创建时间过滤器
            val timeFilter = if (timeRange > 0) {
                val startInstant = Instant.now().minus(timeRange.toLong(), ChronoUnit.DAYS)
                Filters.gte("timestamp", startInstant.toString())
            } else {
                null
            }

            categories.forEach { cat ->
                // 创建类别过滤器
                val categoryFilter = Filters.eq("category", cat)

                // 组合过滤器
                val filter = if (timeFilter != null) {
                    Filters.and(categoryFilter, timeFilter)
                } else {
                    categoryFilter
                }

                // 计数
                val count = collection.countDocuments(filter).toInt()
                result[cat] = count
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
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            // 获取过去days天的数据
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(timeRange.toLong())

            // 初始化结果Map，包含所有日期
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                result[currentDate.format(dateFormatter)] = 0
                currentDate = currentDate.plusDays(1)
            }

            // 创建过滤器
            val filter = if (category != null) {
                Filters.eq("category", category)
            } else {
                null
            }

            // 查询事件
            val cursor = if (filter != null) {
                collection.find(filter)
            } else {
                collection.find()
            }

            cursor.forEach { doc ->
                val timestamp = doc.getString("timestamp") ?: return@forEach

                try {
                    // 尝试解析时间戳
                    val instant = Instant.parse(timestamp)
                    val date = LocalDate.ofInstant(instant, ZoneId.systemDefault())
                    val dateStr = date.format(dateFormatter)

                    // 如果日期在范围内，增加计数
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        result[dateStr] = (result[dateStr] ?: 0) + 1
                    }
                } catch (e: Exception) {
                    // 忽略无法解析的时间戳
                    logger.debug(e) { "Could not parse timestamp: $timestamp" }
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
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            // 获取过去days天的数据
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(timeRange.toLong())

            // 初始化结果Map，包含所有周
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

            // 创建过滤器
            val filter = if (category != null) {
                Filters.eq("category", category)
            } else {
                null
            }

            // 查询事件
            val cursor = if (filter != null) {
                collection.find(filter)
            } else {
                collection.find()
            }

            cursor.forEach { doc ->
                val timestamp = doc.getString("timestamp") ?: return@forEach

                try {
                    // 尝试解析时间戳
                    val instant = Instant.parse(timestamp)
                    val date = LocalDate.ofInstant(instant, ZoneId.systemDefault())

                    // 如果日期在范围内
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        val yearWeek =
                            "${date.year}-W${date.get(weekFields.weekOfWeekBasedYear()).toString().padStart(2, '0')}"
                        result[yearWeek] = (result[yearWeek] ?: 0) + 1
                    }
                } catch (e: Exception) {
                    // 忽略无法解析的时间戳
                    logger.debug(e) { "Could not parse timestamp: $timestamp" }
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
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            // 获取过去days天的数据
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(timeRange.toLong())

            // 初始化结果Map，包含所有月份
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

            // 创建过滤器
            val filter = if (category != null) {
                Filters.eq("category", category)
            } else {
                null
            }

            // 查询事件
            val cursor = if (filter != null) {
                collection.find(filter)
            } else {
                collection.find()
            }

            cursor.forEach { doc ->
                val timestamp = doc.getString("timestamp") ?: return@forEach

                try {
                    // 尝试解析时间戳
                    val instant = Instant.parse(timestamp)
                    val date = LocalDate.ofInstant(instant, ZoneId.systemDefault())

                    // 如果日期在范围内
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        val yearMonth = date.format(monthFormatter)
                        result[yearMonth] = (result[yearMonth] ?: 0) + 1
                    }
                } catch (e: Exception) {
                    // 忽略无法解析的时间戳
                    logger.debug(e) { "Could not parse timestamp: $timestamp" }
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
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getMetadataCollection()

            val cutoffTime = Instant.now().minus(timeRange.toLong(), ChronoUnit.DAYS).toEpochMilli()
            val filter = Filters.gte("timestamp", cutoffTime)

            // 使用distinct获取唯一的设备标识
            val uniqueIds = collection.distinct("_id", filter, String::class.java)

            return uniqueIds.toList().size
        } catch (e: Exception) {
            logger.error(e) { "Error getting unique users count: ${e.message}" }
            return 0
        }
    }

    /**
     * 根据ID获取元数据
     * @param id 元数据ID
     * @return 元数据对象，如果不存在则返回null
     */
    override fun getMetadataById(id: String): TelemetryMetadata? {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getMetadataCollection()
            val doc = collection.find(Filters.eq("_id", id)).firstOrNull()
            
            return doc?.let {
                TelemetryMetadata(
                    _id = it.getString("_id") ?: "",
                    systemId = it.getString("systemId") ?: "unknown",
                    pluginVersion = it.getString("pluginVersion") ?: "",
                    ideVersion = it.getString("ideVersion") ?: "",
                    ideBuild = it.getString("ideBuild") ?: "",
                    os = it.getString("os") ?: "",
                    osVersion = it.getString("osVersion") ?: "",
                    javaVersion = it.getString("javaVersion") ?: "",
                    timestamp = it.getLong("timestamp") ?: 0L,
                    receivedTimestamp = it.getString("receivedTimestamp") ?: ""
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving metadata by ID: ${e.message}" }
            return null
        }
    }

    /**
     * 根据ID获取事件
     * @param id 事件ID
     * @return 事件对象，如果不存在则返回null
     */
    override fun getEventById(id: String): TelemetryEvent? {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()
            val doc = collection.find(Filters.eq("id", id)).firstOrNull()
            
            return doc?.let {
                TelemetryEvent(
                    _id = it.getString("_id") ?: "",
                    id = it.getString("id") ?: "",
                    category = it.getString("category") ?: "",
                    name = it.getString("name") ?: "",
                    value = it.getString("value") ?: "",
                    timestamp = it.getString("timestamp") ?: "",
                    properties = (it.get("properties") as? Document)?.mapNotNull { entry ->
                        entry.key to entry.value.toString()
                    }?.toMap() ?: emptyMap(),
                    metadataId = it.getString("metadataId")
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving event by ID: ${e.message}" }
            return null
        }
    }

    /**
     * 清理过期数据
     * @param retentionDays 保留天数，超过此天数的数据将被删除
     * @return 删除的记录数
     */
    override fun cleanupOldData(retentionDays: Int): Int {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val eventsCollection = mongoConfig.getEventsCollection()
            val metadataCollection = mongoConfig.getMetadataCollection()

            // 计算截止时间
            val cutoffTime = Instant.now().minus(retentionDays.toLong(), ChronoUnit.DAYS).toEpochMilli()

            // 查找需要删除的元数据
            val oldMetadataIds = mutableListOf<String>()
            val metadataFilter = Filters.lt("timestamp", cutoffTime)
            val metadataCursor = metadataCollection.find(metadataFilter)

            metadataCursor.forEach { doc ->
                val id = doc.getString("_id")
                if (id != null) {
                    oldMetadataIds.add(id)
                }
            }

            var totalDeleted = 0

            // 删除关联的事件
            if (oldMetadataIds.isNotEmpty()) {
                val eventFilter = Filters.`in`("metadataId", oldMetadataIds)
                val eventDeleteResult = eventsCollection.deleteMany(eventFilter)
                totalDeleted += eventDeleteResult.deletedCount.toInt()

                logger.info { "删除了 ${eventDeleteResult.deletedCount} 条过期事件数据" }
            }

            // 删除过期的元数据
            val metadataDeleteResult = metadataCollection.deleteMany(metadataFilter)
            totalDeleted += metadataDeleteResult.deletedCount.toInt()

            logger.info { "删除了 ${metadataDeleteResult.deletedCount} 条过期元数据" }

            return totalDeleted
        } catch (e: Exception) {
            logger.error(e) { "清理过期数据失败: ${e.message}" }
            return 0
        }
    }

    /**
     * 删除单个事件
     * @param id 事件ID
     * @return 是否删除成功
     */
    override fun deleteEvent(id: String): Boolean {
        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()
            val result = collection.deleteOne(Filters.eq("id", id))
            return result.deletedCount > 0
        } catch (e: Exception) {
            logger.error(e) { "Error deleting event with ID $id: ${e.message}" }
            return false
        }
    }
    
    /**
     * 批量删除事件
     * @param ids 事件ID列表
     * @return 成功删除的事件数量
     */
    override fun bulkDeleteEvents(ids: List<String>): Int {
        if (ids.isEmpty()) return 0

        try {
            val mongoConfig = DatabaseFactory.getTypedConfig<MongoDBConfig>()
            val collection = mongoConfig.getEventsCollection()

            // 创建批量删除请求
            val result = collection.deleteMany(Filters.`in`("_id", ids))
            return result.deletedCount.toInt()
        } catch (e: Exception) {
            logger.error(e) { "Failed to bulk delete events: ${e.message}" }
            return 0
        }
    }

    /**
     * 辅助方法：将MongoDB文档转换为TelemetryEvent对象
     */
    private fun documentToEvent(document: Document): TelemetryEvent {
        return TelemetryEvent(
            _id = document.getString("_id") ?: "",
            id = document.getString("id") ?: "",
            category = document.getString("category") ?: "",
            name = document.getString("name") ?: "",
            value = document.getString("value") ?: "",
            timestamp = document.getString("timestamp") ?: "",
            properties = (document.get("properties") as? Document)?.mapNotNull { entry ->
                entry.key to entry.value.toString()
            }?.toMap() ?: emptyMap(),
            metadataId = document.getString("metadataId")
        )
    }
} 