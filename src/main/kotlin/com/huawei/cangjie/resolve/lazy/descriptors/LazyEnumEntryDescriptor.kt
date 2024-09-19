package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.descriptors.ClassConstructorDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjEnmuEntryInfo
import com.huawei.cangjie.types.CangJieType

class LazyEnumEntryDescriptor(
    c: LazyClassContext,
    thisDescriptor: DeclarationDescriptor,
    name: Name,
    override val classLikeInfo: CjEnmuEntryInfo,
    isExternal: Boolean
) : LazyClassDescriptor(c, thisDescriptor, name, classLikeInfo, isExternal) {
      val types = mutableListOf<CangJieType>()


    private var _constructor: EnumEntryConstructorDescriptor? = null
    fun checkEntrys() {
        val typeReferences = classLikeInfo.typeReferences

        typeReferences.forEach {

            types.add(c.typeResolver.resolveType(scopeForClassHeaderResolution, it, c.trace, false))
        }


    }

    init {
        checkEntrys()
    }

    override fun getConstructors(): MutableCollection<ClassConstructorDescriptor> {
        return mutableListOf(getUnsubstitutedPrimaryConstructor() )
    }
    override fun getUnsubstitutedPrimaryConstructor(): EnumEntryConstructorDescriptor  {
        if (_constructor == null) {
            _constructor = EnumEntryConstructorDescriptor(this, null, this.source) {
                types
            }

        }
        return _constructor!!

    }
}
