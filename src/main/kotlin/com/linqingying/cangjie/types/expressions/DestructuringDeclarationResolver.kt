package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.resolve.LocalVariableResolver
import com.linqingying.cangjie.resolve.TypeResolver

class DestructuringDeclarationResolver(

    private val fakeCallResolver: FakeCallResolver,
    private val localVariableResolver: LocalVariableResolver,
    private val typeResolver: TypeResolver
) {

}
