package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.ReceiverParameterDescriptor
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.ResolutionFacade
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValue
import com.linqingying.cangjie.resolve.calls.smartcasts.IdentifierInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.Nullability
import com.linqingying.cangjie.resolve.dataFlowValueFactory
import com.linqingying.cangjie.resolve.getDataFlowInfoBefore
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.getResolutionScope
import com.linqingying.cangjie.resolve.scopes.receivers.ImplicitReceiver
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.isSubtypeOf
import com.linqingying.cangjie.types.util.makeNotNullable
import com.linqingying.cangjie.utils.getImplicitReceiversWithInstance
import com.intellij.psi.PsiElement
import javaslang.Tuple2
import java.util.HashMap

private operator fun <T> Tuple2<T, *>.component1(): T = _1()
private operator fun <T> Tuple2<*, T>.component2(): T = _2()

class SmartCastCalculator(
    val bindingContext: BindingContext,
    private val containingDeclarationOrModule: DeclarationDescriptor,
    contextElement: PsiElement,
    receiver: CjExpression?,
    resolutionFacade: ResolutionFacade
) {
    private val dataFlowValueFactory = resolutionFacade.dataFlowValueFactory

    // keys are VariableDescriptor's and ThisReceiver's
    private val entityToSmartCastInfo: Map<Any, SmartCastInfo> = processDataFlowInfo(
        bindingContext.getDataFlowInfoBefore(contextElement),
        contextElement.getResolutionScope(bindingContext, resolutionFacade),
        receiver
    )

    fun types(descriptor: VariableDescriptor): Collection<CangJieType> {
        val type = descriptor.returnType ?: return emptyList()
        return entityType(descriptor, type)
    }

    fun types(thisReceiverParameter: ReceiverParameterDescriptor): Collection<CangJieType> {
        val type = thisReceiverParameter.type
        val thisReceiver = thisReceiverParameter.value as? ImplicitReceiver ?: return listOf(type)
        return entityType(thisReceiver, type)
    }

    private fun entityType(entity: Any, ownType: CangJieType): Collection<CangJieType> {
        val smartCastInfo = entityToSmartCastInfo[entity] ?: return listOf(ownType)

        var types = smartCastInfo.types + ownType

        if (smartCastInfo.notNull) {
            types = types.map { it.makeNotNullable() }
        }

        return types
    }

    private data class SmartCastInfo(var types: Collection<CangJieType>, var notNull: Boolean) {
        constructor() : this(emptyList(), false)
    }

    private fun processDataFlowInfo(
        dataFlowInfo: DataFlowInfo,
        resolutionScope: LexicalScope?,
        receiver: CjExpression?
    ): Map<Any, SmartCastInfo> {
        if (dataFlowInfo == DataFlowInfo.EMPTY) return emptyMap()

        val dataFlowValueToEntity: (DataFlowValue) -> Any?
        if (receiver != null) {
            val receiverType = bindingContext.getType(receiver) ?: return emptyMap()
            val receiverIdentifierInfo = dataFlowValueFactory.createDataFlowValue(
                receiver, receiverType, bindingContext, containingDeclarationOrModule
            ).identifierInfo
            dataFlowValueToEntity = { value ->
                val identifierInfo = value.identifierInfo
                if (identifierInfo is IdentifierInfo.Qualified && identifierInfo.receiverInfo == receiverIdentifierInfo) {
                    (identifierInfo.selectorInfo as? IdentifierInfo.Variable)?.variable
                } else null
            }
        } else {
            dataFlowValueToEntity = fun(value: DataFlowValue): Any? {
                when (val identifierInfo = value.identifierInfo) {
                    is IdentifierInfo.Variable -> return identifierInfo.variable
                    is IdentifierInfo.Receiver -> return identifierInfo.value as? ImplicitReceiver

                    is IdentifierInfo.Qualified -> {
                        val receiverInfo = identifierInfo.receiverInfo
                        val selectorInfo = identifierInfo.selectorInfo
                        if (receiverInfo !is IdentifierInfo.Receiver || selectorInfo !is IdentifierInfo.Variable) return null
                        val receiverValue = receiverInfo.value as? ImplicitReceiver ?: return null
                        if (resolutionScope?.findNearestReceiverForVariable(selectorInfo.variable)?.value != receiverValue) return null
                        return selectorInfo.variable
                    }

                    else -> return null
                }
            }
        }

        val entityToInfo = HashMap<Any, SmartCastInfo>()

        for ((dataFlowValue, types) in dataFlowInfo.completeTypeInfo) {
            val entity = dataFlowValueToEntity.invoke(dataFlowValue)
            if (entity != null) {
                entityToInfo[entity] = SmartCastInfo(types.toJavaList(), false)
            }
        }

        for ((dataFlowValue, nullability) in dataFlowInfo.completeNullabilityInfo) {
            if (nullability == Nullability.NOT_NULL) {
                val entity = dataFlowValueToEntity(dataFlowValue) ?: continue
                entityToInfo.getOrPut(entity) { SmartCastInfo() }.notNull = true
            }
        }

        return entityToInfo
    }

    private fun LexicalScope.findNearestReceiverForVariable(variableDescriptor: VariableDescriptor): ReceiverParameterDescriptor? {
        val classifier = variableDescriptor.containingDeclaration as? ClassifierDescriptor ?: return null
        val type = classifier.defaultType
        return getImplicitReceiversWithInstance().firstOrNull { it.type.isSubtypeOf(type) }
    }
}
