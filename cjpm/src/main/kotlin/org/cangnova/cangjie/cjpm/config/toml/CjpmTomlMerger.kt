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

package org.cangnova.cangjie.cjpm.project.model.toml

/**
 * CJPM TOML 配置合并工具类
 */
object CjpmTomlMerger {
    /**
     * 合并两个配置，用 other 的非空值覆盖 base 的值
     *
     * @param base 基础配置
     * @param other 要合并的配置
     * @return 合并后的新配置
     */
    fun merge(base: CjpmTomlConfig, other: CjpmTomlConfig): CjpmTomlConfig {
        return CjpmTomlConfig(
            `package` = mergePackageConfig(base.`package`, other.`package`),
            workspace = mergeWorkspaceConfig(base.workspace, other.workspace),
            dependencies = mergeDependencies(base.dependencies, other.dependencies),
            testDependencies = mergeDependencies(base.testDependencies, other.testDependencies),
            scriptDependencies = mergeDependencies(base.scriptDependencies, other.scriptDependencies),
            ffi = mergeFfiConfig(base.ffi, other.ffi),
            profile = mergeProfileConfig(base.profile, other.profile),
            target = mergeTargetConfigs(base.target, other.target)
        )
    }

    private fun mergePackageConfig(base: PackageConfig?, other: PackageConfig?): PackageConfig? {
        if (base == null) return other
        if (other == null) return base

        return PackageConfig(
            name = other.name.takeIf { it.isNotBlank() } ?: base.name,
            version = other.version.takeIf { it.isNotBlank() } ?: base.version,
            cjcVersion = other.cjcVersion.takeIf { it.isNotBlank() } ?: base.cjcVersion,
            description = other.description ?: base.description,
            compileOption = other.compileOption ?: base.compileOption,
            overrideCompileOption = other.overrideCompileOption ?: base.overrideCompileOption,
            linkOption = other.linkOption ?: base.linkOption,
            outputType = other.outputType,
            srcDir = other.srcDir,
            targetDir = other.targetDir,
            packageConfiguration = mergePackageConfigurations(base.packageConfiguration, other.packageConfiguration) ?: mapOf()
        )
    }

    private fun mergeWorkspaceConfig(base: WorkspaceConfig?, other: WorkspaceConfig?): WorkspaceConfig? {
        if (base == null) return other
        if (other == null) return base

        return WorkspaceConfig(
            members = (base.members + other.members).distinct(),
            buildMembers = other.buildMembers ?: base.buildMembers,
            testMembers = other.testMembers ?: base.testMembers,
            compileOption = other.compileOption ?: base.compileOption,
            overrideCompileOption = other.overrideCompileOption ?: base.overrideCompileOption,
            linkOption = other.linkOption ?: base.linkOption,
            targetDir = other.targetDir ?: base.targetDir
        )
    }

    private fun mergeDependencies(
        base: Map<String, DependencyConfig>,
        other: Map<String, DependencyConfig>
    ): Map<String, DependencyConfig> {
        val result = mutableMapOf<String, DependencyConfig>()
        result.putAll(base)
        result.putAll(other)
        return result
    }

    private fun mergeFfiConfig(base: FfiConfig?, other: FfiConfig?): FfiConfig? {
        if (base == null) return other
        if (other == null) return base

        return FfiConfig(
            c = base.c.orEmpty() + (other.c.orEmpty())
        )
    }

    private fun mergeProfileConfig(base: ProfileConfig?, other: ProfileConfig?): ProfileConfig? {
        if (base == null) return other
        if (other == null) return base

        return ProfileConfig(
            build = other.build ?: base.build,
            test = other.test ?: base.test,
            bench = other.bench ?: base.bench,
            run = other.run ?: base.run,
            customizedOption = base.customizedOption.orEmpty() + (other.customizedOption.orEmpty())
        )
    }

    private fun mergeTargetConfigs(
        base: Map<String, TargetConfig>,
        other: Map<String, TargetConfig>
    ): Map<String, TargetConfig> {
        val result = mutableMapOf<String, TargetConfig>()
        result.putAll(base)
        
        other.forEach { (key, value) ->
            val baseConfig = result[key]
            result[key] = mergeTargetConfig(baseConfig, value)
        }
        
        return result
    }

    private fun mergeTargetConfig(base: TargetConfig?, other: TargetConfig): TargetConfig {
        if (base == null) return other

        return TargetConfig(
            compileOption = other.compileOption ?: base.compileOption,
            overrideCompileOption = other.overrideCompileOption ?: base.overrideCompileOption,
            linkOption = other.linkOption ?: base.linkOption,
            dependencies = mergeDependencies(base.dependencies.orEmpty(), other.dependencies.orEmpty()),
            testDependencies = mergeDependencies(base.testDependencies.orEmpty(), other.testDependencies.orEmpty()),
            binDependencies = mergeBinDependencies(base.binDependencies, other.binDependencies),
            compileMacrosForTarget = other.compileMacrosForTarget ?: base.compileMacrosForTarget,
            debug = other.debug ?: base.debug,
            release = other.release ?: base.release
        )
    }

    private fun mergeBinDependencies(base: BinDependenciesConfig?, other: BinDependenciesConfig?): BinDependenciesConfig? {
        if (base == null) return other
        if (other == null) return base

        return BinDependenciesConfig(
            pathOption = (base.pathOption.orEmpty() + other.pathOption.orEmpty()).distinct(),
            packageOption = base.packageOption.orEmpty() + other.packageOption.orEmpty()
        )
    }

    private fun mergePackageConfigurations(
        base: Map<String, PackageConfigurationInfo>?,
        other: Map<String, PackageConfigurationInfo>?
    ): Map<String, PackageConfigurationInfo>? {
        if (base == null) return other
        if (other == null) return base

        return buildMap {
            putAll(base)
            putAll(other)
        }
    }
} 