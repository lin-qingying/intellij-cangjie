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

import cn.cangnova.cangjie.toolchain.api.CjPackageOptions

/**
 * CangJie包管理选项的默认实现
 */
data class DefaultCjPackageOptions(
    override val scope: CjPackageOptions.InstallScope = CjPackageOptions.InstallScope.PROJECT,
    override val dependencyType: CjPackageOptions.DependencyType = CjPackageOptions.DependencyType.PRODUCTION,
    override val saveMode: CjPackageOptions.SaveMode = CjPackageOptions.SaveMode.COMPATIBLE,
    override val targetDir: String? = null,
    override val target: String? = null,
    override val release: Boolean = true,
    override val crossCompile: Boolean = false,
    override val workingDirectory: String? = null,
    override val extraArgs: List<String> = emptyList()
) : CjPackageOptions {

    /**
     * 创建一个新的包管理选项构建器
     */
    class Builder {
        private var scope: CjPackageOptions.InstallScope = CjPackageOptions.InstallScope.PROJECT
        private var dependencyType: CjPackageOptions.DependencyType = CjPackageOptions.DependencyType.PRODUCTION
        private var saveMode: CjPackageOptions.SaveMode = CjPackageOptions.SaveMode.COMPATIBLE
        private var targetDir: String? = null
        private var target: String? = null
        private var release: Boolean = true
        private var crossCompile: Boolean = false
        private var workingDirectory: String? = null
        private var extraArgs: MutableList<String> = mutableListOf()

        /**
         * 设置安装范围
         */
        fun scope(scope: CjPackageOptions.InstallScope) = apply { this.scope = scope }

        /**
         * 设置依赖类型
         */
        fun dependencyType(type: CjPackageOptions.DependencyType) = apply { this.dependencyType = type }

        /**
         * 设置保存模式
         */
        fun saveMode(mode: CjPackageOptions.SaveMode) = apply { this.saveMode = mode }
        
        /**
         * 设置目标目录
         */
        fun targetDir(dir: String?) = apply { this.targetDir = dir }
        
        /**
         * 设置目标平台
         */
        fun target(target: String?) = apply { this.target = target }
        
        /**
         * 设置是否为发布模式
         */
        fun release(release: Boolean) = apply { this.release = release }
        
        /**
         * 设置是否启用交叉编译
         */
        fun crossCompile(crossCompile: Boolean) = apply { this.crossCompile = crossCompile }
        
        /**
         * 设置工作目录
         */
        fun workingDirectory(dir: String?) = apply { this.workingDirectory = dir }

        /**
         * 添加额外参数
         */
        fun addExtraArg(arg: String) = apply { this.extraArgs.add(arg) }

        /**
         * 添加多个额外参数
         */
        fun addExtraArgs(args: List<String>) = apply { this.extraArgs.addAll(args) }

        /**
         * 构建包管理选项
         */
        fun build(): CjPackageOptions {
            return DefaultCjPackageOptions(
                scope = scope,
                dependencyType = dependencyType,
                saveMode = saveMode,
                targetDir = targetDir,
                target = target,
                release = release,
                crossCompile = crossCompile,
                workingDirectory = workingDirectory,
                extraArgs = extraArgs.toList()
            )
        }
    }

    companion object {
        /**
         * 创建一个新的包管理选项构建器
         */
        fun builder(): Builder = Builder()

        /**
         * 创建默认的包管理选项
         */
        fun default(): CjPackageOptions = Builder().build()

        /**
         * 创建用于全局安装的包管理选项
         */
        fun global(): CjPackageOptions = Builder()
            .scope(CjPackageOptions.InstallScope.GLOBAL)
            .build()

        /**
         * 创建用于开发依赖的包管理选项
         */
        fun dev(): CjPackageOptions = Builder()
            .dependencyType(CjPackageOptions.DependencyType.DEVELOPMENT)
            .build()
            
        /**
         * 创建用于调试模式的包管理选项
         */
        fun debug(): CjPackageOptions = Builder()
            .release(false)
            .build()
            
        /**
         * 创建用于交叉编译的包管理选项
         */
        fun crossCompile(target: String): CjPackageOptions = Builder()
            .target(target)
            .crossCompile(true)
            .build()
    }
} 