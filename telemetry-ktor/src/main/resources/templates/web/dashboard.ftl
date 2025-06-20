<#-- Dashboard template -->

<#assign title="CangJie 遥测管理后台 - 仪表盘">
<#assign currentPage="dashboard">
<#assign pageTitle="仪表盘">

<#import "layout.ftl" as layout>

<@layout.page title=title currentPage=currentPage pageTitle=pageTitle additionalStyles='
<style>
    .stat-card {
        border-radius: 1rem;
        overflow: hidden;
        transition: all 0.3s;
        border: none;
    }
    
    .stat-card:hover {
        transform: translateY(-5px);
        box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
    }
    
    .stat-card .card-body {
        padding: 1.5rem;
    }
    
    .stat-card .icon-bg {
        position: absolute;
        right: -15px;
        bottom: -15px;
        opacity: 0.15;
        font-size: 6rem;
        transform: rotate(-15deg);
        transition: all 0.3s;
    }
    
    .stat-card:hover .icon-bg {
        transform: rotate(0deg) scale(1.1);
        opacity: 0.2;
    }
    
    .stat-card .card-footer {
        background: rgba(0, 0, 0, 0.1);
        border-top: none;
        padding: 0.75rem 1.5rem;
    }
    
    .stat-card .card-footer a {
        font-weight: 500;
    }
    
    .stat-value {
        font-size: 2.2rem;
        font-weight: 700;
        margin-bottom: 0;
        line-height: 1.2;
    }
    
    .stat-label {
        text-transform: uppercase;
        letter-spacing: 1px;
        font-size: 0.85rem;
        font-weight: 600;
        margin-bottom: 0.5rem;
        opacity: 0.8;
    }
    
    .recent-table {
        font-size: 0.9rem;
    }
    
    .recent-table th {
        font-weight: 600;
        text-transform: uppercase;
        font-size: 0.8rem;
        letter-spacing: 0.5px;
    }
    
    .quick-link {
        border-radius: 0.75rem;
        font-weight: 500;
        transition: all 0.2s;
        padding: 0.75rem 1rem;
        border-width: 2px;
    }
    
    .quick-link:hover {
        transform: translateY(-2px);
        box-shadow: 0 5px 15px rgba(0, 0, 0, 0.08);
    }
    
    .user-info-card {
        border-radius: 1rem;
        overflow: hidden;
    }
    
    .user-avatar {
        width: 70px;
        height: 70px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background-color: rgba(var(--bs-primary-rgb), 0.1);
        color: var(--bs-primary);
        font-size: 2rem;
    }
    
    .user-info-list {
        border-radius: 0.75rem;
        overflow: hidden;
    }
    
    .user-info-list .list-group-item {
        padding: 0.75rem 1.25rem;
        border-left: none;
        border-right: none;
    }
    
    .user-info-list .list-group-item:first-child {
        border-top: none;
    }
    
    .user-info-list .list-group-item:last-child {
        border-bottom: none;
    }
</style>' additionalScripts='
<script>
    // 添加数字动画效果
    document.addEventListener("DOMContentLoaded", function() {
        const statValues = document.querySelectorAll(".stat-value");
        
        statValues.forEach(element => {
            const finalValue = parseInt(element.getAttribute("data-value") || "0");
            animateValue(element, 0, finalValue, 1500);
        });
        
        function animateValue(obj, start, end, duration) {
            let startTimestamp = null;
            const step = (timestamp) => {
                if (!startTimestamp) startTimestamp = timestamp;
                const progress = Math.min((timestamp - startTimestamp) / duration, 1);
                const currentValue = Math.floor(progress * (end - start) + start);
                obj.innerHTML = currentValue.toLocaleString();
                if (progress < 1) {
                    window.requestAnimationFrame(step);
                }
            };
            window.requestAnimationFrame(step);
        }
    });
</script>'>
    <!-- 统计卡片 -->
    <div class="row g-4 mb-4">
        <div class="col-md-6 col-lg-3">
            <div class="card stat-card bg-primary text-white h-100">
                <div class="card-body position-relative">
                    <p class="stat-label">总事件数</p>
                    <h2 class="stat-value" data-value="${(totalEvents??)?then(totalEvents?c, '0')}">${(totalEvents??)?then(totalEvents, 0)}</h2>
                    <i class="fas fa-chart-line icon-bg"></i>
                </div>
                <div class="card-footer d-flex align-items-center justify-content-between">
                    <a href="/web/events" class="text-white text-decoration-none stretched-link">查看详情</a>
                    <i class="fas fa-arrow-right"></i>
                </div>
            </div>
        </div>
        
        <div class="col-md-6 col-lg-3">
            <div class="card stat-card bg-success text-white h-100">
                <div class="card-body position-relative">
                    <p class="stat-label">总元数据数</p>
                    <h2 class="stat-value" data-value="${(totalMetadata??)?then(totalMetadata?c, '0')}">${(totalMetadata??)?then(totalMetadata, 0)}</h2>
                    <i class="fas fa-database icon-bg"></i>
                </div>
                <div class="card-footer d-flex align-items-center justify-content-between">
                    <a href="/web/metadata" class="text-white text-decoration-none stretched-link">查看详情</a>
                    <i class="fas fa-arrow-right"></i>
                </div>
            </div>
        </div>
        
        <div class="col-md-6 col-lg-3">
            <div class="card stat-card bg-warning text-white h-100">
                <div class="card-body position-relative">
                    <p class="stat-label">统计报表</p>
