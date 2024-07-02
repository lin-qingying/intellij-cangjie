package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.context.GlobalContext
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.resolve.lazy.*

class LocalLazyDeclarationResolver(
    globalContext: GlobalContext,
    trace: BindingTrace,
    private val localClassDescriptorManager: LocalClassDescriptorHolder,
    topLevelDescriptorProvider: TopLevelDescriptorProvider,
    absentDescriptorHandler: AbsentDescriptorHandler
) : LazyDeclarationResolver(globalContext, trace, topLevelDescriptorProvider, absentDescriptorHandler)

class LocalClassDescriptorHolder


class LocalClassifierAnalyzer

class DeclarationScopeProviderForLocalClassifierAnalyzer(
    lazyDeclarationResolver: LazyDeclarationResolver,
    fileScopeProvider: FileScopeProvider,
    private val localClassDescriptorManager: LocalClassDescriptorHolder
) : DeclarationScopeProviderImpl(lazyDeclarationResolver, fileScopeProvider)