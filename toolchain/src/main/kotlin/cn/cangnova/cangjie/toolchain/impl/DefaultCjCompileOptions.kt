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

package cn.cangnova.cangjie.toolchain.impl

import cn.cangnova.cangjie.toolchain.api.CjCompileOptions

/**
 * CangJie编译选项的默认实现
 */
data class DefaultCjCompileOptions(
    override val debug: Boolean = false,
    override val optimizationLevel: CjCompileOptions.OptimizationLevel = CjCompileOptions.OptimizationLevel.NONE,
    override val warningLevel: CjCompileOptions.WarningLevel = CjCompileOptions.WarningLevel.NORMAL,
    override val targetPlatform: CjCompileOptions.TargetPlatform? = null,
    override val experimental: Boolean = false,
    override val incrementalCompile: Boolean = false,
    override val linkOptions: List<String> = emptyList(),
    override val sanitizerCoverage: CjCompileOptions.SanitizerCoverage? = null,
    override val extraArgs: List<String> = emptyList()
) : CjCompileOptions {

    /**
     * 创建一个新的编译选项构建器
     */
    class Builder {
        private var debug: Boolean = false
        private var optimizationLevel: CjCompileOptions.OptimizationLevel = CjCompileOptions.OptimizationLevel.NONE
        private var warningLevel: CjCompileOptions.WarningLevel = CjCompileOptions.WarningLevel.NORMAL
        private var targetPlatform: CjCompileOptions.TargetPlatform? = null
        private var experimental: Boolean = false
        private var incrementalCompile: Boolean = false
        private var linkOptions: MutableList<String> = mutableListOf()
        private var sanitizerCoverage: CjCompileOptions.SanitizerCoverage? = null
        private var extraArgs: MutableList<String> = mutableListOf()

        /**
         * 设置是否生成调试信息
         */
        fun debug(debug: Boolean) = apply { this.debug = debug }

        /**
         * 设置优化级别
         */
        fun optimizationLevel(level: CjCompileOptions.OptimizationLevel) = apply { this.optimizationLevel = level }

        /**
         * 设置警告级别
         */
        fun warningLevel(level: CjCompileOptions.WarningLevel) = apply { this.warningLevel = level }

        /**
         * 设置目标平台
         */
        fun targetPlatform(platform: CjCompileOptions.TargetPlatform?) = apply { this.targetPlatform = platform }
        
        /**
         * 设置是否启用实验性功能
         */
        fun experimental(experimental: Boolean) = apply { this.experimental = experimental }
        
        /**
         * 设置是否启用增量编译
         */
        fun incrementalCompile(incrementalCompile: Boolean) = apply { 
            this.incrementalCompile = incrementalCompile
            if (incrementalCompile) {
                this.experimental = true // 增量编译需要启用实验性功能
            }
        }
        
        /**
         * 添加链接器选项
         */
        fun addLinkOption(option: String) = apply { this.linkOptions.add(option) }
        
        /**
         * 添加多个链接器选项
         */
        fun addLinkOptions(options: List<String>) = apply { this.linkOptions.addAll(options) }
        
        /**
         * 设置代码覆盖率选项
         */
        fun sanitizerCoverage(coverage: CjCompileOptions.SanitizerCoverage?) = apply { this.sanitizerCoverage = coverage }
        
        /**
         * 创建代码覆盖率选项构建器
         */
        fun withSanitizerCoverage(init: SanitizerCoverageBuilder.() -> Unit): Builder {
            val builder = SanitizerCoverageBuilder()
            builder.init()
            this.sanitizerCoverage = builder.build()
            return this
        }

        /**
         * 添加额外参数
         */
        fun addExtraArg(arg: String) = apply { this.extraArgs.add(arg) }

        /**
         * 添加多个额外参数
         */
        fun addExtraArgs(args: List<String>) = apply { this.extraArgs.addAll(args) }

        /**
         * 构建编译选项
         */
        fun build(): CjCompileOptions {
            return DefaultCjCompileOptions(
                debug = debug,
                optimizationLevel = optimizationLevel,
                warningLevel = warningLevel,
                targetPlatform = targetPlatform,
                experimental = experimental,
                incrementalCompile = incrementalCompile,
                linkOptions = linkOptions.toList(),
                sanitizerCoverage = sanitizerCoverage,
                extraArgs = extraArgs.toList()
            )
        }
        
        /**
         * 代码覆盖率选项构建器
         */
        class SanitizerCoverageBuilder {
            private var basicBlockCoverage: Boolean = false
            private var edgeCoverage: Boolean = false
            private var eightBitCounters: Boolean = false
            private var tracePcGuard: Boolean = false
            private var functionEntryCoverage: Boolean = false
            private var stackDepthCoverage: Boolean = false
            private var traceCompares: Boolean = false
            private var traceMemcmp: Boolean = false
            
            /**
             * 设置是否启用基本块覆盖率
             */
            fun basicBlockCoverage(enabled: Boolean) = apply { this.basicBlockCoverage = enabled }
            
            /**
             * 设置是否启用边缘覆盖率
             */
            fun edgeCoverage(enabled: Boolean) = apply { this.edgeCoverage = enabled }
            
            /**
             * 设置是否启用8位计数器覆盖率
             */
            fun eightBitCounters(enabled: Boolean) = apply { this.eightBitCounters = enabled }
            
            /**
             * 设置是否启用追踪PC表
             */
            fun tracePcGuard(enabled: Boolean) = apply { this.tracePcGuard = enabled }
            
            /**
             * 设置是否启用函数入口覆盖率
             */
            fun functionEntryCoverage(enabled: Boolean) = apply { this.functionEntryCoverage = enabled }
            
            /**
             * 设置是否启用堆栈深度覆盖率
             */
            fun stackDepthCoverage(enabled: Boolean) = apply { this.stackDepthCoverage = enabled }
            
            /**
             * 设置是否启用比较追踪
             */
            fun traceCompares(enabled: Boolean) = apply { this.traceCompares = enabled }
            
            /**
             * 设置是否启用内存比较追踪
             */
            fun traceMemcmp(enabled: Boolean) = apply { this.traceMemcmp = enabled }
            
            /**
             * 构建代码覆盖率选项
             */
            fun build(): CjCompileOptions.SanitizerCoverage {
                return CjCompileOptions.SanitizerCoverage(
                    basicBlockCoverage = basicBlockCoverage,
                    edgeCoverage = edgeCoverage,
                    eightBitCounters = eightBitCounters,
                    tracePcGuard = tracePcGuard,
                    functionEntryCoverage = functionEntryCoverage,
                    stackDepthCoverage = stackDepthCoverage,
                    traceCompares = traceCompares,
                    traceMemcmp = traceMemcmp
                )
            }
        }
    }

    companion object {
        /**
         * 创建一个新的编译选项构建器
         */
        fun builder(): Builder = Builder()

        /**
         * 创建默认的编译选项
         */
        fun default(): CjCompileOptions = Builder().build()

        /**
         * 创建用于调试的编译选项
         */
        fun debug(): CjCompileOptions = Builder()
            .debug(true)
            .optimizationLevel(CjCompileOptions.OptimizationLevel.NONE)
            .build()

        /**
         * 创建用于发布的编译选项
         */
        fun release(): CjCompileOptions = Builder()
            .debug(false)
            .optimizationLevel(CjCompileOptions.OptimizationLevel.FULL)
            .build()
            
        /**
         * 创建用于代码覆盖率测试的编译选项
         */
        fun coverage(): CjCompileOptions = Builder()
            .debug(true)
            .withSanitizerCoverage {
                basicBlockCoverage(true)
                edgeCoverage(true)
                eightBitCounters(true)
            }
            .build()
    }
} 