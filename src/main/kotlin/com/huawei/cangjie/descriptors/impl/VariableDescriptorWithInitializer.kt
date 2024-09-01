package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.storage.NullableLazyValue
import com.huawei.cangjie.types.CangJieType

abstract class VariableDescriptorWithInitializerImpl(
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    outType: CangJieType?,
    override val isVar: Boolean,
    source: SourceElement
) : AbstractVariableDescriptor(containingDeclaration, annotations, name, outType, source) {
    private var compileTimeInitializer: NullableLazyValue<ConstantValue<*>>? =
        null
    private var compileTimeInitializerFactory: () -> NullableLazyValue<ConstantValue<*>>? =
        {
            null
        }

    fun setCompileTimeInitializerFactory(compileTimeInitializerFactory: () -> NullableLazyValue<ConstantValue<*>>) {
        assert(!isVar) { "Constant value for variable initializer should be recorded only for final variables: $name" }
        setCompileTimeInitializer(null, compileTimeInitializerFactory)
    }

    override fun cleanCompileTimeInitializerCache() {
        this.compileTimeInitializer = compileTimeInitializerFactory.invoke()
    }

    override fun getCompileTimeInitializer(): ConstantValue<*>? {
        if (compileTimeInitializer != null) {
            return compileTimeInitializer!!.invoke()
        }
        return null
    }



    fun setCompileTimeInitializer(
        compileTimeInitializer: NullableLazyValue<ConstantValue<*>>?,
        compileTimeInitializerFactory: () -> NullableLazyValue<ConstantValue<*>>
    ) {
        assert(!isVar) { "Constant value for variable initializer should be recorded only for final variables: " + name }
        this.compileTimeInitializerFactory = compileTimeInitializerFactory
        this.compileTimeInitializer =
            compileTimeInitializer ?: compileTimeInitializerFactory.invoke()
    }
}
