package com.linqingying.cangjie.contracts.model.structure

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.types.CangJieType

sealed class ESType {
    abstract fun toCangJieType(builtIns: CangJieBuiltIns): CangJieType
}
