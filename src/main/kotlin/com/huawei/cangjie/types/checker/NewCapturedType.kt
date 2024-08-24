package com.huawei.cangjie.types.checker

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.inference.CapturedTypeConstructor
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.*

import com.huawei.cangjie.types.error.ErrorScopeKind
import com.huawei.cangjie.types.model.CaptureStatus
import com.huawei.cangjie.types.model.CapturedTypeMarker
import com.huawei.cangjie.types.util.asTypeProjection
import com.huawei.cangjie.types.util.builtIns


class NewCapturedTypeConstructor(
    override val projection: TypeProjection,
    private var supertypesComputation: (() -> List<UnwrappedType>)? = null,
    private val original: NewCapturedTypeConstructor? = null,
    val typeParameter: TypeParameterDescriptor? = null
) : CapturedTypeConstructor {

    constructor(
        projection: TypeProjection,
        supertypes: List<UnwrappedType>,
        original: NewCapturedTypeConstructor? = null
    ) : this(projection, { supertypes }, original)

    private val _supertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        supertypesComputation?.invoke()
    }

    fun initializeSupertypes(supertypes: List<UnwrappedType>) {
        assert(this.supertypesComputation == null) {
            "Already initialized! oldValue = ${this.supertypesComputation}, newValue = $supertypes"
        }
        this.supertypesComputation = { supertypes }
    }

    override fun getSupertypes() = _supertypes ?: emptyList()
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    //    override fun isFinal() = false
    override fun isDenotable() = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null
    override fun getBuiltIns(): CangJieBuiltIns = projection.type.builtIns

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        NewCapturedTypeConstructor(
            projection.refine(cangjieTypeRefiner),
            supertypesComputation?.let {
                {

                    supertypes.map { it.refine(cangjieTypeRefiner) }

                }
            },
            original ?: this,
            typeParameter = typeParameter
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewCapturedTypeConstructor

        return (original ?: this) === (other.original ?: other)
    }

    override fun hashCode(): Int = original?.hashCode() ?: super.hashCode()
    override fun toString() = "CapturedType($projection)"
}

/**
 * Now [lowerType] is not null only for in projections.
 * Example: `Inv<in String>` For `in String` we create CapturedType with [lowerType] = String.
 *
 * TODO: interface D<T, S: List<T>, D<*, List<Number>> -> D<Q, List<Number>>
 *     We should set [lowerType] for Q as Number. For this we should use constraint system.
 *
 */
class NewCapturedType(
    val captureStatus: CaptureStatus,
    override val constructor: NewCapturedTypeConstructor,
    val lowerType: UnwrappedType?, // todo check lower type for nullable captured types
    override val attributes: TypeAttributes = TypeAttributes.Empty,
    override val isMarkedOption: Boolean = false,
    val isProjectionNotNull: Boolean = false
) : SimpleType(), CapturedTypeMarker {
    internal constructor(
        captureStatus: CaptureStatus,
        lowerType: UnwrappedType?,
        projection: TypeProjection,
        typeParameter: TypeParameterDescriptor
    ) : this(captureStatus, NewCapturedTypeConstructor(projection, typeParameter = typeParameter), lowerType)

    override val arguments: List<TypeProjection> get() = listOf()

    override val memberScope: MemberScope // todo what about foo().bar() where foo() return captured type?
        get() = ErrorUtils.createErrorScope(ErrorScopeKind.CAPTURED_TYPE_SCOPE, throwExceptions = true)

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        NewCapturedType(captureStatus, constructor, lowerType, newAttributes, isMarkedOption, isProjectionNotNull)

    override fun makeNullableAsSpecified(newNullability: Boolean) =
        NewCapturedType(captureStatus, constructor, lowerType, attributes, newNullability)

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        NewCapturedType(
            captureStatus,
            constructor.refine(cangjieTypeRefiner),
            lowerType?.let { cangjieTypeRefiner.refineType(it).unwrap() },
            attributes,
            isMarkedOption
        )
}

private fun captureArguments(type: UnwrappedType, status: CaptureStatus): List<TypeProjection>? {
    if (type.arguments.size != type.constructor.parameters.size) return null

    val arguments = type.arguments
    if (arguments.all { it.projectionKind == Variance.INVARIANT }) return null

    val capturedArguments = arguments.zip(type.constructor.parameters).map { (projection, parameter) ->
        if (projection.projectionKind == Variance.INVARIANT) return@map projection

        val lowerType =
            if (!projection.isStarProjection && projection.projectionKind == Variance.IN_VARIANCE) {
                projection.type.unwrap()
            } else {
                null
            }

        NewCapturedType(
            status,
            lowerType,
            projection,
            parameter
        ).asTypeProjection() // todo optimization: do not create type projection
    }

    val substitutor = TypeConstructorSubstitution.create(type.constructor, capturedArguments).buildSubstitutor()

    for (index in arguments.indices) {
        val oldProjection = arguments[index]
        val newProjection = capturedArguments[index]

        if (oldProjection.projectionKind == Variance.INVARIANT) continue
        val capturedTypeSupertypes = type.constructor.parameters[index].upperBounds.mapTo(mutableListOf()) {
            CangJieTypePreparator.Default.prepareType(substitutor.safeSubstitute(it, Variance.INVARIANT).unwrap())
        }
//
//        if (!oldProjection.isStarProjection && oldProjection.projectionKind == Variance.OUT_VARIANCE) {
//            capturedTypeSupertypes += CangJieTypePreparator.Default.prepareType(oldProjection.type.unwrap())
//        }

        val capturedType = newProjection.type as NewCapturedType
        capturedType.constructor.initializeSupertypes(capturedTypeSupertypes)
    }

    return capturedArguments
}

