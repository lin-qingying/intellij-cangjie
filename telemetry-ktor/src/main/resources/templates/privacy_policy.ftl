<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CangJie 插件隐私政策</title>
    <style>
        :root {
            --primary-color: #3498db;
            --secondary-color: #2c3e50;
            --accent-color: #e74c3c;
            --text-color: #333;
            --light-gray: #f5f5f5;
            --border-color: #eee;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: var(--text-color);
            max-width: 900px;
            margin: 0 auto;
            padding: 20px;
            background-color: #fff;
        }

        .container {
            box-shadow: 0 0 20px rgba(0, 0, 0, 0.05);
            border-radius: 8px;
            padding: 30px;
            background-color: #fff;
        }

        header {
            text-align: center;
            margin-bottom: 30px;
        }

        h1 {
            color: var(--secondary-color);
            border-bottom: 2px solid var(--border-color);
            padding-bottom: 15px;
            font-size: 2.2em;
        }

        .updated-date {
            background-color: var(--light-gray);
            padding: 8px 15px;
            border-radius: 20px;
            display: inline-block;
            margin-bottom: 20px;
            font-size: 0.9em;
        }

        h2 {
            color: var(--primary-color);
            margin-top: 40px;
            font-size: 1.6em;
            border-left: 4px solid var(--primary-color);
            padding-left: 15px;
        }

        p {
            margin-bottom: 20px;
            font-size: 1.05em;
        }

        ul, ol {
            margin-bottom: 25px;
            padding-left: 25px;
        }

        li {
            margin-bottom: 12px;
        }

        .highlight {
            background-color: #fffde7;
            padding: 15px;
            border-left: 4px solid #ffd600;
            margin: 20px 0;
            border-radius: 4px;
        }

        .section {
            margin-bottom: 40px;
            padding-bottom: 20px;
            border-bottom: 1px solid var(--border-color);
        }

        .section:last-child {
            border-bottom: none;
        }

        .footer {
            margin-top: 60px;
            padding-top: 20px;
            border-top: 1px solid var(--border-color);
            font-size: 0.9em;
            color: #7f8c8d;
            text-align: center;
        }

        a {
            color: var(--primary-color);
            text-decoration: none;
            transition: color 0.3s;
        }

        a:hover {
            color: var(--accent-color);
            text-decoration: underline;
        }

        .contact-info {
            background-color: var(--light-gray);
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }

        .contact-button {
            display: inline-block;
            background-color: var(--primary-color);
            color: white;
            padding: 10px 20px;
            border-radius: 4px;
            margin-top: 10px;
            transition: background-color 0.3s;
        }

        .contact-button:hover {
            background-color: var(--accent-color);
            text-decoration: none;
            color: white;
        }

        @media (max-width: 768px) {
            body {
                padding: 15px;
            }

            .container {
                padding: 20px;
            }

            h1 {
                font-size: 1.8em;
            }

            h2 {
                font-size: 1.4em;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <header>
        <h1>Intellij-CangJie 插件隐私政策</h1>
        <div class="updated-date">最后更新日期: ${lastUpdated}</div>
    </header>

    <div class="section">
        <p>
            感谢您使用本插件。本隐私政策旨在帮助您了解我如何收集、使用和保护您的个人信息。我非常重视您的隐私，并致力于透明地告知您数据处理实践。</p>

        <div class="highlight">
            <p>
                请仔细阅读本隐私政策，以便了解在使用插件时，您的数据将如何被处理。如果您不同意本政策中的任何条款，请关闭遥测开关。</p>
        </div>
    </div>

    <div class="section">
        <h2>1. 信息收集</h2>
        <p>我收集以下类型的信息：</p>
        <ul>
            <li><strong>使用数据</strong>：我收集有关您如何使用插件的遥测数据，包括功能使用频率、操作时间等。这些数据帮助我了解哪些功能最受欢迎，哪些功能可能需要改进。
            </li>
            <li><strong>环境信息</strong>：为了提供更好的服务，我可能会收集您的 IDE 版本、操作系统版本和 Java
                版本等技术信息。这些信息帮助我诊断兼容性问题并优化插件性能。
            </li>
            <li><strong>系统标识符</strong>：我使用随机生成的匿名标识符来区分不同的用户，这不会识别您的个人身份。这有助于我了解用户群体规模和使用模式。
            </li>
            <li><strong>错误报告</strong>：当插件遇到错误时，我可能会收集错误日志和相关环境信息，以帮助我诊断和修复问题。
            </li>
        </ul>

        <p><strong>我不收集的信息：</strong></p>
        <ul>
            <li>您的个人身份信息（如姓名、电子邮件地址等）</li>
            <li>您的代码内容或项目结构</li>
            <li>您的密码或认证凭据</li>
            <li>您的 IP 地址或精确位置</li>
        </ul>
    </div>

    <div class="section">
        <h2>2. 信息使用</h2>
        <p>我使用收集的信息：</p>
        <ul>
            <li>改进插件的功能和性能</li>
            <li>分析用户使用模式，以便做出更好的开发决策</li>
            <li>诊断和修复问题</li>
            <li>提供更好的用户体验</li>
            <li>了解用户需求，规划未来的功能开发</li>
        </ul>

        <p>所有数据分析均以汇总和匿名的形式进行，不会针对特定个人进行分析。</p>
    </div>

    <div class="section">
        <h2>3. 信息共享</h2>
        <p>
            我不会将您的个人信息出售、交易或转让给第三方。我可能会分享匿名的统计数据，但这些数据不会包含任何可识别个人身份的信息。</p>

        <p>在以下情况下，我可能会共享某些信息：</p>
        <ul>
            <li>为了遵守法律法规的要求</li>
            <li>为了保护权利、财产或安全</li>
        </ul>
    </div>

    <div class="section">
        <h2>4. 数据安全</h2>
        <p>我采取合理的安全措施保护您的信息，防止未经授权的访问、使用或披露。所有收集的遥测数据都经过加密传输。</p>

        <p>我的安全措施包括：</p>
        <ul>
            <li>使用 HTTPS 加密传输所有数据</li>
            <li>定期审查和更新安全实践</li>
            <li>限制对数据的访问，仅授权人员可以访问</li>
            <li>对收集的数据进行匿名化处理</li>
        </ul>

        <p>尽管我努力保护您的信息，但请注意，互联网传输方式不能保证100%的安全性。</p>
    </div>

    <div class="section">
        <h2>5. 用户选择</h2>
        <p>您可以随时在插件的设置中选择禁用遥测数据收集。禁用后，将不再收集任何使用数据。</p>

        <p>禁用遥测数据收集的步骤：</p>
        <ol>
            <li>打开 IntelliJ IDEA</li>
            <li>进入 Settings/Preferences</li>
            <li>找到 Tools 部分下的遥测设置(CangJie Telemetry)</li>
            <li>取消选中"允许收集使用数据"选项</li>
            <li>点击 Apply 或 OK 保存设置</li>
        </ol>
    </div>

    <div class="section">
        <h2>6. 数据保留</h2>
        <p>
            我会在合理必要的时间内保留收集的数据，以实现本隐私政策中描述的目的。一般情况下，遥测数据会保留不超过12个月。当数据不再需要时，我会安全地删除或匿名化处理这些数据。</p>
    </div>

    <div class="section">
        <h2>7. 政策变更</h2>
        <p>
            我可能会不时更新本隐私政策。任何变更都会在此页面上发布，重大变更也会通过插件通知您。我鼓励您定期查看本政策，以了解我如何保护您的信息。</p>

        <p>政策更新后，继续使用插件即表示您同意新的条款。</p>
    </div>

    <div class="section">
        <h2>8. 联系我</h2>
        <div class="contact-info">
            <p>如果您对本隐私政策有任何疑问或建议，请通过以下方式联系我：</p>
            <p><strong>电子邮件</strong>: <a href="mailto:contact@cangnova.cn">contact@cangnova.cn</a></p>
            <p><strong>Gitee</strong>: <a href="https://gitee.com/Lin_Qing_Ying/intellij-cangjie" target="_blank">https://gitee.com/Lin_Qing_Ying/intellij-cangjie</a>
            </p>
            <p><strong>GitCode</strong>: <a href="https://gitcode.com/OpenCangjieCommunity/intellij-cangjie"
                                            target="_blank">https://gitcode.com/OpenCangjieCommunity/intellij-cangjie</a>
            </p>

            <p><strong>GitHub</strong>: <a href="https://github.com/lin-qingying/intellij-cangjie" target="_blank">https://github.com/lin-qingying/intellij-cangjie</a>
            </p>
            <p><strong>问题反馈</strong>: <a href="https://gitee.com/Lin_Qing_Ying/intellij-cangjie/issues"
                                             target="_blank" class="contact-button">提交问题或建议</a></p>
        </div>
    </div>

    <div class="footer">
        <p>&copy; ${currentYear} CangNova. 保留所有权利。</p>
        <p>本插件是一个开源项目，遵循 Apache 2.0 许可证。</p>
    </div>
</div>
</body>
</html> 