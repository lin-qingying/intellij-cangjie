package com.linqingying.cangjie.resolve.calls.smartcasts

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.BindingContext


internal fun VariableDescriptor.variableKind(
    usageModule: ModuleDescriptor?,
    bindingContext: BindingContext,
    accessElement: CjElement,
//    languageVersionSettings: LanguageVersionSettings
): DataFlowValue.Kind {
//    if (this is PropertyDescriptor) {
//        return propertyKind(usageModule/*, languageVersionSettings*/)
//    }

//    if (this is LocalVariableDescriptor && this.isDelegated) {
//        // Local delegated property: normally unstable, but can be treated as stable in legacy mode
//        return if (languageVersionSettings.supportsFeature(ProhibitSmartcastsOnLocalDelegatedProperty))
//            DataFlowValue.Kind.PROPERTY_WITH_GETTER
//        else
//            DataFlowValue.Kind.LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY
//
//    }

//    if (this !is LocalVariableDescriptor && this !is ParameterDescriptor) return DataFlowValue.Kind.OTHER
    if (!isVar) return DataFlowValue.Kind.STABLE_VALUE
//    if (this is SyntheticFieldDescriptor) return DataFlowValue.Kind.MUTABLE_PROPERTY

//    // Local variable classification: STABLE or CAPTURED
//    val preliminaryVisitor = PreliminaryDeclarationVisitor.getVisitorByVariable(this, bindingContext)
//    // A case when we just analyse an expression alone: counts as captured
//        ?: return DataFlowValue.Kind.CAPTURED_VARIABLE
//
//    // Analyze who writes variable
//    // If there is no writer: stable
//    val writers = preliminaryVisitor.writers(this)
//    if (writers.isEmpty()) return DataFlowValue.Kind.STABLE_VARIABLE
//
//    // If access element is inside closure: captured
//    val variableContainingDeclaration = this.containingDeclaration
//    if (isAccessedInsideClosure(variableContainingDeclaration, bindingContext, accessElement)) {
//        // stable iff we have no writers in closures AND this closure is AFTER all writers
//        return if (preliminaryVisitor.languageVersionSettings.supportsFeature(CapturedInClosureSmartCasts) &&
//            hasNoWritersInClosures(variableContainingDeclaration, writers, bindingContext) &&
//            isAccessedInsideClosureAfterAllWriters(writers, accessElement)
//        ) {
//            DataFlowValue.Kind.STABLE_VARIABLE
//        } else {
//            DataFlowValue.Kind.CAPTURED_VARIABLE
//        }
//    }

    // Otherwise, stable iff considered position is BEFORE all writers except declarer itself
//    return if (isAccessedBeforeAllClosureWriters(variableContainingDeclaration, writers, bindingContext, accessElement))
//        DataFlowValue.Kind.STABLE_VARIABLE
//    else
        return  DataFlowValue.Kind.CAPTURED_VARIABLE
}
