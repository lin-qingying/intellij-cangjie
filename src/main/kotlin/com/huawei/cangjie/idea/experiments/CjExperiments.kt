package com.huawei.cangjie.idea.experiments

object CjExperiments {
    @EnabledInStable
    const val BUILD_TOOL_WINDOW = "com.huawei.cangjie.cjpm.build.tool.window"

    @EnabledInStable
    const val EVALUATE_BUILD_SCRIPTS = "com.huawei.cangjie.cjpm.evaluate.build.scripts"

    const val CJPM_FEATURES_SETTINGS_GUTTER = "com.huawei.cangjie.cjpm.features.settings.gutter"

    const val PROC_MACROS = "com.huawei.cangjie.macros.proc"
    @EnabledInStable
    const val FN_LIKE_PROC_MACROS = "com.huawei.cangjie.macros.proc.function-like"
    @EnabledInStable
    const val DERIVE_PROC_MACROS = "com.huawei.cangjie.macros.proc.derive"
    const val ATTR_PROC_MACROS = "com.huawei.cangjie.macros.proc.attr"

    @EnabledInStable
    const val FETCH_ACTUAL_STDLIB_METADATA = "com.huawei.cangjie.cjpm.fetch.actual.stdlib.metadata"

    @EnabledInStable
    const val CRATES_LOCAL_INDEX = "com.huawei.cangjie.crates.local.index"

    @EnabledInStable
    const val WSL_TOOLCHAIN = "com.huawei.cangjie.wsl"

    const val EMULATE_TERMINAL = "com.huawei.cangjie.cjpm.emulate.terminal"

    const val INTENTIONS_IN_FN_LIKE_MACROS = "com.huawei.cangjie.ide.intentions.macros.function-like"

    const val SSR = "com.huawei.cangjie.ssr"

    const val SOURCE_BASED_COVERAGE = "com.huawei.cangjie.coverage.source"

    const val MIR_MOVE_ANALYSIS = "com.huawei.cangjie.mir.move-analysis"
    const val MIR_BORROW_CHECK = "com.huawei.cangjie.mir.borrow-check"
}

/**
 * Experimental feature should be annotated with `@EnabledInStable` if it is enabled in stable releases,
 * i.e. it is included in `resources-stable/META-INF/experiments.xml` with `percentOfUsers="100"`.
 *
 * Enabled experimental features without `@EnabledInStable` annotation are intended to be collected in
 * [com.huawei.cangjie.idea.actions.diagnostic.CreateNewGithubIssue]
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnabledInStable
