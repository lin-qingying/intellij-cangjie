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

import cn.cangnova.cangjie.toolchain.api.CjCompileOptions
import cn.cangnova.cangjie.toolchain.impl.CjToolchainLoader
import cn.cangnova.cangjie.toolchain.impl.DefaultCjCompileOptions
import cn.cangnova.cangjie.toolchain.impl.DefaultCjPackageOptions
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 官方CangJie工具链使用示例
 */
object ToolchainExample {
    
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
        
        // 获取编译器
        val compiler = toolchain.getCompiler()
        println("编译器版本: ${compiler.getVersion()}")
        
        // 获取包管理器
        val packageManager = toolchain.getPackageManager()
        println("包管理器版本: ${packageManager.getVersion()}")
        
        // 创建编译选项
        val compileOptions = DefaultCjCompileOptions.builder()
            .debug(true)
            .optimizationLevel(CjCompileOptions.OptimizationLevel.BASIC)
            .warningLevel(CjCompileOptions.WarningLevel.ALL)
            .build()
            
        // 创建带有高级选项的编译选项
        val advancedOptions = DefaultCjCompileOptions.builder()
            .debug(true)
            .optimizationLevel(CjCompileOptions.OptimizationLevel.MEDIUM)
            .warningLevel(CjCompileOptions.WarningLevel.ALL)
            // 启用实验性功能
            .experimental(true)
            // 启用增量编译
            .incrementalCompile(true)
            // 添加链接器选项
            .addLinkOption("--build-id=sha1")
            // 添加代码覆盖率选项
            .withSanitizerCoverage {
                basicBlockCoverage(true)
                edgeCoverage(true)
                eightBitCounters(true)
                tracePcGuard(true)
            }
            .build()
            
        // 创建用于发布的编译选项
        val releaseOptions = DefaultCjCompileOptions.release()
        
        // 创建用于代码覆盖率测试的编译选项
        val coverageOptions = DefaultCjCompileOptions.coverage()

        // 编译示例
        if (args.isNotEmpty()) {
            val sourcePath = Paths.get(args[0])
            val outputPath = Paths.get("build")
            
            println("编译文件: $sourcePath")
            
            // 根据参数选择不同的编译选项
            val options = when (args.getOrNull(1)) {
                "advanced" -> advancedOptions
                "release" -> releaseOptions
                "coverage" -> coverageOptions
                else -> compileOptions
            }
            
            val result = compiler.compile(sourcePath, outputPath, options)
            
            println("编译${if (result.success) "成功" else "失败"}")
            println("状态: ${result.status}")
            
            if (result.diagnostics.isNotEmpty()) {
                println("诊断信息:")
                result.diagnostics.forEach { diagnostic ->
                    println("  [${diagnostic.level}] ${diagnostic.filePath}:${diagnostic.line}:${diagnostic.column}: ${diagnostic.message}")
                }
            }
            
            if (result.outputFiles.isNotEmpty()) {
                println("输出文件:")
                result.outputFiles.forEach { file ->
                    println("  $file")
                }
            }
        }
        
        // 安装依赖示例
        if (args.size >= 3) {
            val packageName = args[2]
            val packageOptions = DefaultCjPackageOptions.default()
            
            println("安装依赖包: $packageName")
            val result = packageManager.install(packageName, null, packageOptions)
            
            println("安装${if (result.success) "成功" else "失败"}")
            println("状态: ${result.status}")
            
            if (result.packages.isNotEmpty()) {
                println("已安装的包:")
                result.packages.forEach { pkg ->
                    println("  ${pkg.name}@${pkg.version} (${pkg.status})")
                }
            }
        }
    }
} 