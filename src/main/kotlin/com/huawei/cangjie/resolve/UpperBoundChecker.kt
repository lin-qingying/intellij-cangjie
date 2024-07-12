package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.types.checker.CangJieTypeChecker

@DefaultImplementation(impl = UpperBoundChecker::class)
open class UpperBoundChecker(
    private val typeChecker: CangJieTypeChecker,
)
