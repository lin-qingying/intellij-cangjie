<#-- 事件详情模板 -->

<#assign title="CangJie 遥测管理后台 - 事件详情">
<#assign currentPage="events">
<#assign pageTitle="事件详情">

<#import "layout.ftl" as layout>

<@layout.page title=title currentPage=currentPage pageTitle=pageTitle additionalStyles='
<style>
    .event-card {
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
    
    .property-table th {
        font-weight: 600;
    }
    
    .property-key {
        font-weight: 500;
        color: var(--bs-primary);
    }
</style>' additionalScripts='
<script>
    // 删除确认
    document.addEventListener("DOMContentLoaded", function() {
        const deleteForm = document.getElementById("delete-event-form");
        if (deleteForm) {
            deleteForm.addEventListener("submit", function(e) {
                if (!confirm("确定要删除此事件吗？此操作无法撤销。")) {
                    e.preventDefault();
                }
            });
        }
    });
</script>'>

    <div class="row mb-4">
        <div class="col-md-12">
            <div class="d-flex justify-content-between align-items-center">
                <h1 class="h3 mb-0">
                    <i class="fas fa-file-alt text-primary me-2"></i>
                    事件详情
                </h1>
                <div>
                    <form id="delete-event-form" action="/web/events/${event.id}/delete" method="post" class="d-inline">
                        <button type="submit" class="btn btn-danger me-2">
                            <i class="fas fa-trash-alt me-1"></i> 删除
                        </button>
                    </form>
                    <a href="/web/events" class="btn btn-outline-secondary">
                        <i class="fas fa-arrow-left me-1"></i> 返回列表
                    </a>
                </div>
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
            <div class="card event-card shadow-sm">
                <div class="card-header bg-transparent">
                    <div class="d-flex justify-content-between align-items-center">
                        <h5 class="card-title mb-0">
                            <span class="badge bg-${categoryColor!"primary"} me-2">${event.category}</span>
                            ${event.name}
                        </h5>
                        <span class="badge bg-secondary">
                            <i class="fas fa-clock me-1"></i> ${formattedTime!"未知时间"}
                        </span>
                    </div>
                </div>
                <div class="card-body">
                    <div class="row g-4">
                        <div class="col-md-6">
                            <div class="card h-100">
                                <div class="card-header bg-transparent">
                                    <i class="fas fa-info-circle text-primary me-2"></i> 基本信息
                                </div>
                                <div class="card-body">
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4 detail-label">ID</dt>
                                        <dd class="col-sm-8 detail-value text-truncate">${event.id}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">类别</dt>
                                        <dd class="col-sm-8 detail-value">${event.category}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">名称</dt>
                                        <dd class="col-sm-8 detail-value">${event.name}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">值</dt>
                                        <dd class="col-sm-8 detail-value">${event.value!""}</dd>
                                    </dl>
                                </div>
                            </div>
                        </div>
                        
                        <#if event.metadataId??>
                        <div class="col-md-6">
                            <div class="card h-100">
                                <div class="card-header bg-transparent">
                                    <i class="fas fa-database text-primary me-2"></i> 关联元数据
                                    <a href="/web/metadata/${event.metadataId}" class="float-end text-decoration-none">
                                        <i class="fas fa-external-link-alt"></i>
                                    </a>
                                </div>
                                <div class="card-body">
                                    <#if metadata??>
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4 detail-label">系统 ID</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.systemId}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">插件版本</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.pluginVersion}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">IDE版本</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.ideVersion}</dd>
                                        
                                        <dt class="col-sm-4 detail-label">操作系统</dt>
                                        <dd class="col-sm-8 detail-value">${metadata.os} ${metadata.osVersion}</dd>
                                    </dl>
                                    <#else>
                                    <div class="text-center py-3">
                                        <p class="text-muted mb-0">无法加载关联元数据</p>
                                    </div>
                                    </#if>
                                </div>
                            </div>
                        </div>
                        </#if>
                        
                        <div class="col-12">
                            <div class="card">
                                <div class="card-header bg-transparent">
                                    <i class="fas fa-code text-primary me-2"></i> 属性数据
                                </div>
                                <div class="card-body">
                                    <#assign props = event.properties!{}>
                                    <#if props?has_content>
                                        <div class="table-responsive">
                                            <table class="table table-striped table-hover property-table">
                                                <thead class="table-light">
                                                    <tr>
                                                        <th scope="col">键</th>
                                                        <th scope="col">值</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <#list props as key, value>
                                                        <tr>
                                                            <td class="property-key">${key}</td>
                                                            <td>${value!'null'}</td>
                                                        </tr>
                                                    </#list>
                                                </tbody>
                                            </table>
                                        </div>
                                    <#else>
                                        <p class="text-center text-muted my-3">没有属性数据</p>
                                    </#if>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

</@layout.page> 