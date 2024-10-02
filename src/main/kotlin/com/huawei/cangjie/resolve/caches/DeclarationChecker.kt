package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
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
fun PropertyDescriptor.getEffectiveModality(languageVersionSettings: LanguageVersionSettings): Modality =
    when (languageVersionSettings.supportsFeature(LanguageFeature.TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck)) {
        true -> getEffectiveModality()
        false -> modality
    }
private fun PropertyDescriptor.getEffectiveModality(): Modality =
    when (modality == Modality.OPEN && (containingDeclaration as? ClassDescriptor)?.modality == Modality.FINAL) {
        true -> Modality.FINAL
        false -> modality
    }
