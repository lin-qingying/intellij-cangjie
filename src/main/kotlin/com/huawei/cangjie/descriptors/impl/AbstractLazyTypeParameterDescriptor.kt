package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.SupertypeLoopChecker
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.Variance

abstract class AbstractLazyTypeParameterDescriptor(

    storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    variance: Variance,
//    isReified: Boolean,
    index: Int,
    source: SourceElement,
    supertypeLoopChecker: SupertypeLoopChecker
) :
    AbstractTypeParameterDescriptor(
        storageManager, containingDeclaration, annotations, name, variance, /*isReified, */index, source,
        supertypeLoopChecker
    ) {

    override fun toString(): String {
        // Not using descriptor renderer to preserve laziness
        return String.format(
            "%s%s%s",
  "",
           "",
            name
        )
    }

}
