/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.toolchain.impl.example

import cn.cangnova.cangjie.toolchain.api.CjPackageManager
import cn.cangnova.cangjie.toolchain.api.CjPackageOptions
import cn.cangnova.cangjie.toolchain.api.CjPackageResult
import cn.cangnova.cangjie.toolchain.impl.CjToolchainLoader
import cn.cangnova.cangjie.toolchain.impl.DefaultCjPackageOptions
import java.nio.file.Path
import java.nio.file.Paths

/**
 * CangJie包管理器使用示例
 */
object PackageManagerExample {
    
    @JvmStatic
    fun main(args: Array<String>) {
        // 获取所有可用的工具链
        val toolchains = CjToolchainLoader.getToolchains()
        
        if (toolchains.isEmpty()) {
            println("未检测到可用的CangJie工具链，请确保已安装CangJie编译器")
            return
        }
        
        // 使用第一个可用的工具链
        val toolchain = toolchains.first()
        println("使用工具链: ${toolchain.version} (${toolchain.homePath})")
        
        // 获取包管理器
        val packageManager = toolchain.getPackageManager()
        println("包管理器版本: ${packageManager.getVersion()}")
        
        // 根据命令行参数执行不同的操作
        if (args.isEmpty()) {
            printUsage()
            return
        }
        
        when (args[0]) {
            "init" -> initProject(packageManager, args)
            "build" -> buildProject(packageManager, args)
            "run" -> runProject(packageManager, args)
            "test" -> testProject(packageManager, args)
            "clean" -> cleanProject(packageManager, args)
            "install" -> installPackage(packageManager, args)
            "update" -> updatePackage(packageManager, args)
            "uninstall" -> uninstallPackage(packageManager, args)
            else -> printUsage()
        }
    }
    
    private fun printUsage() {
        println("""
            CangJie包管理器使用示例
            用法:
              init <name> [path]                    - 初始化新项目
              build <project-path> [target]         - 构建项目
              run <project-path> [args...]          - 运行项目
              test <project-path> [test-paths...]   - 运行测试
              clean <project-path>                  - 清理项目
              install <package-name> [version]      - 安装依赖包
              update [package-name]                 - 更新依赖包
              uninstall <package-name>              - 移除依赖包
        """.trimIndent())
    }
    
    private fun initProject(packageManager: CjPackageManager, args: Array<String>) {
        if (args.size < 2) {
            println("错误: 缺少项目名称")
            return
        }
        
        val name = args[1]
        val path = if (args.size > 2) Paths.get(args[2]) else null
        val outputType = CjPackageManager.OutputType.EXECUTABLE
        val options = DefaultCjPackageOptions.default()
        
        println("初始化项目: $name")
        val result = packageManager.init(name, path, outputType, options)
        
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        println("输出信息:")
        println(result.output)
    }
    
    private fun buildProject(packageManager: CjPackageManager, args: Array<String>) {
        if (args.size < 2) {
            println("错误: 缺少项目路径")
            return
        }
        
        val projectPath = Paths.get(args[1])
        val target = if (args.size > 2) args[2] else null
        
        // 创建发布模式的包管理选项
        val options = DefaultCjPackageOptions.builder()
            .release(true)
            .build()
        
        println("构建项目: $projectPath")
        val result = packageManager.build(projectPath, target, options)
        
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        println("输出信息:")
        println(result.output)
    }
    
    private fun runProject(packageManager: CjPackageManager, args: Array<String>) {
        if (args.size < 2) {
            println("错误: 缺少项目路径")
            return
        }
        
        val projectPath = Paths.get(args[1])
        val runArgs = if (args.size > 2) args.slice(2 until args.size) else emptyList()
        
        // 创建默认的包管理选项
        val options = DefaultCjPackageOptions.default()
        
        println("运行项目: $projectPath")
        val result = packageManager.run(projectPath, runArgs, options)
        
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        println("输出信息:")
        println(result.output)
    }
    
    private fun testProject(packageManager: CjPackageManager, args: Array<String>) {
        if (args.size < 2) {
            println("错误: 缺少项目路径")
            return
        }
        
        val projectPath = Paths.get(args[1])
        val testPaths = if (args.size > 2) {
            args.slice(2 until args.size).map { Paths.get(it) }
        } else {
            null
        }
        
        // 创建调试模式的包管理选项
        val options = DefaultCjPackageOptions.debug()
        
        println("测试项目: $projectPath")
        val result = packageManager.test(projectPath, testPaths, null, options)
        
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        println("输出信息:")
        println(result.output)
    }
    
    private fun cleanProject(packageManager: CjPackageManager, args: Array<String>) {
        if (args.size < 2) {
            println("错误: 缺少项目路径")
            return
        }
        
        val projectPath = Paths.get(args[1])
        val options = DefaultCjPackageOptions.default()
        
        println("清理项目: $projectPath")
        val result = packageManager.clean(projectPath, options)
        
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        println("输出信息:")
        println(result.output)
    }
    
    private fun installPackage(packageManager: CjPackageManager, args: Array<String>) {
        if (args.size < 2) {
            println("错误: 缺少包名称")
            return
        }
        
        val packageName = args[1]
        val version = if (args.size > 2) args[2] else null
        
        // 创建保存精确版本的包管理选项
        val options = DefaultCjPackageOptions.builder()
            .saveMode(CjPackageOptions.SaveMode.EXACT)
            .build()
        
        println("安装依赖包: $packageName${version?.let { "@$it" } ?: ""}")
        val result = packageManager.install(packageName, version, options)
        
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        
        if (result.packages.isNotEmpty()) {
            println("已安装的包:")
            result.packages.forEach { pkg ->
                println("  ${pkg.name}@${pkg.version} (${pkg.status})")
            }
        }
        
        println("输出信息:")
        println(result.output)
    }
    
    private fun updatePackage(packageManager: CjPackageManager, args: Array<String>) {
        val packageName = if (args.size > 1) args[1] else null
        val options = DefaultCjPackageOptions.default()
        
        println("更新依赖包${packageName?.let { ": $it" } ?: ""}")
        val result = packageManager.update(packageName, options)
        
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        
        if (result.packages.isNotEmpty()) {
            println("已更新的包:")
            result.packages.forEach { pkg ->
                println("  ${pkg.name}@${pkg.version} (${pkg.status})")
            }
        }
        
        println("输出信息:")
        println(result.output)
    }
    
    private fun uninstallPackage(packageManager: CjPackageManager, args: Array<String>) {
        if (args.size < 2) {
            println("错误: 缺少包名称")
            return
        }
        
        val packageName = args[1]
        val options = DefaultCjPackageOptions.default()
        
        println("卸载依赖包: $packageName")
        val result = packageManager.uninstall(packageName, options)
        
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        
        if (result.packages.isNotEmpty()) {
            println("已卸载的包:")
            result.packages.forEach { pkg ->
                println("  ${pkg.name}@${pkg.version} (${pkg.status})")
            }
        }
        
        println("输出信息:")
        println(result.output)
    }
    
    private fun printOperationResult(result: CjPackageResult) {
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        
        if (result.packages.isNotEmpty()) {
            println("受影响的包:")
            result.packages.forEach { pkg ->
                println("  ${pkg.name}@${pkg.version} (${pkg.status})")
            }
        }
        
        println("输出信息:")
        println(result.output)
    }
} 