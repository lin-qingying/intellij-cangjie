package cn.cangnova.repository

import cn.cangnova.model.SystemSettings

/**
 * 系统设置仓库接口
 */
interface SystemSettingsRepository {
    /**
     * 获取系统设置
     * @return 系统设置对象
     */
    suspend fun getSettings(): SystemSettings
    
    /**
     * 更新系统设置
     * @param settings 要更新的系统设置对象
     * @return 更新后的系统设置对象
     */
    suspend fun updateSettings(settings: SystemSettings): SystemSettings
    
    /**
     * 更新系统设置 - 提供单独的参数而不是整个对象
     * @param dataRetentionDays 数据保留天数
     * @param apiKeyEnabled 是否启用API密钥
     * @param anonymousReportingEnabled 是否启用匿名报告
     * @return 更新后的系统设置对象
     */
    suspend fun updateSettings(
        dataRetentionDays: Int,
        apiKeyEnabled: Boolean,
        anonymousReportingEnabled: Boolean
    ): SystemSettings {
        val currentSettings = getSettings()
        val updatedSettings = currentSettings.copy(
            dataRetentionDays = dataRetentionDays,
            apiKeyEnabled = apiKeyEnabled,
            anonymousReportingEnabled = anonymousReportingEnabled
        )
        return updateSettings(updatedSettings)
    }
    
    /**
     * 重置系统设置为默认值
     * @return 重置后的系统设置对象
     */
    suspend fun resetSettings(): SystemSettings
} 