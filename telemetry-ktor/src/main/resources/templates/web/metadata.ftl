<#-- 元数据列表模板 -->

<#assign title="CangJie 遥测管理后台 - 元数据列表">
<#assign currentPage="metadata">
<#assign pageTitle="元数据列表">

<#import "layout.ftl" as layout>

<@layout.page title=title currentPage=currentPage pageTitle=pageTitle additionalStyles='
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
    
    .metadata-table {
        font-size: 0.9rem;
    }
    
    .metadata-table th {
        font-weight: 600;
        text-transform: uppercase;
        font-size: 0.75rem;
        letter-spacing: 0.5px;
        white-space: nowrap;
    }
    
    .metadata-table td {
        vertical-align: middle;
    }
    
    .os-badge {
        font-size: 0.75rem;
        padding: 0.35rem 0.65rem;
        border-radius: 0.5rem;
        font-weight: 600;
    }
    
    .details-btn {
        border-radius: 0.5rem;
        padding: 0.25rem 0.75rem;
        font-size: 0.8rem;
        transition: all 0.2s;
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
    
    .search-input {
        border-radius: 0.5rem;
        padding: 0.6rem 1rem;
    }
    
    .metadata-detail-table th {
        width: 35%;
        font-size: 0.85rem;
    }
    
    .metadata-detail-table td {
        font-size: 0.9rem;
    }
</style>'>
    <!-- 筛选卡片 -->
    <div class="card filter-card">
        <div class="card-header d-flex justify-content-between align-items-center">
            <div>
                <i class="fas fa-filter me-2 text-primary"></i>
                <span class="fw-bold">筛选条件</span>
            </div>
            <button class="btn btn-sm btn-outline-secondary collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#filterCollapse" aria-expanded="false">
                <i class="fas fa-chevron-down"></i>
            </button>
        </div>
        <div class="collapse" id="filterCollapse">
            <div class="card-body">
                <form action="/web/metadata" method="get" class="row g-3 filter-form">
                    <!-- 搜索框 -->
                    <div class="col-lg-12 mb-2">
                        <div class="input-group">
                            <span class="input-group-text bg-transparent border-end-0">
                                <i class="fas fa-search text-muted"></i>
                            </span>
                            <input type="text" class="form-control border-start-0" id="searchInput" placeholder="搜索元数据..." aria-label="搜索元数据">
                            <button class="btn btn-outline-secondary border-start-0" type="button" id="resetSearch" title="清除搜索">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                        <small class="text-muted">搜索将在当前页面结果中进行筛选</small>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="os" class="form-label">操作系统</label>
                        <select class="form-select" id="os" name="os">
                            <option value="">全部操作系统</option>
                            <option value="Windows">Windows</option>
                            <option value="Mac OS X">macOS</option>
                            <option value="Linux">Linux</option>
                        </select>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="ideVersion" class="form-label">IDE版本</label>
                        <select class="form-select" id="ideVersion" name="ideVersion">
                            <option value="">全部版本</option>
                            <#if ideVersions?? && ideVersions?size gt 0>
                                <#list ideVersions as version>
                                    <option value="${version}">${version}</option>
                                </#list>
                            </#if>
                        </select>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="pluginVersion" class="form-label">插件版本</label>
                        <select class="form-select" id="pluginVersion" name="pluginVersion">
                            <option value="">全部版本</option>
                            <#if pluginVersions?? && pluginVersions?size gt 0>
                                <#list pluginVersions as version>
                                    <option value="${version}">${version}</option>
                                </#list>
                            </#if>
                        </select>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="pageSize" class="form-label">每页显示</label>
                        <select class="form-select" id="pageSize" name="pageSize">
                            <option value="10" <#if pageSize == 10>selected</#if>>10 条/页</option>
                            <option value="25" <#if pageSize == 25>selected</#if>>25 条/页</option>
                            <option value="50" <#if pageSize == 50>selected</#if>>50 条/页</option>
                            <option value="100" <#if pageSize == 100>selected</#if>>100 条/页</option>
                        </select>
                    </div>
                    
                    <div class="col-md-12 d-flex align-items-end gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="fas fa-filter me-2"></i>应用筛选
                        </button>
                        <button type="button" id="resetFilters" class="btn btn-outline-secondary">
                            <i class="fas fa-undo me-2"></i>重置筛选
                        </button>
                        <#if totalItems??>
                            <div class="ms-auto text-muted">
                                共 <span class="fw-bold">${totalItems}</span> 条记录
                            </div>
                        </#if>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- 元数据列表卡片 -->
    <div class="card shadow-sm">
        <div class="card-header d-flex justify-content-between align-items-center">
            <div>
                <i class="fas fa-info-circle me-2 text-primary"></i>
                <span class="fw-bold">元数据列表</span>
            </div>
            <div class="dropdown">
                <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="fas fa-ellipsis-v"></i>
                </button>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><a class="dropdown-item" href="/web/metadata/export?format=csv"><i class="fas fa-file-csv me-2"></i>导出为CSV</a></li>
                    <li><a class="dropdown-item" href="/web/metadata/export?format=json"><i class="fas fa-file-code me-2"></i>导出为JSON</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item help-btn" href="#" data-bs-toggle="modal" data-bs-target="#helpModal"><i class="fas fa-question-circle me-2"></i>帮助</a></li>
                </ul>
            </div>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover metadata-table mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">ID</th>
                            <th>插件版本</th>
                            <th>IDE版本</th>
                            <th>操作系统</th>
                            <th>Java版本</th>
                            <th>时间</th>
                            <th class="pe-3">操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if metadata?? && metadata?size gt 0>
                            <#list metadata as item>
                                <tr>
                                    <td class="ps-3">${item.id}</td>
                                    <td>${item.pluginVersion}</td>
                                    <td>${item.ideVersion} <span class="text-muted small">(${item.ideBuild})</span></td>
                                    <td>
                                        <#assign osBadgeClass = "bg-secondary">
                                        <#if item.os?contains("Windows")>
                                            <#assign osBadgeClass = "bg-primary">
                                        <#elseif item.os?contains("Mac")>
                                            <#assign osBadgeClass = "bg-info">
                                        <#elseif item.os?contains("Linux")>
                                            <#assign osBadgeClass = "bg-success">
                                        </#if>
                                        <span class="badge ${osBadgeClass} os-badge">${item.os}</span>
                                        <span class="text-muted small">${item.osVersion}</span>
                                    </td>
                                    <td>${item.javaVersion}</td>
                                    <td>${item.formattedTime}</td>
                                    <td class="pe-3">
                                        <div class="d-flex gap-2">
                                            <button type="button" class="btn btn-sm btn-outline-info details-btn" 
                                                    data-bs-toggle="modal" 
                                                    data-bs-target="#detailsModal-${item?index}">
                                                <i class="fas fa-info-circle me-1"></i> 快速查看
                                            </button>
                                            <a href="/web/metadata/${item.id}" class="btn btn-sm btn-primary">
                                                <i class="fas fa-external-link-alt me-1"></i> 详细页面
                                            </a>
                                        </div>
                                    </td>
                                </tr>
                            </#list>
                        <#else>
                            <tr>
                                <td colspan="7" class="text-center py-4">
                                    <div class="d-flex flex-column align-items-center">
                                        <i class="fas fa-search fa-3x text-muted mb-3"></i>
                                        <h5 class="fw-bold">没有找到元数据</h5>
                                        <p class="text-muted">尝试调整筛选条件或清除筛选</p>
                                    </div>
                                </td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </div>
            
            <#-- 分页 -->
            <#if totalPages gt 1>
                <div class="d-flex justify-content-between align-items-center p-3 border-top">
                    <div class="text-muted small">
                        显示 ${((currentPage - 1) * pageSize) + 1} - ${(currentPage * pageSize > totalItems)?then(totalItems, currentPage * pageSize)} 条，共 ${totalItems} 条
                    </div>
                    <nav aria-label="Page navigation">
                        <ul class="pagination">
                            <li class="page-item <#if currentPage <= 1>disabled</#if>">
                                <a class="page-link" href="/web/metadata?page=${currentPage - 1}&pageSize=${pageSize}<#if os??>&os=${os}</#if><#if ideVersion??>&ideVersion=${ideVersion}</#if><#if pluginVersion??>&pluginVersion=${pluginVersion}</#if>" aria-label="Previous">
                                    <i class="fas fa-chevron-left"></i>
                                </a>
                            </li>
                            
                            <#list 1..totalPages as i>
                                <#if i == currentPage>
                                    <li class="page-item active"><span class="page-link">${i}</span></li>
                                <#elseif i <= 2 || i >= totalPages - 1 || (i >= currentPage - 1 && i <= currentPage + 1)>
                                    <li class="page-item"><a class="page-link" href="/web/metadata?page=${i}&pageSize=${pageSize}<#if os??>&os=${os}</#if><#if ideVersion??>&ideVersion=${ideVersion}</#if><#if pluginVersion??>&pluginVersion=${pluginVersion}</#if>">${i}</a></li>
                                <#elseif i == 3 || i == totalPages - 2>
                                    <li class="page-item disabled"><span class="page-link">...</span></li>
                                </#if>
                            </#list>
                            
                            <li class="page-item <#if currentPage >= totalPages>disabled</#if>">
                                <a class="page-link" href="/web/metadata?page=${currentPage + 1}&pageSize=${pageSize}<#if os??>&os=${os}</#if><#if ideVersion??>&ideVersion=${ideVersion}</#if><#if pluginVersion??>&pluginVersion=${pluginVersion}</#if>" aria-label="Next">
                                    <i class="fas fa-chevron-right"></i>
                                </a>
                            </li>
                        </ul>
                    </nav>
                </div>
            </#if>
        </div>
    </div>
    
    <#-- 详情模态框 -->
    <#if metadata?? && metadata?size gt 0>
        <#list metadata as item>
            <div class="modal fade" id="detailsModal-${item?index}" tabindex="-1" aria-labelledby="detailsModalLabel-${item?index}" aria-hidden="false">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="detailsModalLabel-${item?index}">
                                <i class="fas fa-info-circle me-2 text-primary"></i>元数据详情
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <div class="mb-3">
                                <span class="badge bg-primary">ID: ${item.id}</span>
                            </div>
                            <table class="table table-sm metadata-detail-table">
                                <tbody>
                                    <tr>
                                        <th>插件版本</th>
                                        <td>${item.pluginVersion}</td>
                                    </tr>
                                    <tr>
                                        <th>IDE版本</th>
                                        <td>${item.ideVersion}</td>
                                    </tr>
                                    <tr>
                                        <th>IDE构建号</th>
                                        <td>${item.ideBuild}</td>
                                    </tr>
                                    <tr>
                                        <th>操作系统</th>
                                        <td>${item.os}</td>
                                    </tr>
                                    <tr>
                                        <th>系统版本</th>
                                        <td>${item.osVersion}</td>
                                    </tr>
                                    <tr>
                                        <th>Java版本</th>
                                        <td>${item.javaVersion}</td>
                                    </tr>
                                    <tr>
                                        <th>客户端时间</th>
                                        <td>${item.formattedTime}</td>
                                    </tr>
                                    <tr>
                                        <th>接收时间</th>
                                        <td>${item.timestamp?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
                        </div>
                    </div>
                </div>
            </div>
        </#list>
    </#if>
    
    <!-- 帮助模态框 -->
    <div class="modal fade" id="helpModal" tabindex="-1" aria-labelledby="helpModalLabel" aria-hidden="false">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="helpModalLabel"><i class="fas fa-question-circle me-2 text-primary"></i>帮助信息</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <h6 class="fw-bold">如何使用筛选功能</h6>
                    <ul class="mb-4">
                        <li>使用<strong>操作系统</strong>筛选特定系统的元数据</li>
                        <li>使用<strong>IDE版本</strong>筛选特定IDE版本的元数据</li>
                        <li>使用<strong>插件版本</strong>筛选特定插件版本的元数据</li>
                        <li>使用<strong>搜索框</strong>在当前页面结果中快速查找</li>
                    </ul>
                    
                    <h6 class="fw-bold">操作系统标识</h6>
                    <div class="d-flex flex-wrap gap-2 mb-3">
                        <span class="badge bg-primary">Windows</span>
                        <span class="badge bg-info">macOS</span>
                        <span class="badge bg-success">Linux</span>
                        <span class="badge bg-secondary">其他</span>
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
    
    <script>
    document.addEventListener("DOMContentLoaded", function() {
        // 初始化提示工具
        var tooltipTriggerList = document.querySelectorAll("[data-bs-toggle=tooltip]");
        for (var i = 0; i < tooltipTriggerList.length; i++) {
            new bootstrap.Tooltip(tooltipTriggerList[i]);
        }
        
        // 搜索功能
        var searchInput = document.getElementById("searchInput");
        if(searchInput) {
            searchInput.addEventListener("keyup", function() {
                var searchTerm = this.value.toLowerCase();
                var tableRows = document.querySelectorAll(".metadata-table tbody tr");
                
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
                                        cell.innerHTML = before + '<span class="search-highlight">' + match + '</span>' + after;
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
                var osSelect = document.getElementById("os");
                var ideVersionSelect = document.getElementById("ideVersion");
                var pluginVersionSelect = document.getElementById("pluginVersion");
                var pageSizeSelect = document.getElementById("pageSize");
                
                if (osSelect) osSelect.selectedIndex = 0;
                if (ideVersionSelect) ideVersionSelect.selectedIndex = 0;
                if (pluginVersionSelect) pluginVersionSelect.selectedIndex = 0;
                if (pageSizeSelect) pageSizeSelect.selectedIndex = 0;
            });
        }
    });
    </script>
</@layout.page> 