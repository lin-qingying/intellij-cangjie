package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.SupertypeLoopChecker
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.Variance

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
