package com.huawei.cangjie.ide.codeinsight

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.ide.FrontendInternals
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade

@OptIn(FrontendInternals::class)
class ReferenceVariantsHelper(
    private val bindingContext: BindingContext,
    private val resolutionFacade: ResolutionFacade,
    private val moduleDescriptor: ModuleDescriptor,
    private val visibilityFilter: (DeclarationDescriptor) -> Boolean,
    private val notProperties: Set<FqNameUnsafe> = setOf()
)
