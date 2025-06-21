package cn.cangnova.repository

import cn.cangnova.model.TelemetryEvent
import cn.cangnova.model.TelemetryMetadata
import cn.cangnova.model.TelemetryPayload
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 遥测数据仓库接口，定义了遥测数据的存储和检索操作
 */
interface TelemetryRepository {
    /**
     * 保存遥测数据
     * @param payload 遥测数据载荷
     * @return 保存的事件数量
     */
    fun saveTelemetryData(payload: TelemetryPayload): Int
    
    /**
     * 获取最近的遥测事件
     * @param limit 限制返回的事件数量
     * @return 遥测事件列表
     */
    fun getRecentEvents(limit: Int = 100): List<TelemetryEvent>
    
    /**
     * 获取最近的遥测元数据
     * @param limit 限制返回的元数据数量
     * @return 遥测元数据列表
     */
    fun getRecentMetadata(limit: Int = 100): List<TelemetryMetadata>
    
    /**
     * 获取遥测事件总数
     * @param category 可选的事件类别过滤
     * @return 事件总数
     */
    fun getTotalEventsCount(category: String? = null): Int
    
    /**
     * 获取遥测元数据总数
     * @return 元数据总数
     */
    fun getTotalMetadataCount(): Int
    
    /**
     * 获取分页的遥测事件
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @param category 可选的事件类别过滤
     * @return 遥测事件列表
     */
    fun getEvents(page: Int, pageSize: Int, category: String? = null): List<TelemetryEvent>
    
    /**
     * 获取分页的遥测元数据
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @return 遥测元数据列表
     */
    fun getMetadata(page: Int, pageSize: Int): List<TelemetryMetadata>
    
    /**
     * 获取所有事件类别
     * @return 事件类别列表
     */
    fun getEventCategories(): List<String>
    
    /**
     * 获取指定类别的事件名称列表
     * @param category 可选的事件类别过滤
     * @return 事件名称列表
     */
    fun getEventNames(category: String? = null): List<String>
    
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
    fun getFilteredEvents(
        page: Int,
        pageSize: Int,
        category: String? = null,
        name: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        metadataId: String? = null
    ): List<TelemetryEvent> {

        return getEvents(page, pageSize, category)
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
    fun getFilteredEventsCount(
        category: String? = null,
        name: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        metadataId: String? = null
    ): Int {
        // 默认实现，可以被具体实现类覆盖以提供更高效的实现
        return getTotalEventsCount(category)
    }
    
    /**
     * 清理旧数据
     * @param retentionDays 保留天数
     * @return 删除的记录数
     */
    fun cleanupOldData(retentionDays: Int): Int
    
    /**
     * 按类别获取事件计数
     * @param category 可选的事件类别过滤
     * @param timeRange 时间范围（天）
     * @return 类别及其对应的事件计数映射
     */
    fun getEventCountByCategory(category: String? = null, timeRange: Int = 30): Map<String, Int>
    
    /**
     * 按天获取事件计数
     * @param timeRange 时间范围（天）
     * @param category 可选的事件类别过滤
     * @return 日期及其对应的事件计数映射
     */
    fun getEventCountByDay(timeRange: Int = 30, category: String? = null): Map<String, Int>
    
    /**
     * 按周获取事件计数
     * @param timeRange 时间范围（天）
     * @param category 可选的事件类别过滤
     * @return 周及其对应的事件计数映射
     */
    fun getEventCountByWeek(timeRange: Int = 30, category: String? = null): Map<String, Int>
    
    /**
     * 按月获取事件计数
     * @param timeRange 时间范围（天）
     * @param category 可选的事件类别过滤
     * @return 月及其对应的事件计数映射
     */
    fun getEventCountByMonth(timeRange: Int = 30, category: String? = null): Map<String, Int>
    
    /**
     * 获取唯一用户数
     * @param timeRange 时间范围（天）
     * @return 唯一用户数
     */
    fun getUniqueUsersCount(timeRange: Int = 30): Int
    
    /**
     * 根据ID获取事件
     * @param id 事件ID
     * @return 事件对象，如果不存在则返回null
     */
    fun getEventById(id: String): TelemetryEvent?
    
    /**
     * 根据ID获取元数据
     * @param id 元数据ID
     * @return 元数据对象，如果不存在则返回null
     */
    fun getMetadataById(id: String): TelemetryMetadata?
    
    /**
     * 删除单个事件
     * @param id 事件ID
     * @return 是否删除成功
     */
    fun deleteEvent(id: String): Boolean
    
    /**
     * 批量删除事件
     * @param ids 事件ID列表
     * @return 成功删除的事件数量
     */
    fun bulkDeleteEvents(ids: List<String>): Int
    
    /**
     * 辅助方法：解析日期字符串
     * @param dateStr 日期字符串（格式：yyyy-MM-dd）
     * @return 解析后的LocalDate，如果解析失败则返回null
     */
    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            null
        }
    }
} 