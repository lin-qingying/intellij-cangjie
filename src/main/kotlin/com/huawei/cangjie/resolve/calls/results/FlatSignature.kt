package com.huawei.cangjie.resolve.calls.results

import com.huawei.cangjie.builtins.getValueParameterTypesFromCallableReflectionType
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.MemberDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import com.huawei.cangjie.types.AbstractTypeChecker
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.model.*

interface SpecificityComparisonCallbacks {
    fun isNonSubtypeNotLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean
}

class TypeWithConversion(val resultType: CangJieTypeMarker?, val originalTypeIfWasConverted: CangJieTypeMarker? = null)

@JvmName("createWithConvertedTypes")
fun <T> FlatSignature.Companion.create(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    parameterTypes: List<TypeWithConversion?>,
): FlatSignature<T> {
    val extensionReceiverType = descriptor.extensionReceiverParameter?.type
    val contextReceiverTypes = descriptor.contextReceiverParameters.mapNotNull { TypeWithConversion(it.type) }

    return FlatSignature(
        origin,
        descriptor.typeParameters,
        valueParameterTypes = contextReceiverTypes + extensionReceiverType?.let { listOf(TypeWithConversion(it)) }.orEmpty() + parameterTypes,
        hasExtensionReceiver = extensionReceiverType != null,
        contextReceiverCount = contextReceiverTypes.size,
//        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isExpect = descriptor is MemberDescriptor /*&& descriptor.isExpect*/,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}

class FlatSignature<out T> constructor(
    val origin: T,
    val typeParameters: Collection<TypeParameterMarker>,
    val hasExtensionReceiver: Boolean,
    val contextReceiverCount: Int,

    val numDefaults: Int,
    val isExpect: Boolean,
    val isSyntheticMember: Boolean,
    val valueParameterTypes: List<TypeWithConversion?>,
)
{
    val isGeneric = typeParameters.isNotEmpty()

    constructor(
        origin: T,
        typeParameters: Collection<TypeParameterMarker>,
        valueParameterTypes: List<CangJieTypeMarker?>,
        hasExtensionReceiver: Boolean,
        contextReceiverCount: Int,

        numDefaults: Int,
        isExpect: Boolean,
        isSyntheticMember: Boolean,
    ) : this(
        origin, typeParameters, hasExtensionReceiver, contextReceiverCount,  numDefaults, isExpect,
        isSyntheticMember, valueParameterTypes.map(::TypeWithConversion)
    )
    companion object
}
interface SimpleConstraintSystem {
    fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker
    fun addSubtypeConstraint(subType: CangJieTypeMarker, superType: CangJieTypeMarker)
    fun hasContradiction(): Boolean

    // todo hack for migration
    val captureFromArgument get() = false

    val context: TypeSystemInferenceExtensionContext
}
fun <D : CallableDescriptor> FlatSignature.Companion.createFromCallableDescriptor(descriptor: D): FlatSignature<D> =
    FlatSignature(
        descriptor,
        descriptor.typeParameters,
        valueParameterTypes = descriptor.contextReceiverParameters.map { it.type }
                + listOfNotNull(descriptor.extensionReceiverParameter?.type)
                + descriptor.valueParameters.map { it.argumentValueType },
        hasExtensionReceiver = descriptor.extensionReceiverParameter?.type != null,
        contextReceiverCount = descriptor.contextReceiverParameters.size,

        numDefaults = 0,
        isExpect = descriptor is MemberDescriptor  ,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
val ValueParameterDescriptor.argumentValueType get() =  type
fun <T> SimpleConstraintSystem.isSignatureNotLessSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    useOriginalSamTypes: Boolean = false
): Boolean {
    if (specific.hasExtensionReceiver != general.hasExtensionReceiver) return false
    if (specific.contextReceiverCount > general.contextReceiverCount) return false
    if (specific.valueParameterTypes.size - specific.contextReceiverCount != general.valueParameterTypes.size - general.contextReceiverCount)
        return false

    if (!isValueParameterTypeNotLessSpecific(specific, general, callbacks, specificityComparator) { it?.resultType }) {
        return false
    }

    if (useOriginalSamTypes && !isValueParameterTypeNotLessSpecific(
            specific, general, callbacks, specificityComparator
        ) { it?.originalTypeIfWasConverted }
    ) {
        return false
    }

    return !hasContradiction()
}
private fun <T> SimpleConstraintSystem.isValueParameterTypeNotLessSpecific(
    specific: FlatSignature<T>,
    general: FlatSignature<T>,
    callbacks: SpecificityComparisonCallbacks,
    specificityComparator: TypeSpecificityComparator,
    typeKindSelector: (TypeWithConversion?) -> CangJieTypeMarker?
): Boolean {
    val typeParameters = general.typeParameters
    val typeSubstitutor = registerTypeVariables(typeParameters)

    val specificContextReceiverCount = specific.contextReceiverCount
    val generalContextReceiverCount = general.contextReceiverCount

    var specificValueParameterTypes = specific.valueParameterTypes
    var generalValueParameterTypes = general.valueParameterTypes

    if (specificContextReceiverCount != generalContextReceiverCount) {
        specificValueParameterTypes = specificValueParameterTypes.drop(specificContextReceiverCount)
        generalValueParameterTypes = generalValueParameterTypes.drop(generalContextReceiverCount)
    }

    for (index in specificValueParameterTypes.indices) {
        val specificType = typeKindSelector(specificValueParameterTypes[index]) ?: continue
        val generalType = typeKindSelector(generalValueParameterTypes[index]) ?: continue

        if (specificityComparator.isDefinitelyLessSpecific(specificType, generalType)) {
            return false
        }

        if (typeParameters.isEmpty() || !generalType.dependsOnTypeParameters(context, typeParameters)) {
            if (!AbstractTypeChecker.isSubtypeOf(context, specificType, generalType)) {
                if (!callbacks.isNonSubtypeNotLessSpecific(specificType, generalType)) {
                    return false
                }
            }
        } else {
            val substitutedGeneralType = typeSubstitutor.safeSubstitute(context, generalType)

            /**
             * Example:
             * fun <X> Array<out X>.sort(): Unit {}
             * fun <Y: Comparable<Y>> Array<out Y>.sort(): Unit {}
             * Here, when we try solve this CS(Y is variables) then Array<out X> <: Array<out Y> and this system impossible to solve,
             * so we capture types from receiver and value parameters.
             */
            val specificCapturedType = AbstractTypeChecker.prepareType(context, specificType)
                .let { if (captureFromArgument) context.captureFromExpression(it) ?: it else it }
            addSubtypeConstraint(specificCapturedType, substitutedGeneralType)
        }
    }

    return true
}
fun <T> FlatSignature.Companion.createFromReflectionType(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    // Reflection type for callable references with bound receiver doesn't contain receiver type
    hasBoundExtensionReceiver: Boolean,
    reflectionType: UnwrappedType
): FlatSignature<T> {
    // Note that receiver is taking over descriptor, not reflection type
    // This is correct as extension receiver can't have any defaults/varargs/coercions, so there is no need to use reflection type
    // Plus, currently, receiver for reflection type is taking from *candidate*, see buildReflectionType, this candidate can
    // have transient receiver which is not the same in its signature
    val receiver = descriptor.extensionReceiverParameter?.type
    val contextReceiversTypes = descriptor.contextReceiverParameters.mapNotNull { it.type }
    val parameters = if (descriptor is VariableDescriptor) {
        emptyList()
    } else {
        reflectionType.getValueParameterTypesFromCallableReflectionType(
            receiver != null && !hasBoundExtensionReceiver
        ).map { it.type }
    }

    return FlatSignature(
        origin,
        descriptor.typeParameters,
        contextReceiversTypes + listOfNotNull(receiver) + parameters,
        hasExtensionReceiver = receiver != null,
        contextReceiverCount = contextReceiversTypes.size,
//        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isExpect = descriptor is MemberDescriptor /*&& descriptor.isExpect*/,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}
object OverloadabilitySpecificityCallbacks : SpecificityComparisonCallbacks {
    override fun isNonSubtypeNotLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean =
        false
}
