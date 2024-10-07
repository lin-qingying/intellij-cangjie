package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.resolve.LocalVariableResolver
import com.huawei.cangjie.resolve.TypeResolver

class DestructuringDeclarationResolver(

    private val fakeCallResolver: FakeCallResolver,
    private val localVariableResolver: LocalVariableResolver,
    private val typeResolver: TypeResolver
) {

}
