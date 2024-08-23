package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.resolve.MissingSupertypesResolver
import com.huawei.cangjie.resolve.calls.checkers.CheckerContext
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver


interface DeclarationChecker {
    fun check(declaration: CjDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext)
}

class DeclarationCheckerContext(
    override val trace: BindingTrace,
    override val languageVersionSettings: LanguageVersionSettings,
    override val deprecationResolver: DeprecationResolver,
    override val moduleDescriptor: ModuleDescriptor,
//    val expectActualTracker: ExpectActualTracker,
    val missingSupertypesResolver: MissingSupertypesResolver
) : CheckerContext
