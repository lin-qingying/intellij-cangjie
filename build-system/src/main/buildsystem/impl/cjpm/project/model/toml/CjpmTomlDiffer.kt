package org.cangnova.cangjie.buildsystem.impl.cjpm.project.model.toml

/**
 * CJPM TOML 配置差异比较工具类
 */
object CjpmTomlDiffer {
    /**
     * 比较两个配置之间的差异
     *
     * @param base 基础配置
     * @param other 要比较的配置
     * @return 配置差异结果
     */
    fun diff(base: CjpmTomlConfig, other: CjpmTomlConfig): DiffResult {
        val differences = mutableListOf<Difference>()

        // 比较包配置
        diffPackageConfig(base.`package`, other.`package`)?.let { differences.addAll(it) }

        // 比较工作空间配置
        diffWorkspaceConfig(base.workspace, other.workspace)?.let { differences.addAll(it) }

        // 比较依赖配置
        differences.addAll(diffDependencies("dependencies", base.dependencies, other.dependencies))
        differences.addAll(diffDependencies("test-dependencies", base.testDependencies, other.testDependencies))
        differences.addAll(diffDependencies("script-dependencies", base.scriptDependencies, other.scriptDependencies))

        // 比较 FFI 配置
        diffFfiConfig(base.ffi, other.ffi)?.let { differences.addAll(it) }

        // 比较 Profile 配置
        diffProfileConfig(base.profile, other.profile)?.let { differences.addAll(it) }

        // 比较目标配置
        differences.addAll(diffTargetConfigs(base.target, other.target))

        return DiffResult(differences)
    }

