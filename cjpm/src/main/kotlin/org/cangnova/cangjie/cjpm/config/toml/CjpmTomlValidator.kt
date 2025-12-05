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
 * CJPM TOML 配置验证器
 */
object CjpmTomlValidator {
    /**
     * 验证配置的完整性和正确性
     *
     * @param config 要验证的配置
     * @return 验证结果
     */
    fun validate(config: CjpmTomlConfig): ValidationResult {
        val violations = mutableListOf<Violation>()

        // 验证包配置
        validatePackageConfig(config.`package`, violations)

        // 验证工作空间配置
        validateWorkspaceConfig(config.workspace, config.`package`, violations)

        // 验证依赖配置
        validateDependencies(config.dependencies, "dependencies", violations)
        validateDependencies(config.testDependencies, "test-dependencies", violations)
        validateDependencies(config.scriptDependencies, "script-dependencies", violations)

        // 验证目标配置
        validateTargetConfigs(config.target, violations)

        return ValidationResult(violations)
    }

    private fun validatePackageConfig(config: PackageConfig?, violations: MutableList<Violation>) {
        if (config == null) {
            violations.add(Violation(ViolationType.ERROR, "package", "Package configuration is required"))
            return
        }

        if (config.name.isBlank()) {
            violations.add(Violation(ViolationType.ERROR, "package.name", "Package name cannot be empty"))
        }
        if (!isValidPackageName(config.name)) {
            violations.add(Violation(ViolationType.ERROR, "package.name", 
                "Package name can only contain lowercase letters, numbers, and hyphens"))
        }

        if (config.version.isBlank()) {
            violations.add(Violation(ViolationType.ERROR, "package.version", "Package version cannot be empty"))
        }
        if (!isValidVersion(config.version)) {
            violations.add(Violation(ViolationType.ERROR, "package.version", 
                "Invalid version format. Expected format: MAJOR.MINOR.PATCH"))
        }

        if (config.cjcVersion.isBlank()) {
            violations.add(Violation(ViolationType.ERROR, "package.cjc-version", "CJC version cannot be empty"))
        }
    }

    private fun validateWorkspaceConfig(
        workspace: WorkspaceConfig?, 
        packageConfig: PackageConfig?,
        violations: MutableList<Violation>
    ) {
        if (workspace != null && packageConfig != null) {
            violations.add(Violation(ViolationType.ERROR, "workspace", 
                "Cannot have both workspace and package configurations"))
        }

        workspace?.let {
            if (it.members.isEmpty()) {
                violations.add(Violation(ViolationType.ERROR, "workspace.members", 
                    "Workspace must have at least one member"))
            }

            it.buildMembers?.let { buildMembers ->
                val invalidMembers = buildMembers.filter { member -> member !in it.members }
                if (invalidMembers.isNotEmpty()) {
                    violations.add(Violation(ViolationType.ERROR, "workspace.build-members",
                        "Build members must be a subset of workspace members: $invalidMembers"))
                }
            }

            it.testMembers?.let { testMembers ->
                val invalidMembers = testMembers.filter { member -> member !in (it.buildMembers ?: it.members) }
                if (invalidMembers.isNotEmpty()) {
                    violations.add(Violation(ViolationType.ERROR, "workspace.test-members",
                        "Test members must be a subset of build members: $invalidMembers"))
                }
            }
        }
    }

    private fun validateDependencies(
        dependencies: Map<String, DependencyConfig>,
        path: String,
        violations: MutableList<Violation>
    ) {
        dependencies.forEach { (name, config) ->
            if (!isValidPackageName(name)) {
                violations.add(Violation(ViolationType.ERROR, "$path.$name",
                    "Dependency name can only contain lowercase letters, numbers, and hyphens"))
            }

            if (config.path == null && config.git == null) {
                violations.add(Violation(ViolationType.ERROR, "$path.$name",
                    "Dependency must specify either path or git source"))
            }

            if (config.path != null && config.git != null) {
                violations.add(Violation(ViolationType.ERROR, "$path.$name",
                    "Dependency cannot specify both path and git source"))
            }

            config.version?.let {
                if (!isValidVersion(it)) {
                    violations.add(Violation(ViolationType.ERROR, "$path.$name.version",
                        "Invalid version format. Expected format: MAJOR.MINOR.PATCH"))
                }
            }
        }
    }

    private fun validateTargetConfigs(
        targets: Map<String, TargetConfig>,
        violations: MutableList<Violation>
    ) {
        targets.forEach { (name, config) ->
            if (!isValidTargetTriple(name)) {
                violations.add(
                    Violation(
                        ViolationType.WARNING, "targetPlatform.$name",
                    "Target triple format should be: <arch>-<vendor>-<sys>-<abi>"))
            }

            validateDependencies(config.dependencies.orEmpty(), "targetPlatform.$name.dependencies", violations)
            validateDependencies(
                config.testDependencies.orEmpty(),
                "targetPlatform.$name.test-dependencies",
                violations
            )
        }
    }

    private fun isValidPackageName(name: String): Boolean {
        return name.matches(Regex("^[a-z0-9-]+$"))
    }

    private fun isValidVersion(version: String): Boolean {
        return version.matches(Regex("^\\d+\\.\\d+\\.\\d+(?:-[a-zA-Z0-9.-]+)?(?:\\+[a-zA-Z0-9.-]+)?$"))
    }

    private fun isValidTargetTriple(triple: String): Boolean {
        val parts = triple.split("-")
        return parts.size >= 3
    }

    /**
     * 违规类型
     */
    enum class ViolationType {
        ERROR,      // 错误，必须修复
        WARNING     // 警告，建议修复
    }

    /**
     * 配置违规
     */
    data class Violation(
        val type: ViolationType,    // 违规类型
        val path: String,           // 配置路径
        val message: String         // 违规信息
    )

    /**
     * 验证结果
     */
    data class ValidationResult(
        val violations: List<Violation>
    ) {
        /**
         * 是否有错误
         */
        val hasErrors: Boolean
            get() = violations.any { it.type == ViolationType.ERROR }

        /**
         * 是否有警告
         */
        val hasWarnings: Boolean
            get() = violations.any { it.type == ViolationType.WARNING }

        /**
         * 获取所有错误
         */
        fun getErrors(): List<Violation> = violations.filter { it.type == ViolationType.ERROR }

        /**
         * 获取所有警告
         */
        fun getWarnings(): List<Violation> = violations.filter { it.type == ViolationType.WARNING }
    }
} 