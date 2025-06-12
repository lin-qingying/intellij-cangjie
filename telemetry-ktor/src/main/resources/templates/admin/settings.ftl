<#-- 系统设置模板 -->

<#assign title="CangJie 遥测管理后台 - 系统设置">
<#assign currentPage="settings">
<#assign pageTitle="系统设置">

<#import "layout.ftl" as layout>

<@layout.page title=title currentPage=currentPage pageTitle=pageTitle additionalStyles='
<style>
    .settings-card {
        border-radius: 1rem;
        border: none;
        box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
        margin-bottom: 1.5rem;
        transition: all 0.2s ease;
    }
    
    .settings-card:hover {
        transform: translateY(-3px);
        box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
    }
    
    .settings-card .card-header {
        background-color: transparent;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
        padding: 1.25rem 1.5rem;
        font-weight: 600;
        display: flex;
        align-items: center;
    }
    
    .settings-card .card-header i {
        font-size: 1.25rem;
        margin-right: 0.75rem;
        width: 28px;
        height: 28px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 0.5rem;
    }
    
    .settings-card .card-body {
        padding: 1.5rem;
    }
    
    .system-badge {
        font-size: 0.875rem;
        padding: 0.5rem 0.75rem;
        border-radius: 0.5rem;
    }
    
    .list-group-item {
        padding: 0.75rem 1.25rem;
        border-left: none;
        border-right: none;
    }
    
    .list-group-item:first-child {
        border-top: none;
    }
    
    .list-group-item:last-child {
        border-bottom: none;
    }
    
    .memory-bar {
        height: 10px;
        border-radius: 5px;
        margin-top: 0.5rem;
        background-color: #e9ecef;
        overflow: hidden;
    }
    
    .memory-used {
        height: 100%;
        background: linear-gradient(90deg, #4361ee, #3a0ca3);
    }
    
    .tool-card {
        height: 100%;
        border-radius: 0.75rem;
        border: none;
        transition: all 0.2s;
        background-color: #fff;
    }
    
    .tool-card:hover {
        transform: translateY(-5px);
        box-shadow: 0 0.5rem 1.5rem rgba(0, 0, 0, 0.1);
    }
    
    .tool-card .card-body {
        padding: 1.5rem;
    }
    
    .tool-icon {
        width: 50px;
        height: 50px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 1rem;
        margin-bottom: 1rem;
        font-size: 1.5rem;
    }
    
    .tool-backup {
        background-color: rgba(67, 97, 238, 0.1);
        color: #4361ee;
    }
    
    .tool-cleanup {
        background-color: rgba(255, 159, 67, 0.1);
        color: #ff9f43;
    }
    
    .tool-diagnostic {
        background-color: rgba(0, 207, 232, 0.1);
        color: #00cfe8;
    }
    
    .form-switch .form-check-input {
        width: 3em;
        height: 1.5em;
        margin-top: 0.25em;
    }
    
    .form-control, .form-select {
        border-radius: 0.5rem;
        padding: 0.6rem 1rem;
    }
    
    .form-control:focus, .form-select:focus {
        box-shadow: 0 0 0 0.25rem rgba(67, 97, 238, 0.25);
        border-color: #4361ee;
    }
    
    .tab-content {
        padding: 1.5rem 0;
    }
    
    .nav-tabs .nav-link {
        border: none;
        padding: 0.75rem 1.25rem;
        font-weight: 500;
        color: #6c757d;
        border-radius: 0.5rem 0.5rem 0 0;
    }
    
    .nav-tabs .nav-link.active {
        color: #4361ee;
        border-bottom: 3px solid #4361ee;
        background-color: transparent;
    }
    
    .nav-tabs .nav-link:hover:not(.active) {
        border-bottom: 3px solid #e9ecef;
    }
    
    .alert {
        border-radius: 0.75rem;
        border: none;
    }
    
    .settings-section {
        margin-bottom: 2rem;
    }
    
    .settings-section-title {
        font-size: 1.25rem;
        font-weight: 600;
        margin-bottom: 1rem;
        padding-bottom: 0.5rem;
        border-bottom: 1px solid #e9ecef;
    }
</style>'>

    <!-- 系统状态概览 -->
    <div class="row">
        <div class="col-md-6 col-xl-3 mb-4">
            <div class="settings-card h-100">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-3">
                        <div class="bg-primary-subtle text-primary p-3 rounded-3">
                            <i class="fas fa-database"></i>
                        </div>
                        <div class="ms-3">
                            <h6 class="mb-0 text-muted">数据库类型</h6>
                            <h4 class="fw-bold mb-0">
                                <#if databaseType??>
                                    ${databaseType}
                                <#else>
                                    未知
                                </#if>
                            </h4>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="col-md-6 col-xl-3 mb-4">
            <div class="settings-card h-100">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-3">
                        <div class="bg-success-subtle text-success p-3 rounded-3">
                            <i class="fas fa-microchip"></i>
                        </div>
                        <div class="ms-3">
                            <h6 class="mb-0 text-muted">处理器核心数</h6>
                            <h4 class="fw-bold mb-0">${availableProcessors!'0'}</h4>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="col-md-6 col-xl-3 mb-4">
            <div class="settings-card h-100">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-3">
                        <div class="bg-warning-subtle text-warning p-3 rounded-3">
                            <i class="fas fa-memory"></i>
                        </div>
                        <div class="ms-3">
                            <h6 class="mb-0 text-muted">内存使用</h6>
                            <h4 class="fw-bold mb-0">
                                <#if totalMemoryMB?? && maxMemoryMB??>
                                    ${totalMemoryMB}/${maxMemoryMB} MB
                                <#else>
                                    未知
                                </#if>
                            </h4>
                        </div>
                    </div>
                    <#if totalMemoryMB?? && maxMemoryMB??>
                        <#assign usedPercentage = (totalMemoryMB?number / maxMemoryMB?number) * 100>
                        <div class="memory-bar">
                            <div class="memory-used" style="width: ${usedPercentage}%"></div>
                        </div>
                        <div class="d-flex justify-content-between mt-1">
                            <small class="text-muted">已使用: ${totalMemoryMB} MB</small>
                            <small class="text-muted">可用: ${freeMemoryMB} MB</small>
                        </div>
                    </#if>
                </div>
            </div>
        </div>
        
        <div class="col-md-6 col-xl-3 mb-4">
            <div class="settings-card h-100">
                <div class="card-body">
                    <div class="d-flex align-items-center mb-3">
                        <div class="bg-info-subtle text-info p-3 rounded-3">
                            <i class="fas fa-server"></i>
                        </div>
                        <div class="ms-3">
                            <h6 class="mb-0 text-muted">操作系统</h6>
                            <h4 class="fw-bold mb-0">
                                <#if osName??>
                                    ${osName}
                                <#else>
                                    未知
                                </#if>
                            </h4>
                        </div>
                    </div>
                    <div class="mt-2 text-muted">
                        <small>
                            <i class="fas fa-code-branch me-1"></i> Java ${javaVersion!'未知'}
                            <#if osVersion??>
                                <span class="ms-2"><i class="fas fa-tag me-1"></i> ${osVersion}</span>
                            </#if>
                        </small>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 设置选项卡 -->
    <div class="settings-card">
        <div class="card-header">
            <i class="fas fa-cogs bg-primary-subtle text-primary"></i>
            <span>系统配置</span>
        </div>
        <div class="card-body">
            <ul class="nav nav-tabs" id="settingsTabs" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" id="general-tab" data-bs-toggle="tab" data-bs-target="#general" type="button" role="tab" aria-controls="general" aria-selected="true">
                        <i class="fas fa-sliders-h me-2"></i>常规设置
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="database-tab" data-bs-toggle="tab" data-bs-target="#database" type="button" role="tab" aria-controls="database" aria-selected="false">
                        <i class="fas fa-database me-2"></i>数据库设置
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="advanced-tab" data-bs-toggle="tab" data-bs-target="#advanced" type="button" role="tab" aria-controls="advanced" aria-selected="false">
                        <i class="fas fa-code me-2"></i>高级设置
                    </button>
                </li>
            </ul>
            
            <div class="tab-content" id="settingsTabsContent">
                <!-- 常规设置 -->
                <div class="tab-pane fade show active" id="general" role="tabpanel" aria-labelledby="general-tab">
                    <#if success??>
                        <div class="alert alert-success">
                            <div class="d-flex">
                                <div class="me-3">
                                    <i class="fas fa-check-circle fa-2x"></i>
                                </div>
                                <div>
                                    <h5 class="alert-heading">保存成功</h5>
                                    <p class="mb-0">系统设置已成功更新。</p>
                                </div>
                            </div>
                        </div>
                    </#if>
                    
                    <#if reset??>
                        <div class="alert alert-info">
                            <div class="d-flex">
                                <div class="me-3">
                                    <i class="fas fa-sync fa-2x"></i>
                                </div>
                                <div>
                                    <h5 class="alert-heading">重置成功</h5>
                                    <p class="mb-0">系统设置已重置为默认值。</p>
                                </div>
                            </div>
                        </div>
                    </#if>
                    
                    <#if error??>
                        <div class="alert alert-danger">
                            <div class="d-flex">
                                <div class="me-3">
                                    <i class="fas fa-exclamation-circle fa-2x"></i>
                                </div>
                                <div>
                                    <h5 class="alert-heading">操作失败</h5>
                                    <p class="mb-0">${error}</p>
                                </div>
                            </div>
                        </div>
                    </#if>
                    
                    <form action="/web/settings/update" method="post">
                        <div class="settings-section">
                            <h5 class="settings-section-title">数据保留策略</h5>
                            <div class="row mb-4">
                                <div class="col-md-6">
                                    <label for="dataRetentionDays" class="form-label">数据保留天数</label>
                                    <input type="number" class="form-control" id="dataRetentionDays" name="dataRetentionDays" value="${settings.dataRetentionDays}">
                                    <div class="form-text">设置遥测数据的保留时间。超过此天数的数据将被自动清理。</div>
                                </div>
                                <div class="col-md-6">
                                    <label for="cleanupSchedule" class="form-label">清理计划</label>
                                    <select class="form-select" id="cleanupSchedule" name="cleanupSchedule">
                                        <option value="daily" <#if settings.cleanupSchedule == "daily">selected</#if>>每天</option>
                                        <option value="weekly" <#if settings.cleanupSchedule == "weekly">selected</#if>>每周</option>
                                        <option value="monthly" <#if settings.cleanupSchedule == "monthly">selected</#if>>每月</option>
                                    </select>
                                    <div class="form-text">设置自动清理过期数据的频率。</div>
                                </div>
                            </div>
                            
                            <div class="form-check form-switch mb-3">
                                <input class="form-check-input" type="checkbox" id="enableAutoCleanup" name="enableAutoCleanup" <#if settings.enableAutoCleanup>checked</#if>>
                                <label class="form-check-label" for="enableAutoCleanup">启用自动清理</label>
                            </div>
                        </div>
                        
                        <div class="settings-section">
                            <h5 class="settings-section-title">备份设置</h5>
                            <div class="row mb-4">
                                <div class="col-md-6">
                                    <label for="backupSchedule" class="form-label">备份计划</label>
                                    <select class="form-select" id="backupSchedule" name="backupSchedule">
                                        <option value="daily" <#if settings.backupSchedule == "daily">selected</#if>>每天</option>
                                        <option value="weekly" <#if settings.backupSchedule == "weekly">selected</#if>>每周</option>
                                        <option value="monthly" <#if settings.backupSchedule == "monthly">selected</#if>>每月</option>
                                    </select>
                                    <div class="form-text">设置自动备份数据的频率。</div>
                                </div>
                                <div class="col-md-6">
                                    <label for="backupRetention" class="form-label">备份保留数量</label>
                                    <input type="number" class="form-control" id="backupRetention" name="backupRetention" value="${settings.backupRetention}">
                                    <div class="form-text">系统将保留最近的几份备份。</div>
                                </div>
                            </div>
                            
                            <div class="form-check form-switch mb-3">
                                <input class="form-check-input" type="checkbox" id="enableAutoBackup" name="enableAutoBackup" <#if settings.enableAutoBackup>checked</#if>>
                                <label class="form-check-label" for="enableAutoBackup">启用自动备份</label>
                            </div>
                        </div>
                        
                        <div class="d-flex justify-content-end">
                            <button type="button" id="resetButton" class="btn btn-outline-secondary me-2">重置</button>
                            <button type="submit" class="btn btn-primary">保存设置</button>
                        </div>
                    </form>
                </div>
                
                <!-- 数据库设置 -->
                <div class="tab-pane fade" id="database" role="tabpanel" aria-labelledby="database-tab">
                    <div class="alert alert-info">
                        <div class="d-flex">
                            <div class="me-3">
                                <i class="fas fa-info-circle fa-2x"></i>
                            </div>
                            <div>
                                <h5 class="alert-heading">配置说明</h5>
                                <p class="mb-0">数据库配置在应用程序启动时从配置文件加载。如需更改数据库设置，请修改配置文件并重启应用程序。</p>
                            </div>
                        </div>
                    </div>
                    
                    <div class="settings-section">
                        <h5 class="settings-section-title">当前数据库配置</h5>
                        <div class="row">
                            <div class="col-md-6">
                                <div class="mb-3">
                                    <label class="form-label">数据库类型</label>
                                    <input type="text" class="form-control" value="${databaseType!'未知'}" readonly>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="mb-3">
                                    <label class="form-label">连接状态</label>
                                    <div>
                                        <span class="badge bg-success system-badge">
                                            <i class="fas fa-check-circle me-1"></i> 已连接
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <form action="/web/settings/update" method="post">
                        <div class="settings-section">
                            <h5 class="settings-section-title">性能设置</h5>
                            <div class="row">
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <label for="eventBatchSize" class="form-label">事件批处理大小</label>
                                        <input type="number" class="form-control" id="eventBatchSize" name="eventBatchSize" value="${settings.eventBatchSize}">
                                        <div class="form-text">设置处理遥测事件时的批处理大小。</div>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <label for="connectionPoolSize" class="form-label">连接池大小</label>
                                        <input type="number" class="form-control" id="connectionPoolSize" name="connectionPoolSize" value="${settings.connectionPoolSize}">
                                        <div class="form-text">设置数据库连接池的大小。</div>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="d-flex justify-content-end">
                                <button type="submit" class="btn btn-primary">保存设置</button>
                            </div>
                        </div>
                    </form>
                </div>
                
                <!-- 高级设置 -->
                <div class="tab-pane fade" id="advanced" role="tabpanel" aria-labelledby="advanced-tab">
                    <div class="alert alert-warning">
                        <div class="d-flex">
                            <div class="me-3">
                                <i class="fas fa-exclamation-triangle fa-2x"></i>
                            </div>
                            <div>
                                <h5 class="alert-heading">注意</h5>
                                <p class="mb-0">高级设置可能会影响系统性能和稳定性。请谨慎修改。</p>
                            </div>
                        </div>
                    </div>
                    
                    <form action="/web/settings/update" method="post">
                        <div class="settings-section">
                            <h5 class="settings-section-title">日志设置</h5>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="logLevel" class="form-label">日志级别</label>
                                    <select class="form-select" id="logLevel" name="logLevel">
                                        <option value="ERROR" <#if settings.logLevel == "ERROR">selected</#if>>ERROR</option>
                                        <option value="WARN" <#if settings.logLevel == "WARN">selected</#if>>WARN</option>
                                        <option value="INFO" <#if settings.logLevel == "INFO">selected</#if>>INFO</option>
                                        <option value="DEBUG" <#if settings.logLevel == "DEBUG">selected</#if>>DEBUG</option>
                                        <option value="TRACE" <#if settings.logLevel == "TRACE">selected</#if>>TRACE</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label for="logRetention" class="form-label">日志保留天数</label>
                                    <input type="number" class="form-control" id="logRetention" name="logRetention" value="${settings.logRetention}">
                                </div>
                            </div>
                        </div>
                        
                        <div class="settings-section">
                            <h5 class="settings-section-title">缓存设置</h5>
                            <div class="form-check form-switch mb-3">
                                <input class="form-check-input" type="checkbox" id="enableCache" name="enableCache" <#if settings.enableCache>checked</#if>>
                                <label class="form-check-label" for="enableCache">启用缓存</label>
                            </div>
                            <div class="row">
                                <div class="col-md-6">
                                    <label for="cacheSize" class="form-label">缓存大小 (MB)</label>
                                    <input type="number" class="form-control" id="cacheSize" name="cacheSize" value="${settings.cacheSize}">
                                </div>
                                <div class="col-md-6">
                                    <label for="cacheExpiry" class="form-label">缓存过期时间 (分钟)</label>
                                    <input type="number" class="form-control" id="cacheExpiry" name="cacheExpiry" value="${settings.cacheExpiry}">
                                </div>
                            </div>
                            
                            <div class="d-flex justify-content-end mt-3">
                                <button type="submit" class="btn btn-primary">保存设置</button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
    
    <!-- 维护工具 -->
    <div class="settings-card">
        <div class="card-header">
            <i class="fas fa-tools bg-warning-subtle text-warning"></i>
            <span>维护工具</span>
        </div>
        <div class="card-body">
            <#if cleanupSuccess??>
                <div class="alert alert-success mb-4">
                    <div class="d-flex">
                        <div class="me-3">
                            <i class="fas fa-check-circle fa-2x"></i>
                        </div>
                        <div>
                            <h5 class="alert-heading">数据清理成功</h5>
                            <p class="mb-0">已成功清理 ${cleanupCount!'0'} 条过期数据。</p>
                        </div>
                    </div>
                </div>
            </#if>
            
            <div class="row g-4">
                <div class="col-md-4">
                    <div class="tool-card shadow-sm">
                        <div class="card-body text-center">
                            <div class="tool-icon tool-backup mx-auto">
                                <i class="fas fa-save"></i>
                            </div>
                            <h5 class="card-title">数据库备份</h5>
                            <p class="card-text text-muted">创建数据库的完整备份，保存所有遥测数据和配置。</p>
                            <button class="btn btn-primary" id="backupButton">
                                <i class="fas fa-download me-2"></i>创建备份
                            </button>
                        </div>
                    </div>
                </div>
                
                <div class="col-md-4">
                    <div class="tool-card shadow-sm">
                        <div class="card-body text-center">
                            <div class="tool-icon tool-cleanup mx-auto">
                                <i class="fas fa-broom"></i>
                            </div>
                            <h5 class="card-title">清理旧数据</h5>
                            <p class="card-text text-muted">删除超过保留期限（${settings.dataRetentionDays}天）的旧数据，释放存储空间。</p>
                            <button class="btn btn-warning" id="cleanupButton">
                                <i class="fas fa-trash-alt me-2"></i>清理数据
                            </button>
                        </div>
                    </div>
                </div>
                
                <div class="col-md-4">
                    <div class="tool-card shadow-sm">
                        <div class="card-body text-center">
                            <div class="tool-icon tool-diagnostic mx-auto">
                                <i class="fas fa-stethoscope"></i>
                            </div>
                            <h5 class="card-title">系统诊断</h5>
                            <p class="card-text text-muted">运行系统诊断检查，识别并解决潜在问题。</p>
                            <button class="btn btn-info" id="diagnosticButton">
                                <i class="fas fa-heartbeat me-2"></i>运行诊断
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <!-- 重置确认模态框 -->
    <div class="modal fade" id="resetModal" tabindex="-1" aria-labelledby="resetModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="resetModalLabel">确认重置设置</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p>您确定要将所有设置重置为默认值吗？此操作不可撤销。</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <form action="/web/settings/reset" method="post">
                        <button type="submit" class="btn btn-danger">确认重置</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
    
    <!-- 清理确认模态框 -->
    <div class="modal fade" id="cleanupModal" tabindex="-1" aria-labelledby="cleanupModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="cleanupModalLabel">确认清理数据</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p>您确定要清理超过 ${settings.dataRetentionDays} 天的所有数据吗？此操作不可撤销。</p>
                    <div class="alert alert-warning">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        数据清理可能需要一些时间，请耐心等待。
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <form action="/web/settings/cleanup" method="post">
                        <button type="submit" class="btn btn-danger">确认清理</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
    
    <!-- 功能确认模态框 -->
    <div class="modal fade" id="featureModal" tabindex="-1" aria-labelledby="featureModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="featureModalLabel">功能预告</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p>此功能将在未来版本中提供。</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" data-bs-dismiss="modal">确定</button>
                </div>
            </div>
        </div>
    </div>
    
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // 重置按钮点击事件
            document.getElementById('resetButton').addEventListener('click', function() {
                var resetModal = new bootstrap.Modal(document.getElementById('resetModal'));
                resetModal.show();
            });
            
            // 清理按钮点击事件
            document.getElementById('cleanupButton').addEventListener('click', function() {
                var cleanupModal = new bootstrap.Modal(document.getElementById('cleanupModal'));
                cleanupModal.show();
            });
            
            // 功能按钮点击事件
            document.getElementById('backupButton').addEventListener('click', function() {
                var featureModal = new bootstrap.Modal(document.getElementById('featureModal'));
                featureModal.show();
            });
            
            document.getElementById('diagnosticButton').addEventListener('click', function() {
                var featureModal = new bootstrap.Modal(document.getElementById('featureModal'));
                featureModal.show();
            });
        });
    </script>
</@layout.page> 