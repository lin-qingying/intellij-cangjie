<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CangJie 遥测管理后台 - 登录</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Font Awesome -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --primary-color: #4361ee;
            --secondary-color: #3f37c9;
            --accent-color: #4895ef;
            --background-color: #f8f9fa;
            --text-color: #212529;
            --text-muted: #6c757d;
            --border-radius: 1rem;
        }
        
        body {
            height: 100vh;
            background-color: var(--background-color);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            overflow: hidden;
        }
        
        .login-container {
            height: 100vh;
            max-width: 100%;
            display: flex;
            overflow: hidden;
        }
        
        .login-left {
            flex: 1;
            background: linear-gradient(135deg, #4361ee, #3a0ca3);
            color: white;
            display: flex;
            flex-direction: column;
            justify-content: center;
            padding: 2rem 4rem;
            position: relative;
            overflow: hidden;
            display: none;
        }
        
        .login-right {
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 2rem;
            background-color: white;
        }
        
        .login-form-container {
            width: 100%;
            max-width: 400px;
        }
        
        .login-logo {
            margin-bottom: 2rem;
            text-align: center;
        }
        
        .login-logo i {
            font-size: 2.5rem;
            color: var(--primary-color);
            margin-bottom: 1rem;
        }
        
        .login-logo h1 {
            font-size: 1.8rem;
            font-weight: 700;
            margin: 0;
            color: var(--text-color);
        }
        
        .login-logo p {
            color: var(--text-muted);
            margin-top: 0.5rem;
        }
        
        .login-form {
            background-color: white;
            border-radius: var(--border-radius);
            padding: 2rem;
        }
        
        .form-floating {
            margin-bottom: 1.25rem;
        }
        
        .form-floating .form-control {
            border-radius: 0.5rem;
            border: 1px solid #dee2e6;
            padding: 1rem 0.75rem;
        }
        
        .form-floating .form-control:focus {
            box-shadow: 0 0 0 0.25rem rgba(67, 97, 238, 0.25);
            border-color: var(--primary-color);
        }
        
        .btn-login {
            background-color: var(--primary-color);
            border-color: var(--primary-color);
            border-radius: 0.5rem;
            width: 100%;
            padding: 0.75rem;
            font-size: 1rem;
            font-weight: 500;
            transition: all 0.3s ease;
        }
        
        .btn-login:hover {
            background-color: var(--secondary-color);
            border-color: var(--secondary-color);
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(67, 97, 238, 0.3);
        }
        
        .alert {
            margin-bottom: 1.5rem;
            border-radius: 0.5rem;
            border: none;
        }
        
        .copyright {
            margin-top: 2rem;
            color: var(--text-muted);
            text-align: center;
        }
        
        .shape {
            position: absolute;
            opacity: 0.1;
            border-radius: 50%;
            background-color: white;
        }
        
        .shape-1 {
            width: 300px;
            height: 300px;
            top: -100px;
            left: -100px;
        }
        
        .shape-2 {
            width: 200px;
            height: 200px;
            bottom: -50px;
            right: -50px;
        }
        
        @media (min-width: 992px) {
            .login-left {
                display: flex;
            }
            
            .login-form-container {
                padding: 0 1rem;
            }
        }
        
        .welcome-text {
            position: relative;
            z-index: 2;
        }
        
        .welcome-text h2 {
            font-size: 2.5rem;
            font-weight: 700;
            margin-bottom: 1.5rem;
        }
        
        .welcome-text p {
            font-size: 1.1rem;
            opacity: 0.9;
            margin-bottom: 2rem;
            line-height: 1.6;
        }
        
        .feature-list {
            padding-left: 0;
            list-style: none;
        }
        
        .feature-list li {
            margin-bottom: 1rem;
            display: flex;
            align-items: center;
        }
        
        .feature-list li i {
            margin-right: 0.75rem;
            background-color: rgba(255, 255, 255, 0.2);
            width: 30px;
            height: 30px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 0.8rem;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <!-- 左侧介绍区域 -->
        <div class="login-left">
            <div class="shape shape-1"></div>
            <div class="shape shape-2"></div>
            
            <div class="welcome-text">
                <h2>欢迎使用 CangJie 遥测系统</h2>
                <p>CangJie 遥测系统提供全面的插件使用数据分析，帮助您了解用户行为和改进产品体验。</p>
                
                <ul class="feature-list">
                    <li>
                        <i class="fas fa-chart-line"></i>
                        <span>实时数据分析与可视化</span>
                    </li>
                    <li>
                        <i class="fas fa-users"></i>
                        <span>用户行为与使用模式追踪</span>
                    </li>
                    <li>
                        <i class="fas fa-shield-alt"></i>
                        <span>安全的数据收集与存储</span>
                    </li>
                    <li>
                        <i class="fas fa-tachometer-alt"></i>
                        <span>性能监控与问题诊断</span>
                    </li>
                </ul>
            </div>
        </div>
        
        <!-- 右侧登录区域 -->
        <div class="login-right">
            <div class="login-form-container">
                <div class="login-logo">
                    <i class="fas fa-chart-line"></i>
                    <h1>CangJie 遥测系统</h1>
                    <p>管理员登录</p>
                </div>
                
                <#if error??>
                    <div class="alert alert-danger" role="alert">
                        <#if error == "invalid_credentials">
                            <i class="fas fa-exclamation-circle me-2"></i>用户名或密码错误
                        <#else>
                            <i class="fas fa-exclamation-circle me-2"></i>${error}
                        </#if>
                    </div>
                </#if>
                
                <form action="/web/login" method="post" class="login-form">
                    <div class="form-floating">
                        <input type="text" class="form-control" id="username" name="username" placeholder="用户名" required>
                        <label for="username">用户名</label>
                    </div>
                    <div class="form-floating">
                        <input type="password" class="form-control" id="password" name="password" placeholder="密码" required>
                        <label for="password">密码</label>
                    </div>
                    <div class="d-grid gap-2 mt-4">
                        <button class="btn btn-primary btn-login" type="submit">
                            <i class="fas fa-sign-in-alt me-2"></i>登录
                        </button>
                    </div>
                </form>
                
                <div class="copyright">
                    <small>&copy; ${.now?string('yyyy')} CangJie Telemetry. All rights reserved.</small>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html> 