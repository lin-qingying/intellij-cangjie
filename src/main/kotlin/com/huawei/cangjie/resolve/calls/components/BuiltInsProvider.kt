package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem


interface BuiltInsProvider {
    val builtIns: CangJieBuiltIns
}

//internal val ConstraintSystemBuilder.builtIns: CangJieBuiltIns get() = ((this as NewConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns
//internal val NewConstraintSystem.builtIns: CangJieBuiltIns get() = ((this as NewConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns
