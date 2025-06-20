<#-- 元数据详情模板 -->

<#assign title="CangJie 遥测管理后台 - 元数据详情">
<#assign currentPage="metadata">
<#assign pageTitle="元数据详情">

<#import "layout.ftl" as layout>

<@layout.page title=title currentPage=currentPage pageTitle=pageTitle additionalStyles='
<style>
    .metadata-card {
        border-radius: 1rem;
        overflow: hidden;
    }
    
    .detail-label {
        font-weight: 600;
        color: var(--bs-gray-600);
    }
    
    .detail-value {
        font-weight: 500;
    }
    
    .system-badge {
        font-size: 0.8rem;
        padding: 0.35rem 0.65rem;
    }
</style>'>

    <div class="row mb-4">
        <div class="col-md-12">
            <div class="d-flex justify-content-between align-items-center">
                <h1 class="h3 mb-0">
                    <i class="fas fa-info-circle text-primary me-2"></i>
                    元数据详情
                </h1>
                <a href="/web/metadata" class="btn btn-outline-secondary">
                    <i class="fas fa-arrow-left me-1"></i> 返回列表
                </a>
            </div>
        </div>
    </div>
    
    <#if error??>
        <div class="alert alert-danger" role="alert">
            <i class="fas fa-exclamation-circle me-2"></i> ${error}
        </div>
    </#if>
    
    <div class="row">
        <div class="col-lg-12">
            <div class="card metadata-card shadow-sm">
                <div class="card-header bg-transparent">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <span class="text-primary fw-bold">系统 ID:</span> ${metadata.systemId}
                        </div>
                        <div>
                            <span class="badge bg-primary system-badge">
                                <i class="fas fa-clock me-1"></i> ${formattedTime}
                            </span>
                        </div>
                    </div>
                </div>
                <div class="card-body">
                    <div class="row g-4">
                        <div class="col-md-6">
                            <div class="card h-100">
                                <div class="card-header bg-transparent">
                                    <i class="fas fa-plug text-primary me-2"></i> 插件信息
                                </div>
                                <div class="card-body">
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4 detail-label">插件版本</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.pluginVersion}</dd>
                                    </dl>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-md-6">
                            <div class="card h-100">
                                <div class="card-header bg-transparent">
                                    <i class="fas fa-laptop-code text-primary me-2"></i> IDE信息
                                </div>
                                <div class="card-body">
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4 detail-label">IDE版本</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.ideVersion}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">IDE构建</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.ideBuild}</dd>
                                    </dl>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-md-6">
                            <div class="card h-100">
                                <div class="card-header bg-transparent">
                                    <i class="fas fa-desktop text-primary me-2"></i> 系统信息
                                </div>
                                <div class="card-body">
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4 detail-label">操作系统</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.os}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">系统版本</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.osVersion}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">Java版本</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.javaVersion}</dd>
                                    </dl>
                                </div>
                            </div>
                        </div>
                        
                        <div class="col-md-6">
                            <div class="card h-100">
                                <div class="card-header bg-transparent">
                                    <i class="fas fa-database text-primary me-2"></i> 元数据信息
                                </div>
                                <div class="card-body">
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4 detail-label">ID</dt>
                                        <dd class="col-sm-8 detail-value text-truncate">${metadata.id}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">时间戳</dt>
                                        <dd class="col-sm-8 detail-value">${formattedTime}</dd>
                                    </dl>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="card-footer bg-transparent">
                    <div class="row">
                        <div class="col-md-12 text-end">
                            <a href="/web/events?metadataId=${metadata.id}" class="btn btn-primary">
                                <i class="fas fa-list me-1"></i> 查看相关事件
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

</@layout.page> 