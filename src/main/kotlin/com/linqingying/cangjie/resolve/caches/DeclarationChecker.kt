package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.resolve.MissingSupertypesResolver
import com.linqingying.cangjie.resolve.calls.checkers.CheckerContext
import com.linqingying.cangjie.resolve.deprecation.DeprecationResolver


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
