package com.linqingying.cangjie.cjpm.project.workspace

/**
 * Defines a reason a package is in a project.
 */
enum class PackageOrigin {
    /**
     * 该包来自标准库
     */
    STDLIB,

    /**
     * 该包是工作区的一部分。
     */
    WORKSPACE,

    /**
     * [WORKSPACE]或其他[DEPENDENCY]包的外部依赖项
     */
    DEPENDENCY,

    /**
     * [STDLIB]或其他[STDLIB_Dependency]包的外部依赖项
     */
    STDLIB_DEPENDENCY
}
