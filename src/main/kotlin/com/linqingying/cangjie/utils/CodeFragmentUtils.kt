package com.linqingying.cangjie.utils

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.psi.CjCodeFragment
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.types.CangJieType
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import kotlin.reflect.KProperty

object CodeFragmentUtils {
    val RUNTIME_TYPE_EVALUATOR: Key<Function1<CjExpression, CangJieType?>> = Key.create("RUNTIME_TYPE_EVALUATOR")

    // Identifier that the codeFragment is used in the debugger evaluator for compilation. See [PerFileAnalysisCache.getAnalysisResults]
    val USED_FOR_COMPILATION_IN_IR_EVALUATOR: Key<Boolean> = Key.create("USED_FOR_COMPILATION_IN_EVALUATOR")
}

var CjCodeFragment.externalDescriptors: List<DeclarationDescriptor>? by CopyablePsiUserDataProperty(Key.create("EXTERNAL_DESCRIPTORS"))

class CopyablePsiUserDataProperty<in R : PsiElement, T : Any>(val key: Key<T>) {
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getCopyableUserData(key)

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T?) = thisRef.putCopyableUserData(key, value)
}