<#--                    <div class="d-flex align-items-center">-->
<#--                        <h2 class="stat-value me-2"><i class="fas fa-chart-bar"></i></h2>-->
<#--                        <span class="fs-5 fw-semibold">分析</span>-->
<#--                    </div>-->
                    <i class="fas fa-chart-pie icon-bg"></i>
                </div>
                <div class="card-footer d-flex align-items-center justify-content-between">
                    <a href="/web/reports" class="text-white text-decoration-none stretched-link">查看报表</a>
                    <i class="fas fa-arrow-right"></i>
                </div>
            </div>
        </div>
        
        <div class="col-md-6 col-lg-3">
            <div class="card stat-card bg-danger text-white h-100">
                <div class="card-body position-relative">
                    <p class="stat-label">系统设置</p>
<#--                    <div class="d-flex align-items-center">-->
<#--                        <h2 class="stat-value me-2"><i class="fas fa-cog"></i></h2>-->
<#--                        <span class="fs-5 fw-semibold">配置</span>-->
<#--                    </div>-->
                    <i class="fas fa-wrench icon-bg"></i>
                </div>
                <div class="card-footer d-flex align-items-center justify-content-between">
                    <a href="/web/settings" class="text-white text-decoration-none stretched-link">管理设置</a>
                    <i class="fas fa-arrow-right"></i>
                </div>
            </div>
        </div>
    </div>
    
    <div class="row g-4">
        <!-- 最近元数据 -->
        <div class="col-lg-7">
            <div class="card shadow-sm h-100">
                <div class="card-header bg-transparent d-flex justify-content-between align-items-center">
                    <div>
                        <i class="fas fa-history me-2 text-primary"></i>
                        <span class="fw-bold">最近元数据</span>
                    </div>
                    <a href="/web/metadata" class="btn btn-sm btn-outline-primary">
                        查看全部 <i class="fas fa-arrow-right ms-1"></i>
                    </a>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover recent-table mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th class="ps-3">插件版本</th>
                                    <th>IDE版本</th>
                                    <th>操作系统</th>
                                    <th class="pe-3">时间</th>
                                </tr>
                            </thead>
                            <tbody>
                                <#if recentMetadata?? && recentMetadata?size gt 0>
                                    <#list recentMetadata as item>
                                        <tr>
                                            <td class="ps-3">${item.pluginVersion!"未知版本"}</td>
                                            <td>${item.ideVersion!"未知IDE"}</td>
                                            <td>${item.os!""} ${item.osVersion!""}</td>
                                            <td class="pe-3">${item.formattedTime!"未知时间"}</td>
                                        </tr>
                                    </#list>
                                <#else>
                                    <tr>
                                        <td colspan="4" class="text-center py-4">没有找到元数据</td>
                                    </tr>
                                </#if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 右侧面板 -->
        <div class="col-lg-5">
            <!-- 管理员信息 -->
            <div class="card shadow-sm mb-4 user-info-card">
                <div class="card-header bg-transparent">
                    <i class="fas fa-user-circle me-2 text-primary"></i>
                    <span class="fw-bold">管理员信息</span>
                </div>
                <div class="card-body">
                    <#if user??>
                        <div class="d-flex align-items-center mb-4">
                            <div class="user-avatar me-3">
                                <i class="fas fa-user"></i>
                            </div>
                            <div>
                                <h5 class="fw-bold mb-1">${user.username}</h5>
                                <span class="badge bg-primary">${user.role}</span>
                            </div>
                        </div>
                        
                        <ul class="list-group user-info-list">
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                <div>
                                    <i class="fas fa-clock text-muted me-2"></i>
                                    <span>上次登录时间</span>
                                </div>
                                <span class="badge bg-light text-dark">${user.lastLoginTime!'-'}</span>
                            </li>
                            <li class="list-group-item d-flex justify-content-between align-items-center">
                                <div>
                                    <i class="fas fa-calendar-alt text-muted me-2"></i>
                                    <span>创建时间</span>
                                </div>
                                <span class="badge bg-light text-dark">${user.createdAt!'-'}</span>
                            </li>
                        </ul>
                    <#else>
                        <div class="alert alert-warning">
                            <i class="fas fa-exclamation-triangle me-2"></i>
                            无法获取管理员信息
                        </div>
                    </#if>
                </div>
            </div>
            
            <!-- 快速链接 -->
            <div class="card shadow-sm">
                <div class="card-header bg-transparent">
                    <i class="fas fa-link me-2 text-primary"></i>
                    <span class="fw-bold">快速链接</span>
                </div>
                <div class="card-body">
                    <div class="row g-3">
                        <div class="col-6">
                            <a href="/web/events" class="btn btn-outline-primary quick-link w-100">
                                <i class="fas fa-list me-2"></i> 事件列表
                            </a>
                        </div>
                        <div class="col-6">
                            <a href="/web/metadata" class="btn btn-outline-success quick-link w-100">
                                <i class="fas fa-info-circle me-2"></i> 元数据
                            </a>
                        </div>
                        <div class="col-6">
                            <a href="/web/reports" class="btn btn-outline-warning quick-link w-100">
                                <i class="fas fa-chart-bar me-2"></i> 统计报表
                            </a>
                        </div>
                        <div class="col-6">
                            <a href="/web/settings" class="btn btn-outline-danger quick-link w-100">
                                <i class="fas fa-cog me-2"></i> 系统设置
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</@layout.page> 