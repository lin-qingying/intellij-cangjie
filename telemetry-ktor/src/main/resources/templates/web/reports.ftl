<#-- 统计报表模板 -->

<#assign title="CangJie 遥测管理后台 - 统计报表">
<#assign currentPage="reports">
<#assign pageTitle="统计报表">
<#assign additionalScripts="">

<#import "layout.ftl" as layout>

<@layout.page title=title currentPage=currentPage pageTitle=pageTitle additionalStyles='
<style>
    .stats-card {
        border-radius: 0.75rem;
        border: none;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        margin-bottom: 1.5rem;
        transition: transform 0.2s, box-shadow 0.2s;
    }
    
    .stats-card:hover {
        transform: translateY(-3px);
        box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
    }
    
    .stats-card .card-header {
        background: transparent;
        border-bottom: 1px solid rgba(var(--bs-body-color-rgb), .1);
        padding: 1rem 1.25rem;
        font-weight: 600;
    }
    
    .chart-container {
        position: relative;
        height: 300px;
        width: 100%;
    }
    
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
    
    .data-table {
        font-size: 0.9rem;
    }
    
    .data-table th {
        font-weight: 600;
        text-transform: uppercase;
        font-size: 0.75rem;
        letter-spacing: 0.5px;
        white-space: nowrap;
    }
    
    .data-table td {
        vertical-align: middle;
    }
    
    .category-badge {
        font-size: 0.75rem;
        padding: 0.35rem 0.65rem;
        border-radius: 0.5rem;
        font-weight: 600;
    }
    
    .summary-card {
        border-radius: 0.75rem;
        border: none;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        transition: all 0.2s;
    }
    
    .summary-card .card-body {
        padding: 1.5rem;
    }
    
    .summary-card .summary-icon {
        width: 48px;
        height: 48px;
        border-radius: 0.75rem;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 1rem;
    }
    
    .summary-card .summary-value {
        font-size: 1.75rem;
        font-weight: 700;
        margin-bottom: 0.5rem;
    }
    
    .summary-card .summary-label {
        color: var(--bs-secondary);
        font-size: 0.875rem;
    }
    
    .error-alert {
        border-radius: 0.75rem;
        border: none;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }
</style>'>
    <#if error??>
        <div class="alert alert-danger error-alert mb-4" role="alert">
            <div class="d-flex align-items-center">
                <i class="fas fa-exclamation-triangle me-3 fs-4"></i>
                <div>
                    <h5 class="mb-1">获取数据时出错</h5>
                    <div>${error}</div>
                </div>
            </div>
        </div>
    </#if>

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
                <form action="/web/reports" method="get" class="row g-3 filter-form">
                    <div class="col-md-3">
                        <label for="timeRange" class="form-label">时间范围</label>
                        <select class="form-select" id="timeRange" name="timeRange">
                            <option value="7" <#if timeRange?? && timeRange == 7>selected</#if>>最近7天</option>
                            <option value="30" <#if timeRange?? && timeRange == 30 || !timeRange??>selected</#if>>最近30天</option>
                            <option value="90" <#if timeRange?? && timeRange == 90>selected</#if>>最近90天</option>
                            <option value="180" <#if timeRange?? && timeRange == 180>selected</#if>>最近180天</option>
                            <option value="365" <#if timeRange?? && timeRange == 365>selected</#if>>最近365天</option>
                        </select>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="category" class="form-label">事件类别</label>
                        <select class="form-select" id="category" name="category">
                            <option value="">全部类别</option>
                            <#if categories?? && categories?size gt 0>
                                <#list categories as cat>
                                    <option value="${cat}" <#if selectedCategory?? && selectedCategory == cat>selected</#if>>${cat}</option>
                                </#list>
                            </#if>
                        </select>
                    </div>
                    
                    <div class="col-md-3">
                        <label for="groupBy" class="form-label">分组方式</label>
                        <select class="form-select" id="groupBy" name="groupBy">
                            <option value="day" <#if groupBy?? && groupBy == "day" || !groupBy??>selected</#if>>按天</option>
                            <option value="week" <#if groupBy?? && groupBy == "week">selected</#if>>按周</option>
                            <option value="month" <#if groupBy?? && groupBy == "month">selected</#if>>按月</option>
                        </select>
                    </div>
                    
                    <div class="col-md-12 d-flex align-items-end gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="fas fa-filter me-2"></i>应用筛选
                        </button>
                        <button type="button" id="resetFilters" class="btn btn-outline-secondary">
                            <i class="fas fa-undo me-2"></i>重置筛选
                        </button>
                        <div class="ms-auto">
                            <div class="dropdown">
                                <button class="btn btn-outline-primary dropdown-toggle" type="button" id="exportDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                                    <i class="fas fa-download me-2"></i>导出数据
                                </button>
                                <ul class="dropdown-menu" aria-labelledby="exportDropdown">
                                    <li><a class="dropdown-item" href="/web/reports/export?format=csv<#if timeRange??>&timeRange=${timeRange}</#if><#if selectedCategory??>&category=${selectedCategory}</#if>"><i class="fas fa-file-csv me-2"></i>导出为CSV</a></li>
                                    <li><a class="dropdown-item" href="/web/reports/export?format=json<#if timeRange??>&timeRange=${timeRange}</#if><#if selectedCategory??>&category=${selectedCategory}</#if>"><i class="fas fa-file-code me-2"></i>导出为JSON</a></li>
                                    <li><a class="dropdown-item" href="/web/reports/export?format=pdf<#if timeRange??>&timeRange=${timeRange}</#if><#if selectedCategory??>&category=${selectedCategory}</#if>"><i class="fas fa-file-pdf me-2"></i>导出为PDF</a></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <#if !error??>
    <!-- 统计摘要卡片 -->
    <div class="row mb-4">
        <div class="col-md-3">
            <div class="card summary-card h-100">
                <div class="card-body">
                    <div class="summary-icon bg-primary-subtle text-primary">
                        <i class="fas fa-chart-bar"></i>
                    </div>
                    <div class="summary-value" id="totalEvents">
                        <#if totalEvents??>${totalEvents}<#else>0</#if>
                    </div>
                    <div class="summary-label">总事件数</div>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card summary-card h-100">
                <div class="card-body">
                    <div class="summary-icon bg-success-subtle text-success">
                        <i class="fas fa-users"></i>
                    </div>
                    <div class="summary-value" id="uniqueUsers">
                        <#if uniqueUsers??>${uniqueUsers}<#else>0</#if>
                    </div>
                    <div class="summary-label">独立用户数</div>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card summary-card h-100">
                <div class="card-body">
                    <div class="summary-icon bg-info-subtle text-info">
                        <i class="fas fa-calendar-day"></i>
                    </div>
                    <div class="summary-value" id="dailyAverage">
                        <#if dailyAverage??>${dailyAverage}<#else>0</#if>
                    </div>
                    <div class="summary-label">日均事件数</div>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card summary-card h-100">
                <div class="card-body">
                    <div class="summary-icon bg-warning-subtle text-warning">
                        <i class="fas fa-bolt"></i>
                    </div>
                    <div class="summary-value" id="topCategory">
                        <#if topCategory??>${topCategory}<#else>-</#if>
                    </div>
                    <div class="summary-label">最常见事件类别</div>
                </div>
            </div>
        </div>
    </div>

    <!-- 图表卡片 -->
    <div class="row">
        <div class="col-md-6">
            <div class="card stats-card mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <div>
                        <i class="fas fa-chart-pie me-2 text-primary"></i>
                        <span>事件类别分布</span>
                    </div>
                    <div class="dropdown">
                        <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" id="chartTypeDropdown1" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="fas fa-chart-pie me-1"></i>饼图
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="chartTypeDropdown1">
                            <li><a class="dropdown-item chart-type" href="#" data-chart="pie" data-target="categoryChart"><i class="fas fa-chart-pie me-2"></i>饼图</a></li>
                            <li><a class="dropdown-item chart-type" href="#" data-chart="doughnut" data-target="categoryChart"><i class="fas fa-circle-notch me-2"></i>环形图</a></li>
                            <li><a class="dropdown-item chart-type" href="#" data-chart="bar" data-target="categoryChart"><i class="fas fa-chart-bar me-2"></i>柱状图</a></li>
                        </ul>
                    </div>
                </div>
                <div class="card-body">
                    <div class="chart-container">
                        <canvas id="categoryChart"></canvas>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card stats-card mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <div>
                        <i class="fas fa-chart-line me-2 text-primary"></i>
                        <span>事件趋势</span>
                    </div>
                    <div class="dropdown">
                        <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" id="chartTypeDropdown2" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="fas fa-chart-line me-1"></i>折线图
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="chartTypeDropdown2">
                            <li><a class="dropdown-item chart-type" href="#" data-chart="line" data-target="dailyChart"><i class="fas fa-chart-line me-2"></i>折线图</a></li>
                            <li><a class="dropdown-item chart-type" href="#" data-chart="bar" data-target="dailyChart"><i class="fas fa-chart-bar me-2"></i>柱状图</a></li>
                            <li><a class="dropdown-item chart-type" href="#" data-chart="area" data-target="dailyChart"><i class="fas fa-chart-area me-2"></i>面积图</a></li>
                        </ul>
                    </div>
                </div>
                <div class="card-body">
                    <div class="chart-container">
                        <canvas id="dailyChart"></canvas>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <div class="row">
        <div class="col-md-6">
            <div class="card shadow-sm mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <div>
                        <i class="fas fa-table me-2 text-primary"></i>
                        <span class="fw-bold">事件类别数据</span>
                    </div>
                    <button class="btn btn-sm btn-outline-secondary" type="button" data-bs-toggle="collapse" data-bs-target="#categoryTableCollapse" aria-expanded="true">
                        <i class="fas fa-chevron-down"></i>
                    </button>
        </div>
                <div class="collapse show" id="categoryTableCollapse">
                    <div class="card-body p-0">
            <div class="table-responsive">
                            <table class="table table-hover data-table mb-0">
                                <thead class="table-light">
                        <tr>
                                        <th class="ps-3">类别</th>
                            <th>事件数量</th>
                                        <th class="pe-3">百分比</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if eventsByCategory?? && eventsByCategory?size gt 0>
                            <#assign total = 0>
                            <#list eventsByCategory as category, count>
                                <#assign total = total + count>
                            </#list>
                            
                            <#list eventsByCategory as category, count>
                                <tr>
                                                <td class="ps-3">
                                                    <#assign badgeClass = "bg-secondary">
                                                    <#if category == "error">
                                                        <#assign badgeClass = "bg-danger">
                                                    <#elseif category == "warning">
                                                        <#assign badgeClass = "bg-warning">
                                                    <#elseif category == "info">
                                                        <#assign badgeClass = "bg-info">
                                                    <#elseif category == "success">
                                                        <#assign badgeClass = "bg-success">
                                                    </#if>
                                                    <span class="badge ${badgeClass} category-badge">${category}</span>
                                                </td>
                                    <td>${count}</td>
                                                <td class="pe-3">${((count/total)*100)?string("0.##")}%</td>
                                </tr>
                            </#list>
                        <#else>
                            <tr>
                                            <td colspan="3" class="text-center py-4">
                                                <div class="d-flex flex-column align-items-center">
                                                    <i class="fas fa-chart-bar fa-3x text-muted mb-3"></i>
                                                    <h5 class="fw-bold">没有找到事件数据</h5>
                                                    <p class="text-muted">尝试调整筛选条件或清除筛选</p>
                                                </div>
                                            </td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card shadow-sm">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <div>
                        <i class="fas fa-table me-2 text-primary"></i>
                        <span class="fw-bold">每日事件数据</span>
                    </div>
                    <button class="btn btn-sm btn-outline-secondary" type="button" data-bs-toggle="collapse" data-bs-target="#dailyTableCollapse" aria-expanded="true">
                        <i class="fas fa-chevron-down"></i>
                    </button>
                </div>
                <div class="collapse show" id="dailyTableCollapse">
                    <div class="card-body p-0">
            <div class="table-responsive">
                            <table class="table table-hover data-table mb-0">
                                <thead class="table-light">
                        <tr>
                                        <th class="ps-3">日期</th>
                                        <th class="pe-3">事件数量</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#if eventsByDay?? && eventsByDay?size gt 0>
                            <#list eventsByDay?keys?sort as date>
                                <tr>
                                                <td class="ps-3">${date}</td>
                                                <td class="pe-3">${eventsByDay[date]}</td>
                                </tr>
                            </#list>
                        <#else>
                            <tr>
                                            <td colspan="2" class="text-center py-4">
                                                <div class="d-flex flex-column align-items-center">
                                                    <i class="fas fa-calendar fa-3x text-muted mb-3"></i>
                                                    <h5 class="fw-bold">没有找到事件数据</h5>
                                                    <p class="text-muted">尝试调整筛选条件或清除筛选</p>
                                                </div>
                                            </td>
                            </tr>
                        </#if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
            </div>
        </div>
    </div>
    </#if>
    
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            <#if !error??>
            // 初始化图表
            let categoryChart = null;
            let dailyChart = null;
            
            // 类别分布图表
            const categoryCtx = document.getElementById('categoryChart').getContext('2d');
            
            <#if eventsByCategory?? && eventsByCategory?size gt 0>
                const categoryLabels = [];
                const categoryData = [];
                const backgroundColors = [
                    'rgba(255, 99, 132, 0.7)',
                    'rgba(54, 162, 235, 0.7)',
                    'rgba(255, 206, 86, 0.7)',
                    'rgba(75, 192, 192, 0.7)',
                    'rgba(153, 102, 255, 0.7)',
                    'rgba(255, 159, 64, 0.7)',
                    'rgba(199, 199, 199, 0.7)',
                    'rgba(83, 102, 255, 0.7)',
                    'rgba(40, 159, 64, 0.7)',
                    'rgba(210, 199, 199, 0.7)'
                ];
                
                <#list eventsByCategory as category, count>
                    categoryLabels.push('${category?js_string}');
                    categoryData.push(${count});
                </#list>
                
                categoryChart = new Chart(categoryCtx, {
                    type: 'pie',
                    data: {
                        labels: categoryLabels,
                        datasets: [{
                            data: categoryData,
                            backgroundColor: backgroundColors,
                            borderColor: backgroundColors.map(color => color.replace('0.7', '1')),
                            borderWidth: 1,
                            hoverOffset: 15
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                position: 'bottom',
                                labels: {
                                    padding: 20,
                                    usePointStyle: true,
                                    pointStyle: 'circle'
                                }
                            },
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        const label = context.label || '';
                                        const value = context.raw;
                                        const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                        const percentage = Math.round((value / total) * 100);
                                        return context.label + ': ' + value + ' (' + percentage + '%)';
                                    }
                                }
                            }
                        }
                    }
                });
            </#if>
            
            // 每日事件趋势图表
            const dailyCtx = document.getElementById('dailyChart').getContext('2d');
            
            <#if eventsByDay?? && eventsByDay?size gt 0>
                const dailyLabels = [];
                const dailyData = [];
                
                <#list eventsByDay?keys?sort as date>
                    dailyLabels.push('${date?js_string}');
                    dailyData.push(${eventsByDay[date]});
                </#list>
                
                dailyChart = new Chart(dailyCtx, {
                    type: 'line',
                    data: {
                        labels: dailyLabels,
                        datasets: [{
                            label: '每日事件数',
                            data: dailyData,
                            backgroundColor: 'rgba(54, 162, 235, 0.2)',
                            borderColor: 'rgba(54, 162, 235, 1)',
                            borderWidth: 2,
                            tension: 0.3,
                            pointRadius: 3,
                            pointBackgroundColor: 'rgba(54, 162, 235, 1)',
                            pointBorderColor: '#fff',
                            pointBorderWidth: 2,
                            fill: false
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    precision: 0
                                }
                            }
                        },
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                mode: 'index',
                                intersect: false
                            }
                        },
                        interaction: {
                            mode: 'nearest',
                            axis: 'x',
                            intersect: false
                        }
                    }
                });
            </#if>
            
            // 切换图表类型
            document.querySelectorAll('.chart-type').forEach(item => {
                item.addEventListener('click', function(e) {
                    e.preventDefault();
                    const chartType = this.getAttribute('data-chart');
                    const targetChart = this.getAttribute('data-target');
                    const buttonText = this.innerHTML;
                    
                    if (targetChart === 'categoryChart' && categoryChart) {
                        if (chartType === 'pie' || chartType === 'doughnut') {
                            categoryChart.config.type = chartType;
                            document.getElementById('chartTypeDropdown1').innerHTML = buttonText;
                        } else if (chartType === 'bar') {
                            categoryChart.config.type = chartType;
                            document.getElementById('chartTypeDropdown1').innerHTML = buttonText;
                            // 调整柱状图特有的选项
                            categoryChart.options.indexAxis = 'y';
                            categoryChart.options.scales.y.display = true;
                        }
                        categoryChart.update();
                    } else if (targetChart === 'dailyChart' && dailyChart) {
                        if (chartType === 'line') {
                            dailyChart.config.type = chartType;
                            dailyChart.data.datasets[0].fill = false;
                            document.getElementById('chartTypeDropdown2').innerHTML = buttonText;
                        } else if (chartType === 'bar') {
                            dailyChart.config.type = chartType;
                            dailyChart.data.datasets[0].fill = false;
                            document.getElementById('chartTypeDropdown2').innerHTML = buttonText;
                        } else if (chartType === 'area') {
                            dailyChart.config.type = 'line';
                            dailyChart.data.datasets[0].fill = true;
                            document.getElementById('chartTypeDropdown2').innerHTML = buttonText;
                        }
                        dailyChart.update();
                    }
                });
            });
            
            // 添加数字动画效果
            function animateValue(id, start, end, duration) {
                const obj = document.getElementById(id);
                if (!obj) return;
                
                let startTimestamp = null;
                const step = (timestamp) => {
                    if (!startTimestamp) startTimestamp = timestamp;
                    const progress = Math.min((timestamp - startTimestamp) / duration, 1);
                    obj.innerHTML = Math.floor(progress * (end - start) + start).toLocaleString();
                    if (progress < 1) {
                        window.requestAnimationFrame(step);
                    }
                };
                window.requestAnimationFrame(step);
            }
            
            // 启动数字动画
            <#if totalEvents??>
                animateValue('totalEvents', 0, ${totalEvents?c}, 1500);
            </#if>
            <#if uniqueUsers??>
                animateValue('uniqueUsers', 0, ${uniqueUsers?c}, 1500);
            </#if>
            <#if dailyAverage??>
                animateValue('dailyAverage', 0, ${dailyAverage?c}, 1500);
            </#if>
            </#if>
            
            // 重置筛选
            document.getElementById('resetFilters').addEventListener('click', function() {
                document.getElementById('timeRange').selectedIndex = 1; // 默认30天
                document.getElementById('category').selectedIndex = 0;
                document.getElementById('groupBy').selectedIndex = 0;
            });
        });
    </script>
</@layout.page> 