// this function suppose that input type is simple classifier type
internal fun captureFromArguments(type: SimpleType, status: CaptureStatus) =
    captureArguments(type, status)?.let { type.replaceArguments(it) }

private fun UnwrappedType.replaceArguments(arguments: List<TypeProjection>) =
    CangJieTypeFactory.simpleType(attributes, constructor, arguments, isMarkedOption)

private fun captureFromArguments(type: UnwrappedType, status: CaptureStatus): UnwrappedType? {
    val capturedArguments = captureArguments(type, status) ?: return null

    return if (type is FlexibleType) {
        CangJieTypeFactory.flexibleType(
            type.lowerBound.replaceArguments(capturedArguments),
            type.upperBound.replaceArguments(capturedArguments)
        )
    } else {
        type.replaceArguments(capturedArguments)
    }
}

// null means that type should be leaved as is
fun prepareArgumentTypeRegardingCaptureTypes(argumentType: UnwrappedType): UnwrappedType? {
    return null
//    return
//    if (argumentType is NewCapturedType) null else
//        captureFromExpression(argumentType)
}

fun captureFromExpression(type: UnwrappedType): UnwrappedType? {
    val typeConstructor = type.constructor

    if (typeConstructor !is IntersectionTypeConstructor) {
        return captureFromArguments(type, CaptureStatus.FROM_EXPRESSION)
    }

    /*
     * We capture arguments in the intersection types in specific way:
     *  1) Firstly, we create captured arguments for all type arguments grouped by a type constructor* and a type argument's type.
     *      It means, that we create only one captured argument for two types `Foo<*>` and `Foo<*>?` within a flexible type, for instance.
     *      * In addition to grouping by type constructors, we look at possibility locating of two types in different bounds of the same flexible type.
     *        This is necessary in order to create the same captured arguments,
     *        for example, for `MutableList` in the lower bound of the flexible type and for `List` in the upper one.
     *        Example: MutableList<*>..List<*>? -> MutableList<Captured1(*)>..List<Captured2(*)>?, Captured1(*) and Captured2(*) are the same.
     *  2) Secondly, we replace type arguments with captured arguments by given a type constructor and type arguments.
     */
    val capturedArgumentsByComponents = captureArgumentsForIntersectionType(type) ?: return null

    // We reuse `TypeToCapture` for some types, suitability to reuse defines by `isSuitableForType`
    fun findCorrespondingCapturedArgumentsForType(type: CangJieType) =
        capturedArgumentsByComponents.find { typeToCapture -> typeToCapture.isSuitableForType(type) }?.capturedArguments

    fun replaceArgumentsWithCapturedArgumentsByIntersectionComponents(typeToReplace: UnwrappedType): List<SimpleType> {
        return if (typeToReplace.constructor is IntersectionTypeConstructor) {
            typeToReplace.constructor.supertypes.map { componentType ->
                val capturedArguments = findCorrespondingCapturedArgumentsForType(componentType)
                    ?: return@map componentType.asSimpleType()
                componentType.unwrap().replaceArguments(capturedArguments)
            }
        } else {
            val capturedArguments = findCorrespondingCapturedArgumentsForType(typeToReplace)
                ?: return listOf(typeToReplace.asSimpleType())
            listOf(typeToReplace.unwrap().replaceArguments(capturedArguments))
        }
    }

    return if (type is FlexibleType) {
        val lowerIntersectedType =
            intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.lowerBound))
                .makeNullableAsSpecified(type.lowerBound.isMarkedOption)
        val upperIntersectedType =
            intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type.upperBound))
                .makeNullableAsSpecified(type.upperBound.isMarkedOption)

        CangJieTypeFactory.flexibleType(lowerIntersectedType, upperIntersectedType)
    } else {
        intersectTypes(replaceArgumentsWithCapturedArgumentsByIntersectionComponents(type)).makeNullableAsSpecified(type.isMarkedOption)
    }
}

private fun captureArgumentsForIntersectionType(type: CangJieType): List<CapturedArguments>? {
    // It's possible to have one of the bounds as non-intersection type
    fun getTypesToCapture(type: CangJieType) =
        if (type.constructor is IntersectionTypeConstructor) type.constructor.supertypes else listOf(type)

    val filteredTypesToCapture =
        if (type is FlexibleType) {
            val typesToCapture = getTypesToCapture(type.lowerBound) + getTypesToCapture(type.upperBound)
            typesToCapture.distinctBy {
                (/*FlexibleTypeBoundsChecker.getBaseBoundFqNameByMutability(it) ?: */it.constructor) to it.arguments
            }
        } else type.constructor.supertypes

    var changed = false

    val capturedArgumentsByTypes = filteredTypesToCapture.mapNotNull { typeToCapture ->
        val capturedArguments = captureArguments(typeToCapture.unwrap(), CaptureStatus.FROM_EXPRESSION)
            ?: return@mapNotNull null
        changed = true
        CapturedArguments(capturedArguments, originalType = typeToCapture)
    }

    if (!changed) return null

    return capturedArgumentsByTypes
}
private class CapturedArguments(val capturedArguments: List<TypeProjection>, private val originalType: CangJieType) {
    fun isSuitableForType(type: CangJieType): Boolean {
        val areArgumentsMatched = type.arguments.withIndex().all { (i, typeArgumentsType) ->
            originalType.arguments.size > i && typeArgumentsType == originalType.arguments[i]
        }

        if (!areArgumentsMatched) return false

        val areConstructorsMatched = originalType.constructor == type.constructor
//                || areTypesMayBeLowerAndUpperBoundsOfSameFlexibleTypeByMutability(originalType, type)

        if (!areConstructorsMatched) return false

        return true
    }
}
