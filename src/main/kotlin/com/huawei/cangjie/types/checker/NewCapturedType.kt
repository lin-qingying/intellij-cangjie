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
//    override fun isDenotable() = false
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
    override val isMarkedNullable: Boolean = false,
    val isProjectionNotNull: Boolean = false
) : SimpleType(), CapturedTypeMarker {
    internal constructor(
        captureStatus: CaptureStatus, lowerType: UnwrappedType?, projection: TypeProjection, typeParameter: TypeParameterDescriptor
    ) : this(captureStatus, NewCapturedTypeConstructor(projection, typeParameter = typeParameter), lowerType)

    override val arguments: List<TypeProjection> get() = listOf()

    override val memberScope: MemberScope // todo what about foo().bar() where foo() return captured type?
        get() = ErrorUtils.createErrorScope(ErrorScopeKind.CAPTURED_TYPE_SCOPE, throwExceptions = true)

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        NewCapturedType(captureStatus, constructor, lowerType, newAttributes, isMarkedNullable, isProjectionNotNull)

    override fun makeNullableAsSpecified(newNullability: Boolean) =
        NewCapturedType(captureStatus, constructor, lowerType, attributes, newNullability)

    @TypeRefinement
    override fun refine(cangjieTypeRefiner:CangJieTypeRefiner) =
        NewCapturedType(
            captureStatus,
            constructor.refine(cangjieTypeRefiner),
            lowerType?.let { cangjieTypeRefiner.refineType(it).unwrap() },
            attributes,
            isMarkedNullable
        )
}



// null means that type should be leaved as is
fun prepareArgumentTypeRegardingCaptureTypes(argumentType: UnwrappedType): UnwrappedType? {
    return  null
//    return
//    if (argumentType is NewCapturedType) null else
//        captureFromExpression(argumentType)
}
