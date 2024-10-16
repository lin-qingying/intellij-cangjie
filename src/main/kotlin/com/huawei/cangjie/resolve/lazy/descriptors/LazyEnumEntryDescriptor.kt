package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.descriptors.ClassConstructorDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.huawei.cangjie.diagnostics.Errors.REDECLARATION
import com.huawei.cangjie.diagnostics.reportOnDeclaration
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.descriptorUtil.classId
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjClassInfo
import com.huawei.cangjie.resolve.lazy.data.CjEnmuEntryInfo
import com.huawei.cangjie.resolve.source.toSourceElement
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.ClassAndEnumConstructorDescriptor


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
    override val isStatic: Boolean
        get() = true
    override val visibility: DescriptorVisibility = containingDeclaration.visibility
    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> = emptySet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyEnumEntryDescriptor) return false


        if (other.name != name) return false
        if (other.list.size != list.size) return false
        if (other._constructors.size != _constructors.size) return false

        if (!(list.any { other.list.any { otherit -> otherit.name == it.name } })) {
            return false
        }
        if (this.classId != other.classId) {
            return false
        }

        return true
    }

    override val classLikeInfo: CjEnmuEntryInfo = super.classLikeInfo as CjEnmuEntryInfo
//    override val classLikeInfo: CjEnmuEntryInfo = list.last()


    var _constructors: MutableList<EnumEntryConstructorDescriptor> = mutableListOf()
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
//        c.overloadResolver.checkOverloadsInPackage(_constructors)
        checkArgumentBySize()
    }

    fun checkArgumentBySize() {
        val map = _constructors.groupBy {
            it.valueParameters.size
        }
        map.values.forEach { values ->
            if (values.size > 1)
                reportOnDeclaration(c.trace, values.first()) {
                    REDECLARATION.on(it, values)
                }

        }
    }

    init {
        checkEntrys()
    }

    //    是否有无参构造
    fun hasUnsubstitutedPrimaryConstructor(): Boolean {
        return (_constructors.isEmpty() || _constructors.any { it.valueParameters.isEmpty() }) && containingDeclaration is LazyClassDescriptor &&
                (containingDeclaration as LazyClassDescriptor).declaredTypeParameters.isEmpty()

    }

    fun getEnumEntryConstructorDescriptors(): List<ClassAndEnumConstructorDescriptor> {
        return _constructors.map {
            if (it.valueParameters.isEmpty()) {
                this
            } else {
                it
            }
        }

    }

    override fun getConstructors(): List<ClassConstructorDescriptor> {
        return _constructors/*.filter { it.valueParameters.isNotEmpty() }*/
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

    override fun hashCode(): Int {
        var result = list.hashCode()
        result = 31 * result + classLikeInfo.hashCode()
        result = 31 * result + _constructors.hashCode()
        return result
    }
}
