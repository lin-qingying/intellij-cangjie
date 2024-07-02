package com.huawei.cangjie.contracts.model.structure

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.types.CangJieType

sealed class ESType {
    abstract fun toCangJieType(builtIns: CangJieBuiltIns): CangJieType
}