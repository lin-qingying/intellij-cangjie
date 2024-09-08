package com.huawei.cangjie.utils

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjPsiFactory
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.calls.DslMarkerUtils
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.getImplicitReceiversHierarchy

fun LexicalScope.getImplicitReceiversWithInstance( ): Collection<ReceiverParameterDescriptor> {
    return getImplicitReceiversWithInstanceToExpression( ).keys
}


interface ReceiverExpressionFactory {
    val isImmediate: Boolean
    val expressionText: String
    fun createExpression(psiFactory: CjPsiFactory, shortThis: Boolean = true): CjExpression
}
fun LexicalScope.getImplicitReceiversWithInstanceToExpression(

): Map<ReceiverParameterDescriptor, ReceiverExpressionFactory?> {


    val outerDeclarationsWithInstance = LinkedHashSet<DeclarationDescriptor>()
    var current: DeclarationDescriptor? = ownerDescriptor
    while (current != null) {
//        if (current is PropertyAccessorDescriptor) {
//            current = current.correspondingProperty
//        }
        outerDeclarationsWithInstance.add(current)

        val classDescriptor = current as? ClassDescriptor
        if (classDescriptor != null &&  !DescriptorUtils.isLocal(classDescriptor)) break

        current = current.containingDeclaration
    }

    val result = LinkedHashMap<ReceiverParameterDescriptor, ReceiverExpressionFactory?>()
//    for ((index, receiver) in receivers.withIndex()) {
//        val owner = receiver.containingDeclaration
//
//
//        val (expressionText, isImmediateThis) = when {
//            owner in outerDeclarationsWithInstance -> {
//                val thisWithLabel = getThisQualifierName(receiver)?.let { "this@${it.render()}" }
//                when (index) {
//                    0 -> (thisWithLabel ?: "this") to true
//                    else -> thisWithLabel to false
//                }
//            }
//            owner is ClassDescriptor && owner.kind.isSingleton -> {
//                IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(owner) to false
//            }
//            else -> continue
//        }
//
//        result[receiver] = if (expressionText != null) createReceiverExpressionFactory(expressionText, isImmediateThis) else null
//    }

    return result
}
private fun getParametersShadowedByDslMarkers(receiverParameters: List<ReceiverParameterDescriptor>): Set<ReceiverParameterDescriptor> {
    val typesByDslScopes = mutableMapOf<FqName, MutableList<ReceiverParameterDescriptor>>()

//    for (receiverParameter in receiverParameters) {
//        val dslMarkers = DslMarkerUtils.extractDslMarkerFqNames(receiverParameter.value).all()
//        for (marker in dslMarkers) {
//            typesByDslScopes.getOrPut(marker) { mutableListOf() } += receiverParameter
//        }
//    }

    // For each DSL marker, all receivers except the closest one are shadowed by it; that is why we drop it
    return typesByDslScopes.values.flatMapTo(mutableSetOf()) { it.drop(1) }
}
private fun createReceiverExpressionFactory(expressionText: String, isImmediateThis: Boolean): ReceiverExpressionFactory {
    return object : ReceiverExpressionFactory {
        override val isImmediate = isImmediateThis
        override val expressionText: String get() = expressionText
        override fun createExpression(psiFactory: CjPsiFactory, shortThis: Boolean): CjExpression {
            return psiFactory.createExpression(if (shortThis && isImmediateThis) "this" else expressionText)
        }
    }
}
