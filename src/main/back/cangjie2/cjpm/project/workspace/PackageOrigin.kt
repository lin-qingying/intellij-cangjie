package com.huawei.cangjie.cjpm.project.workspace

enum class PackageOrigin {
    /**
     * 该程序包来自标准库
     */
    STDLIB,

    /**
     * 该包是工作区的一部分。
     */
    WORKSPACE,

    /**
     * [工作区]或其他[依赖项]包的外部依赖项
     */
    DEPENDENCY,

    /**
     * [STDLIB]或其他[STDLIB_Dependency]包的外部依赖项
     */
    STDLIB_DEPENDENCY
}
