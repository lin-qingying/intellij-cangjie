package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.context.GlobalContext
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.resolve.lazy.*

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
