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
import cn.cangnova.cangjie.toolchain.impl.CjToolchainLoader
import cn.cangnova.cangjie.toolchain.impl.DefaultCjPackageOptions
import java.nio.file.Paths

/**
 * CangJie交叉编译示例
 */
object CrossCompileExample {
    
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
        
        // 检查命令行参数
        if (args.size < 3) {
            println("用法: CrossCompileExample <project-path> <command> <target-platform>")
            println("命令: build, test")
            println("目标平台示例: x86_64-unknown-linux-gnu, aarch64-apple-darwin")
            return
        }
        
        val projectPath = Paths.get(args[0])
        val command = args[1]
        val targetPlatform = args[2]
        
        // 创建交叉编译选项
        val options = DefaultCjPackageOptions.builder()
            .target(targetPlatform)
            .crossCompile(true)
            .release(true) // 使用发布模式
            .build()
        
        println("项目路径: $projectPath")
        println("目标平台: $targetPlatform")
        
        // 执行对应的命令
        when (command) {
            "build" -> {
                println("开始交叉编译项目...")
                val result = packageManager.build(projectPath, targetPlatform, options)
                printResult(result)
            }
            "test" -> {
                println("开始交叉编译并测试项目...")
                val result = packageManager.test(projectPath, null, targetPlatform, options)
                printResult(result)
            }
            else -> {
                println("不支持的命令: $command")
                println("支持的命令: build, test")
            }
        }
    }
    
    private fun printResult(result: cn.cangnova.cangjie.toolchain.api.CjPackageResult) {
        println("操作${if (result.success) "成功" else "失败"}: ${result.status}")
        println("输出信息:")
        println(result.output)
        
        // 显示交叉编译产物的位置
        if (result.success) {
            println("交叉编译产物位于: target/<target-platform>/ 目录下")
            println("如果有动态库依赖，请确保在目标平台上配置 LD_LIBRARY_PATH 环境变量")
        }
    }
} 