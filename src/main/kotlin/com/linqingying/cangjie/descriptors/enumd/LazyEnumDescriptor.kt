package com.linqingying.cangjie.descriptors.enumd

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.Errors.ENUM_REDECLARATION
import com.linqingying.cangjie.diagnostics.Errors.REDECLARATION
import com.linqingying.cangjie.diagnostics.reportOnDeclaration
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.lazy.LazyClassContext
import com.linqingying.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyClassDescriptor

class LazyEnumDescriptor(
    c: LazyClassContext,
    classLikeInfo: CjClassLikeInfo,

    containingDeclaration: DeclarationDescriptor,
    name: Name,


    ) : LazyClassDescriptor(
    c,
    containingDeclaration, name,
    classLikeInfo, false
) {

init {
    checkArgumentBySize()
}
//    不能在构造器中调用，会引发循环调用，因为只能自底向上解析
    val entrys
        get() = unsubstitutedMemberScope.getContributedDescriptors { true }.filter { DescriptorUtils.isEnumEntry(it) }

    val entrysByNameGroup get() = entrys.groupBy { it.name }

    val entrysByPsi get() = classLikeInfo.declarations.filterIsInstance<CjEnumEntry>()
    val entrysByPsiByNameGroup get() = entrysByPsi.groupBy { it.name }
    fun checkArgumentBySize() {
        entrysByPsiByNameGroup .forEach { (name, entrys) ->

            val map =      entrys.groupBy {  it.typeReferences.size}
            map.values.forEach { values ->
                if (values.size > 1)

                  values.forEach {
                      c.trace.report(ENUM_REDECLARATION.on(it, name) )
                  }


            }
        }
//        entrysByNameGroup.forEach { (name, declarationDescriptors) ->
//
//            val map =      declarationDescriptors.groupBy {  (it as EnumEntryDescriptor).unsubstitutedPrimaryConstructor.valueParameters.size}
//            map.values.forEach { values ->
//                if (values.size > 1)
//                    reportOnDeclaration(c.trace, values.first()) {
//                        REDECLARATION.on(it, values)
//                    }
//
//            }
//        }


    }

}
