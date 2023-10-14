package com.huawei.cangjie.cjpm.util

enum class StdLibType {

    ROOT,


    FEATURE_GATED,


    DEPENDENCY
}


data class StdLibInfo(
    val name: String,
    val type: StdLibType,
    val dependencies: List<String> = emptyList()
)

object AutoInjectedCrates {
    const val STD: String = "std"
    const val CORE: String = "core"
    const val TEST: String = "test"
    val stdlibCrates = listOf(

        StdLibInfo(CORE, StdLibType.ROOT),
        StdLibInfo(STD, StdLibType.ROOT, dependencies = listOf("alloc", "panic_unwind", "panic_abort",
            CORE, "libc", "compiler_builtins", "profiler_builtins", "unwind")),
        StdLibInfo("alloc", StdLibType.ROOT, dependencies = listOf(CORE, "compiler_builtins")),
        StdLibInfo("proc_macro", type = StdLibType.ROOT, dependencies = listOf(STD)),
        StdLibInfo(TEST, type = StdLibType.ROOT, dependencies = listOf(STD, CORE, "libc", "getopts", "term")),

        StdLibInfo("libc", StdLibType.FEATURE_GATED),
        StdLibInfo("panic_unwind", type = StdLibType.FEATURE_GATED, dependencies = listOf(CORE, "libc", "alloc",
            "unwind", "compiler_builtins")),
        StdLibInfo("compiler_builtins", StdLibType.FEATURE_GATED, dependencies = listOf(CORE)),
        StdLibInfo("profiler_builtins", StdLibType.FEATURE_GATED, dependencies = listOf(CORE, "compiler_builtins")),
        StdLibInfo("panic_abort", StdLibType.FEATURE_GATED, dependencies = listOf(CORE, "libc", "compiler_builtins")),
        StdLibInfo("unwind", StdLibType.FEATURE_GATED, dependencies = listOf(CORE, "libc", "compiler_builtins")),
        StdLibInfo("term", StdLibType.FEATURE_GATED, dependencies = listOf(STD, CORE)),
        StdLibInfo("getopts", StdLibType.FEATURE_GATED, dependencies = listOf(STD, CORE)),
    )
}
