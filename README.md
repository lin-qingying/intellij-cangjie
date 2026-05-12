# Cangjie Language Plugin for IntelliJ Platform

[中文文档](README_zh.md)

A comprehensive language plugin for the Cangjie programming language on IntelliJ-based IDEs.

Supports the latest 4 IDE versions .

[Get it from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26907-cangjie)

### Repositories

[Gitee ![star](https://gitee.com/Lin_Qing_Ying/intellij-cangjie/badge/star.svg?theme=dark)](https://gitee.com/Lin_Qing_Ying/intellij-cangjie)

[GitCode Cangjie-SIG ![star](https://gitcode.com/Cangjie-SIG/intellij-cangjie/star/badge.svg)](https://gitcode.com/Cangjie-SIG/intellij-cangjie)

[GitCode Open Cangjie Community ![star](https://gitcode.com/OpenCangjieCommunity/intellij-cangjie/star/badge.svg)](https://gitcode.com/OpenCangjieCommunity/intellij-cangjie)

[CangNova - IntelliJ-based IDE ![star](https://gitcode.com/OpenCangjieCommunity/CangNova/star/badge.svg)](https://gitcode.com/OpenCangjieCommunity/CangNova)

---

## Installation

Search for **CangJie** in the Plugins marketplace of your IntelliJ-based IDE and install it.

![img_2.png](https://gitee.com/Lin_Qing_Ying/intellij-cangjie/raw/analyze/img/img_2.png)

To create a project, use the **CangJie** template.

---

## Features

| Feature             | Status | Description                                                                   |
|---------------------|--------|-------------------------------------------------------------------------------|
| Syntax Parsing      | ✓      | Full syntax support including classes, functions, variable declarations, etc. |
| Syntax Highlighting | ✓      | Basic lexical highlighting                                                    |
| Code Completion     | ±      | Partially supported, continuously improving                                   |
| Code Formatting     | ✓      | Supported                                                                     |
| Debugging           | ✓      | DAP debugging and lldb debugging                                              |
| Run Targets         | ±      | Partially supported                                                           |
| LSP                 | ✓      | Implemented via LSP4IJ                                                        |
| Project Management  | ✓      | CJPM project parsing and dependency management                                |
| Workspace Support   | ✓      | Multi-module workspace projects                                               |

Legend:
- ✓ Implemented
- ± Partially supported
- \- Planned

---

## Project Architecture

For detailed project structure, see [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md).

**Tech Stack:**
- [Kotlin](https://kotlinlang.org/) 2.2.0 + [Gradle](https://gradle.org/) Kotlin DSL
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html) 

---

## Development Guide

### Building the Project

```bash
# Full build
./gradlew build

# Run plugin in IDE sandbox
./gradlew :product:idea-plugin:runIde

# Build plugin distribution
./gradlew :product:idea-plugin:buildPlugin
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :modules:ide:project:test
```

### Multi-Version Support

Defaults to IntelliJ Platform 253. To build against a different baseline:

```bash
./gradlew build -PplatformVersion=242
./gradlew build -PplatformVersion=253
```

Available baselines: 242 / 243 / 251 / 252 / 253 (see `gradle-*.properties`).

---

## Contributing

If you find any issues or missing features, contributions are welcome!

### How to Contribute

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).

You are free to use, modify, and distribute this software under the terms of the license. The license permits:
- Commercial use
- Modification
- Distribution
- Patent use
- Private use

For full license details, see the [LICENSE](LICENSE) file or visit [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

---

<br>
Thank you for your support!