    private fun diffPackageConfig(base: PackageConfig?, other: PackageConfig?): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference("package", null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference("package", base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        if (base.name != other.name) {
            differences.add(Difference("package.name", base.name, other.name, DiffType.MODIFIED))
        }
        if (base.version != other.version) {
            differences.add(Difference("package.version", base.version, other.version, DiffType.MODIFIED))
        }
        if (base.cjcVersion != other.cjcVersion) {
            differences.add(Difference("package.cjc-version", base.cjcVersion, other.cjcVersion, DiffType.MODIFIED))
        }
        if (base.outputType != other.outputType) {
            differences.add(Difference("package.output-type", base.outputType, other.outputType, DiffType.MODIFIED))
        }
        // ... 其他字段的比较

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffDependencies(
        path: String,
        base: Map<String, DependencyConfig>,
        other: Map<String, DependencyConfig>
    ): List<Difference> {
        val differences = mutableListOf<Difference>()

        // 检查移除的依赖
        base.keys.filter { it !in other.keys }.forEach { key ->
            differences.add(Difference("$path.$key", base[key], null, DiffType.REMOVED))
        }

        // 检查新增的依赖
        other.keys.filter { it !in base.keys }.forEach { key ->
            differences.add(Difference("$path.$key", null, other[key], DiffType.ADDED))
        }

        // 检查修改的依赖
        base.keys.intersect(other.keys).forEach { key ->
            val baseDep = base[key]
            val otherDep = other[key]
            if (baseDep != otherDep) {
                differences.add(Difference("$path.$key", baseDep, otherDep, DiffType.MODIFIED))
            }
        }

        return differences
    }

    private fun diffFfiConfig(base: FfiConfig?, other: FfiConfig?): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference("ffi", null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference("ffi", base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        // 比较 C 语言库配置
        val baseC = base.c.orEmpty()
        val otherC = other.c.orEmpty()

        // 检查移除的 C 库
        baseC.keys.filter { it !in otherC.keys }.forEach { key ->
            differences.add(Difference("ffi.c.$key", baseC[key], null, DiffType.REMOVED))
        }

        // 检查新增的 C 库
        otherC.keys.filter { it !in baseC.keys }.forEach { key ->
            differences.add(Difference("ffi.c.$key", null, otherC[key], DiffType.ADDED))
        }

        // 检查修改的 C 库
        baseC.keys.intersect(otherC.keys).forEach { key ->
            val baseCLib = baseC[key]
            val otherCLib = otherC[key]
            if (baseCLib != otherCLib) {
                differences.add(Difference("ffi.c.$key", baseCLib, otherCLib, DiffType.MODIFIED))
            }
        }

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffWorkspaceConfig(base: WorkspaceConfig?, other: WorkspaceConfig?): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference("workspace", null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference("workspace", base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        // 比较成员列表
        if (base.members != other.members) {
            val added = other.members.filter { it !in base.members }
            val removed = base.members.filter { it !in other.members }

            if (added.isNotEmpty()) {
                differences.add(Difference("workspace.members.added", null, added, DiffType.ADDED))
            }
            if (removed.isNotEmpty()) {
                differences.add(Difference("workspace.members.removed", removed, null, DiffType.REMOVED))
            }
        }

        // 比较构建成员列表
        if (base.buildMembers != other.buildMembers) {
            differences.add(
                Difference(
                    "workspace.build-members",
                    base.buildMembers,
                    other.buildMembers,
                    DiffType.MODIFIED
                )
            )
        }

        // 比较测试成员列表
        if (base.testMembers != other.testMembers) {
            differences.add(
                Difference(
                    "workspace.test-members",
                    base.testMembers,
                    other.testMembers,
                    DiffType.MODIFIED
                )
            )
        }

        // 比较编译选项
        if (base.compileOption != other.compileOption) {
            differences.add(
                Difference(
                    "workspace.compile-option",
                    base.compileOption,
                    other.compileOption,
                    DiffType.MODIFIED
                )
            )
        }

        // 比较覆盖编译选项
        if (base.overrideCompileOption != other.overrideCompileOption) {
            differences.add(
                Difference(
                    "workspace.override-compile-option",
                    base.overrideCompileOption,
                    other.overrideCompileOption,
                    DiffType.MODIFIED
                )
            )
        }

        // 比较链接选项
        if (base.linkOption != other.linkOption) {
            differences.add(
                Difference(
                    "workspace.link-option",
                    base.linkOption,
                    other.linkOption,
                    DiffType.MODIFIED
                )
            )
        }

        // 比较目标目录
        if (base.targetDir != other.targetDir) {
            differences.add(
                Difference(
                    "workspace.target-dir",
                    base.targetDir,
                    other.targetDir,
                    DiffType.MODIFIED
                )
            )
        }

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffProfileConfig(base: ProfileConfig?, other: ProfileConfig?): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference("profile", null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference("profile", base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        // 比较构建配置
        if (base.build != other.build) {
            differences.add(
                Difference(
                    "profile.build",
                    base.build,
                    other.build,
                    DiffType.MODIFIED
                )
            )
        }

        // 比较测试配置
        diffTestProfileConfig(base.test, other.test)?.let { differences.addAll(it) }

        // 比较性能测试配置
        diffBenchProfileConfig(base.bench, other.bench)?.let { differences.addAll(it) }

        // 比较运行配置
        diffRunProfileConfig(base.run, other.run)?.let { differences.addAll(it) }

        // 比较自定义选项
        val baseCustomized = base.customizedOption.orEmpty()
        val otherCustomized = other.customizedOption.orEmpty()

        baseCustomized.keys.filter { it !in otherCustomized.keys }.forEach { key ->
            differences.add(
                Difference(
                    "profile.customized-option.$key",
                    baseCustomized[key],
                    null,
                    DiffType.REMOVED
                )
            )
        }

        otherCustomized.keys.filter { it !in baseCustomized.keys }.forEach { key ->
            differences.add(
                Difference(
                    "profile.customized-option.$key",
                    null,
                    otherCustomized[key],
                    DiffType.ADDED
                )
            )
        }

        baseCustomized.keys.intersect(otherCustomized.keys).forEach { key ->
            if (baseCustomized[key] != otherCustomized[key]) {
                differences.add(
                    Difference(
                        "profile.customized-option.$key",
                        baseCustomized[key],
                        otherCustomized[key],
                        DiffType.MODIFIED
                    )
                )
            }
        }

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffTestProfileConfig(base: TestProfileConfig?, other: TestProfileConfig?): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference("profile.test", null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference("profile.test", base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        if (base.noColor != other.noColor) {
            differences.add(Difference("profile.test.no-color", base.noColor, other.noColor, DiffType.MODIFIED))
        }
        if (base.timeoutEach != other.timeoutEach) {
            differences.add(
                Difference(
                    "profile.test.timeout-each",
                    base.timeoutEach,
                    other.timeoutEach,
                    DiffType.MODIFIED
                )
            )
        }
        if (base.randomSeed != other.randomSeed) {
            differences.add(
                Difference(
                    "profile.test.random-seed",
                    base.randomSeed,
                    other.randomSeed,
                    DiffType.MODIFIED
                )
            )
        }
        if (base.bench != other.bench) {
            differences.add(Difference("profile.test.bench", base.bench, other.bench, DiffType.MODIFIED))
        }
        if (base.reportPath != other.reportPath) {
            differences.add(
                Difference(
                    "profile.test.report-path",
                    base.reportPath,
                    other.reportPath,
                    DiffType.MODIFIED
                )
            )
        }
        if (base.reportFormat != other.reportFormat) {
            differences.add(
                Difference(
                    "profile.test.report-format",
                    base.reportFormat,
                    other.reportFormat,
                    DiffType.MODIFIED
                )
            )
        }
        if (base.verbose != other.verbose) {
            differences.add(Difference("profile.test.verbose", base.verbose, other.verbose, DiffType.MODIFIED))
        }

        // 比较环境变量配置
        diffEnvConfig(base.env, other.env, "profile.test.env")?.let { differences.addAll(it) }

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffBenchProfileConfig(base: BenchProfileConfig?, other: BenchProfileConfig?): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference("profile.bench", null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference("profile.bench", base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        if (base.noColor != other.noColor) {
            differences.add(Difference("profile.bench.no-color", base.noColor, other.noColor, DiffType.MODIFIED))
        }
        if (base.randomSeed != other.randomSeed) {
            differences.add(
                Difference(
                    "profile.bench.random-seed",
                    base.randomSeed,
                    other.randomSeed,
                    DiffType.MODIFIED
                )
            )
        }
        if (base.reportPath != other.reportPath) {
            differences.add(
                Difference(
                    "profile.bench.report-path",
                    base.reportPath,
                    other.reportPath,
                    DiffType.MODIFIED
                )
            )
        }
        if (base.baselinePath != other.baselinePath) {
            differences.add(
                Difference(
                    "profile.bench.baseline-path",
                    base.baselinePath,
                    other.baselinePath,
                    DiffType.MODIFIED
                )
            )
        }
        if (base.reportFormat != other.reportFormat) {
            differences.add(
                Difference(
                    "profile.bench.report-format",
                    base.reportFormat,
                    other.reportFormat,
                    DiffType.MODIFIED
                )
            )
        }
        if (base.verbose != other.verbose) {
            differences.add(Difference("profile.bench.verbose", base.verbose, other.verbose, DiffType.MODIFIED))
        }

        // 比较环境变量配置
        diffEnvConfig(base.env, other.env, "profile.bench.env")?.let { differences.addAll(it) }

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffRunProfileConfig(base: RunProfileConfig?, other: RunProfileConfig?): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference("profile.run", null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference("profile.run", base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        // 比较环境变量配置
        diffEnvConfig(base.env, other.env, "profile.run.env")?.let { differences.addAll(it) }

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffEnvConfig(
        base: Map<String, EnvConfig>?,
        other: Map<String, EnvConfig>?,
        path: String
    ): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference(path, null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference(path, base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        // 检查移除的环境变量
        base.keys.filter { it !in other.keys }.forEach { key ->
            differences.add(Difference("$path.$key", base[key], null, DiffType.REMOVED))
        }

        // 检查新增的环境变量
        other.keys.filter { it !in base.keys }.forEach { key ->
            differences.add(Difference("$path.$key", null, other[key], DiffType.ADDED))
        }

        // 检查修改的环境变量
        base.keys.intersect(other.keys).forEach { key ->
            val baseEnv = base[key]
            val otherEnv = other[key]
            if (baseEnv != otherEnv) {
                differences.add(Difference("$path.$key", baseEnv, otherEnv, DiffType.MODIFIED))
            }
        }

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffTargetConfigs(
        base: Map<String, TargetConfig>,
        other: Map<String, TargetConfig>
    ): List<Difference> {
        val differences = mutableListOf<Difference>()

        // 检查移除的目标配置
        base.keys.filter { it !in other.keys }.forEach { target ->
            differences.add(Difference("target.$target", base[target], null, DiffType.REMOVED))
        }

        // 检查新增的目标配置
        other.keys.filter { it !in base.keys }.forEach { target ->
            differences.add(Difference("target.$target", null, other[target], DiffType.ADDED))
        }

        // 检查修改的目标配置
        base.keys.intersect(other.keys).forEach { target ->
            val baseCfg = base[target]!!
            val otherCfg = other[target]!!

            // 比较基本编译选项
            if (baseCfg.compileOption != otherCfg.compileOption) {
                differences.add(
                    Difference(
                        "target.$target.compile-option",
                        baseCfg.compileOption,
                        otherCfg.compileOption,
                        DiffType.MODIFIED
                    )
                )
            }

            // 比较覆盖编译选项
            if (baseCfg.overrideCompileOption != otherCfg.overrideCompileOption) {
                differences.add(
                    Difference(
                        "target.$target.override-compile-option",
                        baseCfg.overrideCompileOption,
                        otherCfg.overrideCompileOption,
                        DiffType.MODIFIED
                    )
                )
            }

            // 比较链接选项
            if (baseCfg.linkOption != otherCfg.linkOption) {
                differences.add(
                    Difference(
                        "target.$target.link-option",
                        baseCfg.linkOption,
                        otherCfg.linkOption,
                        DiffType.MODIFIED
                    )
                )
            }

            // 比较依赖配置
            differences.addAll(
                diffDependencies(
                    "target.$target.dependencies",
                    baseCfg.dependencies.orEmpty(),
                    otherCfg.dependencies.orEmpty()
                )
            )

            // 比较测试依赖配置
            diffDependencies(
                "target.$target.test-dependencies",
                baseCfg.testDependencies.orEmpty(),
                otherCfg.testDependencies.orEmpty()
            ).let { differences.addAll(it) }

            // 比较二进制依赖配置
            diffBinDependencies(
                "target.$target.bin-dependencies",
                baseCfg.binDependencies,
                otherCfg.binDependencies
            )?.let { differences.addAll(it) }

            // 比较目标编译宏
            if (baseCfg.compileMacrosForTarget != otherCfg.compileMacrosForTarget) {
                differences.add(
                    Difference(
                        "target.$target.compile-macros",
                        baseCfg.compileMacrosForTarget,
                        otherCfg.compileMacrosForTarget,
                        DiffType.MODIFIED
                    )
                )
            }

            // 比较调试配置
            if (baseCfg.debug != null || otherCfg.debug != null) {
                differences.addAll(
                    diffTargetConfig(
                        "target.$target.debug",
                        baseCfg.debug,
                        otherCfg.debug
                    )
                )
            }

            // 比较发布配置
            if (baseCfg.release != null || otherCfg.release != null) {
                differences.addAll(
                    diffTargetConfig(
                        "target.$target.release",
                        baseCfg.release,
                        otherCfg.release
                    )
                )
            }
        }

        return differences
    }

    private fun diffBinDependencies(
        path: String,
        base: BinDependenciesConfig?,
        other: BinDependenciesConfig?
    ): List<Difference>? {
        if (base == null && other == null) return null
        if (base == null) return listOf(Difference(path, null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference(path, base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        // 比较路径选项
        val basePaths = base.pathOption.orEmpty()
        val otherPaths = other.pathOption.orEmpty()
        if (basePaths != otherPaths) {
            differences.add(
                Difference(
                    "$path.path-option",
                    basePaths,
                    otherPaths,
                    DiffType.MODIFIED
                )
            )
        }

        // 比较包选项
        val basePackages = base.packageOption.orEmpty()
        val otherPackages = other.packageOption.orEmpty()

        // 检查移除的包选项
        basePackages.keys.filter { it !in otherPackages.keys }.forEach { pkg ->
            differences.add(
                Difference(
                    "$path.package-option.$pkg",
                    basePackages[pkg],
                    null,
                    DiffType.REMOVED
                )
            )
        }

        // 检查新增的包选项
        otherPackages.keys.filter { it !in basePackages.keys }.forEach { pkg ->
            differences.add(
                Difference(
                    "$path.package-option.$pkg",
                    null,
                    otherPackages[pkg],
                    DiffType.ADDED
                )
            )
        }

        // 检查修改的包选项
        basePackages.keys.intersect(otherPackages.keys).forEach { pkg ->
            if (basePackages[pkg] != otherPackages[pkg]) {
                differences.add(
                    Difference(
                        "$path.package-option.$pkg",
                        basePackages[pkg],
                        otherPackages[pkg],
                        DiffType.MODIFIED
                    )
                )
            }
        }

        return differences.takeIf { it.isNotEmpty() }
    }

    private fun diffTargetConfig(
        path: String,
        base: TargetConfig?,
        other: TargetConfig?
    ): List<Difference> {
        if (base == null && other == null) return emptyList()
        if (base == null) return listOf(Difference(path, null, other, DiffType.ADDED))
        if (other == null) return listOf(Difference(path, base, null, DiffType.REMOVED))

        val differences = mutableListOf<Difference>()

        if (base.compileOption != other.compileOption) {
            differences.add(
                Difference(
                    "$path.compile-option",
                    base.compileOption,
                    other.compileOption,
                    DiffType.MODIFIED
                )
            )
        }

        if (base.overrideCompileOption != other.overrideCompileOption) {
            differences.add(
                Difference(
                    "$path.override-compile-option",
                    base.overrideCompileOption,
                    other.overrideCompileOption,
                    DiffType.MODIFIED
                )
            )
        }

        if (base.linkOption != other.linkOption) {
            differences.add(
                Difference(
                    "$path.link-option",
                    base.linkOption,
                    other.linkOption,
                    DiffType.MODIFIED
                )
            )
        }

        differences.addAll(
            diffDependencies(
                "$path.dependencies",
                base.dependencies.orEmpty(),
                other.dependencies.orEmpty()
            )
        )

        differences.addAll(
            diffDependencies(
                "$path.test-dependencies",
                base.testDependencies.orEmpty(),
                other.testDependencies.orEmpty()
            )
        )

        diffBinDependencies(
            "$path.bin-dependencies",
            base.binDependencies,
            other.binDependencies
        )?.let { differences.addAll(it) }

        if (base.compileMacrosForTarget != other.compileMacrosForTarget) {
            differences.add(
                Difference(
                    "$path.compile-macros",
                    base.compileMacrosForTarget,
                    other.compileMacrosForTarget,
                    DiffType.MODIFIED
                )
            )
        }

        return differences
    }

    /**
     * 配置差异类型
     */
    enum class DiffType {
        ADDED,      // 新增配置项
        REMOVED,    // 移除配置项
        MODIFIED    // 修改配置项
    }

    /**
     * 单个配置差异
     */
    data class Difference(
        val path: String,           // 配置项路径
        val oldValue: Any?,         // 原值
        val newValue: Any?,         // 新值
        val type: DiffType          // 差异类型
    )

    /**
     * 配置差异结果
     */
    data class DiffResult(
        val differences: List<Difference>
    ) {
        /**
         * 是否有差异
         */
        val hasDifferences: Boolean
            get() = differences.isNotEmpty()

        /**
         * 获取指定类型的差异
         */
        fun getDifferencesOfType(type: DiffType): List<Difference> {
            return differences.filter { it.type == type }
        }

        /**
         * 获取指定路径的差异
         */
        fun getDifferencesForPath(path: String): List<Difference> {
            return differences.filter { it.path.startsWith(path) }
        }
    }
} 