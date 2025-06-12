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
     * 重置系统设置为默认值
     * @return 重置后的系统设置对象
     */
    suspend fun resetSettings(): SystemSettings
} 