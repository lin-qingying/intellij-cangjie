<#macro page title="CangJie 遥测管理后台" currentPage="" pageTitle="管理后台" additionalStyles="" additionalScripts="">
    <!DOCTYPE html>
    <html lang="zh-CN" data-bs-theme="light">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title}</title>
        <!-- Bootstrap 5.3 CSS -->
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
        <!-- Font Awesome 6 -->
        <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" rel="stylesheet">
        <!-- Custom CSS -->
        <style>
            :root {
                --sidebar-width: 280px;
                --header-height: 60px;
                --transition-speed: 0.3s;
            }

            body {
                font-family: 'Inter', 'Segoe UI', system-ui, -apple-system, sans-serif;
                min-height: 100vh;
                overflow-x: hidden;
            }

            /* 顶部导航栏 */
            .top-navbar {
                height: var(--header-height);
                z-index: 1030;
                backdrop-filter: blur(10px);
                -webkit-backdrop-filter: blur(10px);
                border-bottom: 1px solid rgba(var(--bs-body-color-rgb), .1);
            }

            /* 侧边栏 */
            .sidebar {
                position: fixed;
                top: var(--header-height);
                left: 0;
                width: var(--sidebar-width);
                height: calc(100vh - var(--header-height));
                z-index: 1020;
                transition: transform var(--transition-speed) ease;
                overflow-y: auto;
                scrollbar-width: thin;
            }

            .sidebar-header {
                padding: 1.2rem 1.5rem;
                display: flex;
                align-items: center;
            }

            .sidebar-header h3 {
                margin: 0;
                font-size: 1.25rem;
                font-weight: 600;
            }

            .sidebar-menu {
                padding: 0.5rem 0;
                list-style: none;
                margin: 0;
            }

            .sidebar-menu li {
                margin: 0.25rem 0.75rem;
            }

            .sidebar-menu li a {
                color: var(--bs-body-color);
                text-decoration: none;
                padding: 0.75rem 1rem;
                display: flex;
                align-items: center;
                border-radius: 0.5rem;
                transition: all var(--transition-speed);
            }

            .sidebar-menu li a:hover {
                background-color: rgba(var(--bs-primary-rgb), 0.1);
            }

            .sidebar-menu li a.active {
                background-color: var(--bs-primary);
                color: #fff;
                box-shadow: 0 2px 8px rgba(var(--bs-primary-rgb), 0.3);
            }

            .sidebar-menu li a i {
                margin-right: 0.75rem;
                width: 1.25rem;
                text-align: center;
                font-size: 1.1rem;
            }

            /* 主内容区 */
            .main-content {
                margin-left: var(--sidebar-width);
                padding: 1.5rem;
                min-height: calc(100vh - var(--header-height));
                transition: margin-left var(--transition-speed) ease;
            }

            .content-header {
                margin-bottom: 1.5rem;
                padding-bottom: 1rem;
                border-bottom: 1px solid rgba(var(--bs-body-color-rgb), .1);
            }

            .content-header h1 {
                margin: 0;
                font-size: 1.75rem;
                font-weight: 600;
            }

            .card {
                border: none;
                border-radius: 0.75rem;
                box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
                transition: transform 0.2s, box-shadow 0.2s;
                margin-bottom: 1.5rem;
                overflow: hidden;
            }

            .card:hover {
                transform: translateY(-3px);
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
            }

            .card-header {
                background-color: transparent;
                border-bottom: 1px solid rgba(var(--bs-body-color-rgb), .1);
                font-weight: 600;
                padding: 1rem 1.25rem;
            }

            /* 页脚 */
            .footer {
                margin-left: var(--sidebar-width);
                padding: 1rem 1.5rem;
                text-align: center;
                font-size: 0.875rem;
                color: var(--bs-secondary-color);
                border-top: 1px solid rgba(var(--bs-body-color-rgb), .1);
                transition: margin-left var(--transition-speed) ease;
            }

            /* 响应式设计 */
            @media (max-width: 992px) {
                .sidebar {
                    transform: translateX(-100%);
                }

                .sidebar.show {
                    transform: translateX(0);
                }

                .main-content, .footer {
                    margin-left: 0;
                }
            }

            /* 深色模式切换按钮 */
            .theme-toggle {
                cursor: pointer;
                width: 40px;
                height: 40px;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 50%;
                transition: background-color 0.2s;
            }

            .theme-toggle:hover {
                background-color: rgba(var(--bs-body-color-rgb), .1);
            }

            /* 自定义滚动条 */
            ::-webkit-scrollbar {
                width: 6px;
                height: 6px;
            }

            ::-webkit-scrollbar-track {
                background: transparent;
            }

            ::-webkit-scrollbar-thumb {
                background: rgba(var(--bs-body-color-rgb), .2);
                border-radius: 3px;
            }

            ::-webkit-scrollbar-thumb:hover {
                background: rgba(var(--bs-body-color-rgb), .3);
            }
        </style>
        ${additionalStyles}
    </head>
    <body>
    <!-- 顶部导航栏 -->
    <nav class="navbar navbar-expand-lg top-navbar sticky-top bg-body-tertiary">
        <div class="container-fluid">
            <button class="btn btn-link d-lg-none me-2 p-1" id="toggleSidebar">
                <i class="fas fa-bars"></i>
            </button>

            <a class="navbar-brand d-flex align-items-center" href="/web/dashboard">
                <i class="fas fa-chart-line me-2 text-primary"></i>
                <span class="fw-bold">CangJie 遥测</span>
            </a>

            <div class="d-flex align-items-center ms-auto">
                <div class="theme-toggle me-3" id="themeToggle">
                    <i class="fas fa-moon"></i>
                </div>

                <div class="dropdown">
                    <a class="btn btn-outline-secondary dropdown-toggle d-flex align-items-center" href="#"
                       role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fas fa-user-circle me-2"></i>
                        <span>管理员</span>
                    </a>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <li><a class="dropdown-item" href="/web/settings"><i class="fas fa-cog me-2"></i>设置</a></li>
                        <li>
                            <hr class="dropdown-divider">
                        </li>
                        <li><a class="dropdown-item" href="/web/logout"><i class="fas fa-sign-out-alt me-2"></i>退出登录</a>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </nav>

    <!-- 侧边栏 -->
    <div class="sidebar bg-body-tertiary" id="sidebar">
        <ul class="sidebar-menu">
            <li>
                <a href="/web/dashboard" class="${(currentPage == 'dashboard')?then('active', '')}">
                    <i class="fas fa-tachometer-alt"></i> 仪表盘
                </a>
            </li>
            <li>
                <a href="/web/events" class="${(currentPage == 'events')?then('active', '')}">
                    <i class="fas fa-list"></i> 事件列表
                </a>
            </li>
            <li>
                <a href="/web/metadata" class="${(currentPage == 'metadata')?then('active', '')}">
                    <i class="fas fa-info-circle"></i> 元数据
                </a>
            </li>
            <li>
                <a href="/web/reports" class="${(currentPage == 'reports')?then('active', '')}">
                    <i class="fas fa-chart-bar"></i> 统计报表
                </a>
            </li>
            <li>
                <a href="/web/users" class="${(currentPage == 'users')?then('active', '')}">
                    <i class="fas fa-users"></i> 用户管理
                </a>
            </li>
            <li>
                <a href="/web/settings" class="${(currentPage == 'settings')?then('active', '')}">
                    <i class="fas fa-cog"></i> 系统设置
                </a>
            </li>
            <li>
                <a href="/privacy-policy"  >
             隐私政策
                </a>
            </li>
        </ul>
    </div>

    <!-- 主内容区 -->
    <div class="main-content" id="content">
        <div class="content-header d-flex justify-content-between align-items-center">
            <h1>${pageTitle}</h1>
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb mb-0">
                    <li class="breadcrumb-item"><a href="/web/dashboard">首页</a></li>
                    <li class="breadcrumb-item active" aria-current="page">${pageTitle}</li>
                </ol>
            </nav>
        </div>

        <#nested>
    </div>

    <!-- 页脚 -->
    <footer class="footer" id="footer">
        <p class="mb-0">&copy; ${.now?string('yyyy')} CangJie Telemetry. 版本 1.0.0</p>
    </footer>

    <!-- Bootstrap 5.3 JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
    <!-- Chart.js -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <script>
        // 侧边栏切换
        document.getElementById('toggleSidebar').addEventListener('click', function () {
            document.getElementById('sidebar').classList.toggle('show');
        });

        // 深色/浅色模式切换
        const themeToggle = document.getElementById('themeToggle');
        const htmlElement = document.documentElement;
        const currentTheme = localStorage.getItem('theme') || 'light';

        // 初始化主题
        setTheme(currentTheme);

        themeToggle.addEventListener('click', function () {
            const currentTheme = htmlElement.getAttribute('data-bs-theme');
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            setTheme(newTheme);
            localStorage.setItem('theme', newTheme);
        });

        function setTheme(theme) {
            htmlElement.setAttribute('data-bs-theme', theme);
            themeToggle.innerHTML = theme === 'dark'
                ? '<i class="fas fa-sun"></i>'
                : '<i class="fas fa-moon"></i>';
        }

        // 在大屏幕下点击内容区域不会关闭侧边栏
        document.getElementById('content').addEventListener('click', function () {
            if (window.innerWidth < 992) {
                document.getElementById('sidebar').classList.remove('show');
            }
        });

        // 添加响应式处理
        window.addEventListener('resize', function () {
            if (window.innerWidth >= 992) {
                document.getElementById('sidebar').classList.remove('show');
            }
        });
    </script>

    ${additionalScripts}
    </body>
    </html>
</#macro> 