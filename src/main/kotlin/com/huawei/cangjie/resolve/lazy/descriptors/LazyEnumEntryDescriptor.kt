package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.descriptors.ClassConstructorDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjClassInfo
import com.huawei.cangjie.resolve.lazy.data.CjEnmuEntryInfo
import com.huawei.cangjie.resolve.source.toSourceElement
import com.huawei.cangjie.types.CangJieType


fun CjClassInfo<*>.toSourceElement(): SourceElement {
    return elementByE.toSourceElement()
}

class LazyEnumEntryDescriptor(
    c: LazyClassContext,
    thisDescriptor: DeclarationDescriptor,
    name: Name,
    val list: List<CjEnmuEntryInfo>,
    isExternal: Boolean
) : LazyClassDescriptor(c, thisDescriptor, name, list.first(), isExternal) {


    override val classLikeInfo: CjEnmuEntryInfo = super.classLikeInfo as CjEnmuEntryInfo


    private var _constructors: MutableList<EnumEntryConstructorDescriptor> = mutableListOf()
    fun checkEntrys() {
//REDECLARATION
        list.forEach {
            val types = mutableListOf<CangJieType>()

            val typeReferences = it.typeReferences

            typeReferences.forEach {

                types.add(c.typeResolver.resolveType(scopeForClassHeaderResolution, it, c.trace, false))
            }

            val descriptor = EnumEntryConstructorDescriptor(this, null, it.toSourceElement()) { types }
            c.trace.record(BindingContext.CLASS, it.elementByE, this)
            _constructors.add(descriptor)

        }

//        寻找相同的方法

        c.overloadResolver.checkOverloadsInPackage(_constructors)

    }

    init {
        checkEntrys()
    }

    //    是否有无参构造
    fun hasUnsubstitutedPrimaryConstructor(): Boolean {
        return _constructors.isEmpty() || _constructors.any { it.valueParameters.isEmpty() }

    }

    override fun getConstructors(): List<ClassConstructorDescriptor> {
        return _constructors.filter { it.valueParameters.isNotEmpty() }
    }


    override fun getUnsubstitutedPrimaryConstructor(): EnumEntryConstructorDescriptor? {
        return null
//        if (_constructor == null) {
//            _constructor = EnumEntryConstructorDescriptor(this, null, this.source) {
//                types
//            }
//
//        }
//        return _constructor!!

    }
}
