# Cangjie Language Plugin for IntelliJ Platform

## 适用于 IntelliJ 平台的仓颉语言插件

从最新版本向下支持4个版本  

[从 JetBrains Marketplace 获取](https://plugins.jetbrains.com/plugin/26907-cangjie)

### 仓库

[码云 ![star](https://gitee.com/Lin_Qing_Ying/intellij-cangjie/badge/star.svg?theme=dark)](https://gitee.com/Lin_Qing_Ying/intellij-cangjie)

[GitCode Cangjie-SIG ![star](https://gitcode.com/Cangjie-SIG/intellij-cangjie/star/badge.svg)](https://gitcode.com/Cangjie-SIG/intellij-cangjie)

[GitCode 开源仓颉第三方开发者社区 ![star](https://gitcode.com/OpenCangjieCommunity/intellij-cangjie/star/badge.svg)](https://gitcode.com/OpenCangjieCommunity/intellij-cangjie)

[CangNova 基于 IntelliJ 集成的 IDE ![star](https://gitcode.com/OpenCangjieCommunity/CangNova/star/badge.svg)](https://gitcode.com/OpenCangjieCommunity/CangNova)

---

## 安装和使用

在 IntelliJ 平台的插件界面，搜索 **CangJie** 安装即可。

![img_2.png](https://gitee.com/Lin_Qing_Ying/intellij-cangjie/raw/analyze/img/img_2.png)

要创建项目，请使用 **CangJie** 模板。

---

## 支持的功能

| 功能    | 状态 | 说明                    |
|-------|----|-----------------------|
| 语法解析  | ✓  | 支持完整语法解析，包括类、函数、变量声明等 |
| 语法高亮  | ✓  | 基本词法高亮                |
| 代码补全  | ±  | 部分支持，持续完善中            |
| 代码格式化 | ✓  | 支持                    |
| 调试支持  | ✓  | 支持 DAP 调试和 lldb 调试    |
| 运行目标  | ±  | 部分支持                  |
| LSP   | ✓  | 基于 LSP4IJ 实现          |
| 项目管理  | ✓  | 支持 cjpm 项目解析和依赖管理     |
| 工作区支持 | ✓  | 支持多模块工作区项目            |

说明：
- ✓ 表示已实现
- ± 表示部分支持
- \- 表示计划支持

---

## 项目架构

详细项目结构请参阅 [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)。

**技术栈：**
- [Kotlin](https://kotlinlang.org/) 2.2.0 + [Gradle](https://gradle.org/) Kotlin DSL
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html) 

---

## 开发指南

### 构建项目

```bash
# 完整构建
./gradlew build

# 在 IDE 沙箱中运行插件
./gradlew :plugin:runIde

# 构建插件分发包
./gradlew :plugin:buildPlugin
```

 

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :psi:test
```

### 多版本支持

```bash
# 编译特定 IDE 版本
./gradlew build -PplatformVersion=242
./gradlew build -PplatformVersion=253
```

---

## 贡献

如果您发现任何问题或缺少功能，欢迎您为插件做出贡献。

### 如何贡献

1. Fork 本仓库
2. 创建您的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

您可以在许可证条款下自由使用、修改和分发本软件。该许可证允许：
- 商业使用
- 修改
- 分发
- 专利使用
- 私人使用

完整许可证详情请参阅 [LICENSE](LICENSE) 文件或访问 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)。

---
### 打赏支持
如果对您有帮助，请捐赠以表支持，谢谢,捐赠时请备注,并留下id或姓名，我会添加到下方捐赠列表中
<br> 
<img alt="a9777a62426b943b1a810bf0468ac4d.jpg"   src="./img/a9777a62426b943b1a810bf0468ac4d.jpg" width="200"/>
<br> 
<img alt="1e42688904365c08bfbf4c21f196c01.jpg"   src="./img/1e42688904365c08bfbf4c21f196c01.jpg" width="200"/>
<br>
<b>支持名单</b>
<br>


| ID                                          | 金额  |
|---------------------------------------------|-----|
| [@daitougege](https://gitee.com/daitougege) | 101 |
| [@laditor](https://gitee.com/laditor)       | 10  |
| [@brack_45](https://gitee.com/brack_45)     | 50  |
| [支付宝 ]**亮                                   | 50  |
| [@zx2289](https://gitee.com/zx2289)         | 101 |
| [微信] windhc                                 | 50  |

<br>
感谢所有的捐赠者
<br>
感谢您的支持！