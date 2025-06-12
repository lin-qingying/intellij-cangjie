<#-- 事件列表模板 -->

<#assign title="CangJie 遥测管理后台 - 事件列表">
<#assign currentPage="events">
<#assign pageTitle="事件列表">

<#import "layout.ftl" as layout>

<@layout.page title=title currentPage=currentPage pageTitle=pageTitle additionalStyles='
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
<style>
    .filter-card {
        border-radius: 0.75rem;
        border: none;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        margin-bottom: 1.5rem;
    }
    
    .filter-card .card-header {
        background: transparent;
        border-bottom: 1px solid rgba(var(--bs-body-color-rgb), .1);
        padding: 1rem 1.25rem;
    }
    
    .filter-form .form-control,
    .filter-form .form-select {
        border-radius: 0.5rem;
        padding: 0.6rem 1rem;
    }
    
    .filter-form .form-label {
        font-weight: 500;
        font-size: 0.875rem;
        margin-bottom: 0.5rem;
    }
    
    .events-table {
        font-size: 0.9rem;
    }
    
    .events-table th {
        font-weight: 600;
        text-transform: uppercase;
        font-size: 0.75rem;
        letter-spacing: 0.5px;
        white-space: nowrap;
    }
    
    .events-table td {
        vertical-align: middle;
    }
    
    .event-badge {
        font-size: 0.75rem;
        padding: 0.35rem 0.65rem;
        border-radius: 0.5rem;
        font-weight: 600;
    }
    
    .event-time {
        white-space: nowrap;
        font-size: 0.8rem;
    }
    
    .event-value {
        max-width: 200px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
    
    .properties-btn {
        border-radius: 0.5rem;
        padding: 0.25rem 0.75rem;
        font-size: 0.8rem;
        transition: all 0.2s;
    }
    
    /* 模态框样式优化 */
    .modal {
        will-change: opacity;
    }
    
    .modal-backdrop {
        will-change: opacity;
        transition: opacity 0.15s linear;
        opacity: 0;
    }
    
    .modal-backdrop.show {
        opacity: 0.5;
    }
    
    .properties-modal .modal-content {
        border: none;
        border-radius: 1rem;
        overflow: hidden;
        box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
        will-change: transform;
    }
    
    .properties-modal .modal-header {
        border-bottom: 1px solid rgba(var(--bs-body-color-rgb), .1);
        padding: 1rem 1.5rem;
    }
    
    .properties-modal .modal-footer {
        border-top: 1px solid rgba(var(--bs-body-color-rgb), .1);
        padding: 1rem 1.5rem;
    }
    
    .properties-modal .modal-dialog {
        transition: transform 0.15s ease-out, opacity 0.15s linear !important;
        will-change: transform, opacity;
    }
    
    .properties-modal.fade .modal-dialog {
        transform: scale(0.95);
        opacity: 0;
    }
    
    .properties-modal.show .modal-dialog {
        transform: scale(1);
        opacity: 1;
    }
    
    .properties-table {
        margin-bottom: 0;
    }
    
    .properties-table th {
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
    }
    
    .pagination {
        margin-bottom: 0;
    }
    
    .page-link {
        border-radius: 0.5rem;
        margin: 0 0.15rem;
        padding: 0.375rem 0.75rem;
        border: none;
        color: var(--bs-primary);
    }
    
    .page-item.active .page-link {
        background-color: var(--bs-primary);
        color: #fff;
        box-shadow: 0 2px 5px rgba(var(--bs-primary-rgb), 0.3);
    }
    
    .page-item.disabled .page-link {
        color: var(--bs-secondary);
    }
    
    .search-highlight {
        background-color: rgba(var(--bs-warning-rgb), 0.2);
        border-radius: 0.25rem;
        padding: 0.1rem 0.2rem;
    }
    
    .metadata-toggle {
        color: var(--bs-secondary);
        transition: all 0.2s ease;
    }
    
    .metadata-toggle:hover {
        color: var(--bs-primary);
    }
    
    .metadata-toggle:focus {
        box-shadow: none;
    }
</style>' additionalScripts='
<script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>
<script>
document.addEventListener("DOMContentLoaded", function() {
    // 初始化提示工具
    var tooltipTriggerList = document.querySelectorAll("[data-bs-toggle=tooltip]");
    for (var i = 0; i < tooltipTriggerList.length; i++) {
        new bootstrap.Tooltip(tooltipTriggerList[i]);
    }
    
    // 确保模态框的ARIA属性正确
    var modalElements = document.querySelectorAll(".modal");
    modalElements.forEach(function(modal) {
        // 移除可能导致问题的aria-hidden属性
        modal.removeAttribute("aria-hidden");
        
        // 监听模态框显示事件
        modal.addEventListener("shown.bs.modal", function() {
            // 确保模态框显示时不会被标记为hidden
            this.removeAttribute("aria-hidden");
        });
    });
    
    // 处理元数据折叠/展开按钮的交互
    var metadataToggles = document.querySelectorAll(".metadata-toggle");
    metadataToggles.forEach(function(toggle) {
        toggle.addEventListener("click", function() {
            var icon = this.querySelector("i");
            if (icon.classList.contains("fa-chevron-down")) {
                icon.classList.remove("fa-chevron-down");
                icon.classList.add("fa-chevron-up");
            } else {
                icon.classList.remove("fa-chevron-up");
                icon.classList.add("fa-chevron-down");
            }
        });
    });
    
    // 初始化日期选择器
    if(document.getElementById("startDate")) {
        flatpickr("#startDate", {
            dateFormat: "Y-m-d",
            maxDate: "today"
        });
    }
    
    if(document.getElementById("endDate")) {
        flatpickr("#endDate", {
            dateFormat: "Y-m-d",
            maxDate: "today"
        });
    }
    
    // 搜索功能
    var searchInput = document.getElementById("searchInput");
    if(searchInput) {
        searchInput.addEventListener("keyup", function() {
            var searchTerm = this.value.toLowerCase();
            var tableRows = document.querySelectorAll(".events-table tbody tr");
            
            for (var i = 0; i < tableRows.length; i++) {
                var row = tableRows[i];
                var text = row.textContent.toLowerCase();
                
                if(text.indexOf(searchTerm) > -1) {
                    row.style.display = "";
                    
                    // 高亮匹配文本
                    if(searchTerm.length > 0) {
                        var cells = row.querySelectorAll("td:not(:last-child)");
                        for (var j = 0; j < cells.length; j++) {
                            var cell = cells[j];
                            var originalText = cell.textContent;
                            
                            if(originalText.toLowerCase().indexOf(searchTerm) > -1) {
                                // 简单替换，避免使用复杂的正则表达式
                                var index = originalText.toLowerCase().indexOf(searchTerm.toLowerCase());
                                if (index >= 0) {
                                    var before = originalText.substring(0, index);
                                    var match = originalText.substring(index, index + searchTerm.length);
                                    var after = originalText.substring(index + searchTerm.length);
                                    cell.innerHTML = before + `<span class="search-highlight">` + match + `</span>` + after;
                                }
                            }
                        }
                    }
                } else {
                    row.style.display = "none";
                }
            }
        });
        
        // 重置搜索
        var resetSearchBtn = document.getElementById("resetSearch");
        if (resetSearchBtn) {
            resetSearchBtn.addEventListener("click", function() {
                searchInput.value = "";
                var event = new Event("keyup");
                searchInput.dispatchEvent(event);
            });
        }
    }
    
    // 重置筛选
    var resetFiltersBtn = document.getElementById("resetFilters");
    if (resetFiltersBtn) {
        resetFiltersBtn.addEventListener("click", function() {
            var categorySelect = document.getElementById("category");
            var nameSelect = document.getElementById("name");
            var pageSizeSelect = document.getElementById("pageSize");
            var startDateInput = document.getElementById("startDate");
            var endDateInput = document.getElementById("endDate");
            
            if (categorySelect) categorySelect.selectedIndex = 0;
            if (nameSelect) nameSelect.selectedIndex = 0;
            if (pageSizeSelect) pageSizeSelect.selectedIndex = 0;
            if (startDateInput) startDateInput.value = "";
            if (endDateInput) endDateInput.value = "";
        });
    }
});
</script>'>
    <!-- 筛选卡片 -->
    <div class="card filter-card">
        <div class="card-header d-flex justify-content-between align-items-center">
            <div>
                <i class="fas fa-filter me-2 text-primary"></i>
                <span class="fw-bold">筛选条件</span>
            </div>
            <button class="btn btn-sm btn-outline-secondary collapsed"  type="button" data-bs-toggle="collapse" data-bs-target="#filterCollapse" aria-expanded="true">
                <i class="fas fa-chevron-down"></i>
            </button>
        </div>
        <div class="collapse" id="filterCollapse">
            <div class="card-body">
                <form action="/web/events" method="get" class="row g-3 filter-form">
                    <!-- 搜索框 -->
                    <div class="col-lg-12 mb-2">
                        <div class="input-group">
                            <span class="input-group-text bg-transparent border-end-0">
                                <i class="fas fa-search text-muted"></i>
                            </span>
                            <input type="text" class="form-control border-start-0" id="searchInput" placeholder="搜索事件..." aria-label="搜索事件">
                            <button class="btn btn-outline-secondary border-start-0" type="button" id="resetSearch" title="清除搜索">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                        <small class="text-muted">搜索将在当前页面结果中进行筛选</small>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="category" class="form-label">事件类别</label>
                        <select class="form-select" id="category" name="category">
                            <option value="">全部类别</option>
                            <#if categories??>
                                <#list categories as cat>
                                    <option value="${cat}" <#if category?? && category == cat>selected</#if>>${cat}</option>
                                </#list>
                            </#if>
                        </select>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="name" class="form-label">事件名称</label>
                        <select class="form-select" id="name" name="name">
                            <option value="">全部名称</option>
                            <#if eventNames?? && eventNames?size gt 0>
                                <#list eventNames as eventName>
                                    <option value="${eventName}" <#if selectedEventName?? && selectedEventName == eventName>selected</#if>>${eventName}</option>
                                </#list>
                            </#if>
                        </select>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="startDate" class="form-label">开始日期</label>
                        <input type="text" class="form-control" id="startDate" name="startDate" placeholder="选择开始日期" value="${startDate!}">
                    </div>
                    
                    <div class="col-md-3">
                        <label for="endDate" class="form-label">结束日期</label>
                        <input type="text" class="form-control" id="endDate" name="endDate" placeholder="选择结束日期" value="${endDate!}">
                    </div>
                    
                    <div class="col-md-3">
                        <label for="pageSize" class="form-label">每页显示</label>
                        <select class="form-select" id="pageSize" name="pageSize">
                            <option value="10" <#if pageSize?? && pageSize == 10>selected</#if>>10 条/页</option>
                            <option value="25" <#if pageSize?? && pageSize == 25>selected</#if>>25 条/页</option>
                            <option value="50" <#if pageSize?? && pageSize == 50>selected</#if>>50 条/页</option>
                            <option value="100" <#if pageSize?? && pageSize == 100>selected</#if>>100 条/页</option>
                        </select>
                    </div>
                    
                    <div class="col-md-9 d-flex align-items-end gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="fas fa-filter me-2"></i>应用筛选
                        </button>
                        <button type="button" id="resetFilters" class="btn btn-outline-secondary">
                            <i class="fas fa-undo me-2"></i>重置筛选
                        </button>
                        <#if totalEvents??>
                            <div class="ms-auto text-muted">
                                共 <span class="fw-bold">${totalEvents}</span> 条记录
                            </div>
                        </#if>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- 事件列表卡片 -->
    <div class="card shadow-sm">
        <div class="card-header d-flex justify-content-between align-items-center">
            <div>
                <i class="fas fa-list me-2 text-primary"></i>
                <span class="fw-bold">事件列表</span>
            </div>
            <div class="dropdown">
                <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="fas fa-ellipsis-v"></i>
                </button>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><a class="dropdown-item" href="/web/events/export?format=csv<#if category??>&category=${category}</#if><#if selectedEventName??>&name=${selectedEventName}</#if><#if startDate??>&startDate=${startDate}</#if><#if endDate??>&endDate=${endDate}</#if>"><i class="fas fa-file-csv me-2"></i>导出为CSV</a></li>
                    <li><a class="dropdown-item" href="/web/events/export?format=json<#if category??>&category=${category}</#if><#if selectedEventName??>&name=${selectedEventName}</#if><#if startDate??>&startDate=${startDate}</#if><#if endDate??>&endDate=${endDate}</#if>"><i class="fas fa-file-code me-2"></i>导出为JSON</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item help-btn" href="#" data-bs-toggle="modal" data-bs-target="#helpModal"><i class="fas fa-question-circle me-2"></i>帮助</a></li>
                </ul>
            </div>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover events-table mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">ID</th>
                            <th>类别</th>
                            <th>名称</th>
                            <th>值</th>
                            <th>时间</th>
                            <th class="pe-3">属性</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if events?? && events?size gt 0>
                            <#list events as event>
                                <tr>
                                    <td class="ps-3">${event.id!''}</td>
                                    <td>
                                        <#assign badgeClass = "bg-primary">
                                        <#if event.category?? && event.category == "error">
                                            <#assign badgeClass = "bg-danger">
                                        <#elseif event.category?? && event.category == "warning">
                                            <#assign badgeClass = "bg-warning">
                                        <#elseif event.category?? && event.category == "info">
                                            <#assign badgeClass = "bg-info">
                                        <#elseif event.category?? && event.category == "success">
                                            <#assign badgeClass = "bg-success">
                                        </#if>
                                        <span class="badge ${badgeClass} event-badge">${event.category!''}</span>
                                    </td>
                                    <td>${event.name!''}</td>
                                    <td class="event-value" data-bs-toggle="tooltip" title="${event.value!''}">${event.value!''}</td>
                                    <td class="event-time">${event.timestamp!''}</td>
                                    <td class="pe-3">
                                        <#if event.properties?? && event.properties?size gt 0>
                                            <button type="button" class="btn btn-sm btn-outline-info properties-btn" 
                                                    data-bs-toggle="modal" data-bs-target="#propertiesModal-${event?index}">
                                                <i class="fas fa-list-ul me-1"></i> 查看属性
                                            </button>
                                        <#else>
                                            <span class="badge bg-light text-dark">无属性</span>
                                        </#if>
                                    </td>
                                </tr>
                            </#list>
                        <#else>
                            <tr>
                                <td colspan="6" class="text-center py-4">
                                    <div class="d-flex flex-column align-items-center">
                                        <i class="fas fa-search fa-3x text-muted mb-3"></i>
                                        <h5 class="fw-bold">没有找到事件数据</h5>
                                        <p class="text-muted">尝试调整筛选条件或清除筛选</p>
                                    </div>
                                </td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </div>
            
            <#-- 分页 -->
            <#if totalPages?? && totalPages gt 1>
                <div class="d-flex justify-content-between align-items-center p-3 border-top">
                    <div class="text-muted small">
                        显示 ${((currentPage!1) - 1) * (pageSize!10) + 1} - ${((currentPage!1) * (pageSize!10) > (totalEvents!0))?then(totalEvents!0, (currentPage!1) * (pageSize!10))} 条，共 ${totalEvents!0} 条
                    </div>
                    <nav aria-label="Page navigation">
                        <ul class="pagination">
                            <li class="page-item <#if (currentPage!1) <= 1>disabled</#if>">
                                <a class="page-link" href="/web/events?page=${(currentPage!1) - 1}&pageSize=${pageSize!10}<#if category??>&category=${category}</#if><#if selectedEventName??>&name=${selectedEventName}</#if><#if startDate??>&startDate=${startDate}</#if><#if endDate??>&endDate=${endDate}</#if>" aria-label="Previous">
                                    <i class="fas fa-chevron-left"></i>
                                </a>
                            </li>
                            
                            <#list 1..totalPages as i>
                                <#if i == (currentPage!1)>
                                    <li class="page-item active"><span class="page-link">${i}</span></li>
                                <#elseif i <= 2 || i >= totalPages - 1 || (i >= (currentPage!1) - 1 && i <= (currentPage!1) + 1)>
                                    <li class="page-item"><a class="page-link" href="/web/events?page=${i}&pageSize=${pageSize!10}<#if category??>&category=${category}</#if><#if selectedEventName??>&name=${selectedEventName}</#if><#if startDate??>&startDate=${startDate}</#if><#if endDate??>&endDate=${endDate}</#if>">${i}</a></li>
                                <#elseif i == 3 || i == totalPages - 2>
                                    <li class="page-item disabled"><span class="page-link">...</span></li>
                                </#if>
                            </#list>
                            
                            <li class="page-item <#if (currentPage!1) >= (totalPages!1)>disabled</#if>">
                                <a class="page-link" href="/web/events?page=${(currentPage!1) + 1}&pageSize=${pageSize!10}<#if category??>&category=${category}</#if><#if selectedEventName??>&name=${selectedEventName}</#if><#if startDate??>&startDate=${startDate}</#if><#if endDate??>&endDate=${endDate}</#if>" aria-label="Next">
                                    <i class="fas fa-chevron-right"></i>
                                </a>
                            </li>
                        </ul>
                    </nav>
                </div>
            </#if>
        </div>
    </div>
    
    <#-- 属性模态框 -->
    <#if events?? && events?size gt 0>
        <#list events as event>
            <#if event.properties?? && event.properties?size gt 0>
                <div class="modal fade properties-modal" id="propertiesModal-${event?index}" tabindex="-1" aria-labelledby="propertiesModalLabel-${event?index}" aria-hidden="false">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="propertiesModalLabel-${event?index}"><i class="fas fa-info-circle me-2 text-primary"></i>事件属性</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <div class="mb-3">
                                    <small class="text-muted">事件ID: ${event.id!''}</small>
                                    <h6 class="mb-0 mt-1">${event.name!''}</h6>
                                </div>
                                
                                <!-- 添加元数据信息部分 -->
                                <#if event.metadataId??>
                                <div class="card mb-3 bg-light">
                                    <div class="card-header py-2 bg-transparent d-flex justify-content-between align-items-center">
                                        <h6 class="mb-0"><i class="fas fa-info-circle me-2"></i>元数据信息</h6>
                                        <button class="btn btn-sm btn-link p-0 metadata-toggle" type="button" data-bs-toggle="collapse" data-bs-target="#metadataCollapse-${event?index}" aria-expanded="false">
                                            <i class="fas fa-chevron-down"></i>
                                        </button>
                                    </div>
                                    <div class="collapse" id="metadataCollapse-${event?index}">
                                        <div class="card-body py-2">
                                            <div class="row g-2">
                                                <#assign metadata = metadataMap[event.metadataId!'']!{}>
                                                <div class="col-md-6">
                                                    <small class="d-block text-muted">插件版本</small>
                                                    <span>${metadata.pluginVersion!'未知'}</span>
                                                </div>
                                                <div class="col-md-6">
                                                    <small class="d-block text-muted">IDE版本</small>
                                                    <span>${metadata.ideVersion!'未知'}</span>
                                                </div>
                                                <div class="col-md-6">
                                                    <small class="d-block text-muted">IDE构建号</small>
                                                    <span>${metadata.ideBuild!'未知'}</span>
                                                </div>
                                                <div class="col-md-6">
                                                    <small class="d-block text-muted">操作系统</small>
                                                    <span>${metadata.os!''} ${metadata.osVersion!''}</span>
                                                </div>
                                                <div class="col-md-6">
                                                    <small class="d-block text-muted">Java版本</small>
                                                    <span>${metadata.javaVersion!'未知'}</span>
                                                </div>
                                                <div class="col-md-6">
                                                    <small class="d-block text-muted">系统ID</small>
                                                    <span class="text-monospace">${metadata.systemId!'未知'}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                </#if>
                                
                                <h6 class="mb-2">事件属性</h6>
                                <div class="table-responsive">
                                    <table class="table table-sm properties-table">
                                        <thead class="table-light">
                                            <tr>
                                                <th>键</th>
                                                <th>值</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <#if event.properties?? && event.properties?size gt 0>
                                                <#list event.properties as key, value>
                                                    <tr>
                                                        <td><code>${key!''}</code></td>
                                                        <td>${value!''}</td>
                                                    </tr>
                                                </#list>
                                            <#else>
                                                <tr>
                                                    <td colspan="2" class="text-center py-3 text-muted">
                                                        <i class="fas fa-info-circle me-1"></i>没有属性数据
                                                    </td>
                                                </tr>
                                            </#if>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
                            </div>
                        </div>
                    </div>
                </div>
            </#if>
        </#list>
    </#if>
    
    <!-- 帮助模态框 -->
    <div class="modal fade properties-modal" id="helpModal" tabindex="-1" aria-labelledby="helpModalLabel" aria-hidden="false">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="helpModalLabel"><i class="fas fa-question-circle me-2 text-primary"></i>帮助信息</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <h6 class="fw-bold">如何使用筛选功能</h6>
                    <ul class="mb-4">
                        <li>使用<strong>事件类别</strong>筛选特定类型的事件</li>
                        <li>使用<strong>事件名称</strong>筛选特定名称的事件</li>
                        <li>使用<strong>日期范围</strong>筛选特定时间段的事件</li>
                        <li>使用<strong>搜索框</strong>在当前页面结果中快速查找</li>
                    </ul>
                    
                    <h6 class="fw-bold">事件类别说明</h6>
                    <div class="d-flex flex-wrap gap-2 mb-3">
                        <span class="badge bg-primary">默认</span>
                        <span class="badge bg-success">success</span>
                        <span class="badge bg-info">info</span>
                        <span class="badge bg-warning">warning</span>
                        <span class="badge bg-danger">error</span>
                    </div>
                    
                    <h6 class="fw-bold">导出数据</h6>
                    <p>您可以将当前筛选结果导出为CSV或JSON格式，方便进行进一步分析。</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" data-bs-dismiss="modal">了解了</button>
                </div>
            </div>
        </div>
    </div>
</@layout.page> 