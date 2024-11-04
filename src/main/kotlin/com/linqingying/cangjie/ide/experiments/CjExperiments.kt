package com.linqingying.cangjie.ide.experiments

object CjExperiments {
    @EnabledInStable
    const val BUILD_TOOL_WINDOW = "com.linqingying.cangjie.cjpm.build.tool.window"

    @EnabledInStable
    const val EVALUATE_BUILD_SCRIPTS = "com.linqingying.cangjie.cjpm.evaluate.build.scripts"

    const val CJPM_FEATURES_SETTINGS_GUTTER = "com.linqingying.cangjie.cjpm.features.settings.gutter"

    const val PROC_MACROS = "com.linqingying.cangjie.macros.proc"
    @EnabledInStable
    const val FN_LIKE_PROC_MACROS = "com.linqingying.cangjie.macros.proc.function-like"
    @EnabledInStable
    const val DERIVE_PROC_MACROS = "com.linqingying.cangjie.macros.proc.derive"
    const val ATTR_PROC_MACROS = "com.linqingying.cangjie.macros.proc.attr"

    @EnabledInStable
    const val FETCH_ACTUAL_STDLIB_METADATA = "com.linqingying.cangjie.cjpm.fetch.actual.stdlib.metadata"

    @EnabledInStable
    const val CRATES_LOCAL_INDEX = "com.linqingying.cangjie.crates.local.index"

    @EnabledInStable
    const val WSL_TOOLCHAIN = "com.linqingying.cangjie.wsl"

    const val EMULATE_TERMINAL = "com.linqingying.cangjie.cjpm.emulate.terminal"

    const val INTENTIONS_IN_FN_LIKE_MACROS = "com.linqingying.cangjie.ide.intentions.macros.function-like"

    const val SSR = "com.linqingying.cangjie.ssr"

    const val SOURCE_BASED_COVERAGE = "com.linqingying.cangjie.coverage.source"

    const val MIR_MOVE_ANALYSIS = "com.linqingying.cangjie.mir.move-analysis"
    const val MIR_BORROW_CHECK = "com.linqingying.cangjie.mir.borrow-check"
}

/**
 * Experimental feature should be annotated with `@EnabledInStable` if it is enabled in stable releases,
 * i.e. it is included in `resources-stable/META-INF/experiments.xml` with `percentOfUsers="100"`.
 *
 * Enabled experimental features without `@EnabledInStable` annotation are intended to be collected in
 * [com.linqingying.cangjie.ide.actions.diagnostic.CreateNewGithubIssue]
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnabledInStable
