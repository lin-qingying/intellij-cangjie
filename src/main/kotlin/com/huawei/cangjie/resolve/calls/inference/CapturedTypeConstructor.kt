package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.LockBasedStorageManager
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.error.ErrorScopeKind
import com.huawei.cangjie.types.model.CapturedTypeConstructorMarker
import com.huawei.cangjie.types.model.CapturedTypeMarker
import com.huawei.cangjie.types.util.builtIns

fun TypeSubstitution.wrapWithCapturingSubstitution(needApproximation: Boolean = true): TypeSubstitution =
    if (this is IndexedParametersSubstitution)
        IndexedParametersSubstitution(
            this.parameters,
            this.arguments.zip(this.parameters).map {
                it.first.createCapturedIfNeeded(it.second)
            }.toTypedArray(),
            approximateContravariantCapturedTypes = needApproximation
        )
    else
        object : DelegatedTypeSubstitution(this@wrapWithCapturingSubstitution) {
            override fun approximateContravariantCapturedTypes() = needApproximation
            override fun get(key: CangJieType) =
                super.get(key)
                    ?.createCapturedIfNeeded(key.constructor.declarationDescriptor as? TypeParameterDescriptor)
        }

private fun TypeProjection.createCapturedIfNeeded(typeParameterDescriptor: TypeParameterDescriptor?): TypeProjection {
    if (typeParameterDescriptor == null || projectionKind == Variance.INVARIANT) return this

    // Treat consistent projections as invariant
    if (typeParameterDescriptor.variance == projectionKind) {
        // TODO: Make star projection type lazy

            TypeProjectionImpl(this@createCapturedIfNeeded.type)
    }

    return TypeProjectionImpl(createCapturedType(this))
}

fun CangJieType.isCaptured(): Boolean = constructor is CapturedTypeConstructor

fun createCapturedType(typeProjection: TypeProjection): CangJieType = CapturedType(typeProjection)


interface CapturedTypeConstructor : CapturedTypeConstructorMarker, TypeConstructor {
    val projection: TypeProjection
}

class CapturedTypeConstructorImpl(
    override val projection: TypeProjection
) : CapturedTypeConstructor {
//    var newTypeConstructor: NewCapturedTypeConstructor? = null

    init {
        assert(projection.projectionKind != Variance.INVARIANT) {
            "Only nontrivial projections can be captured, not: $projection"
        }
    }

    override fun getParameters(): List<TypeParameterDescriptor> = listOf()

    override fun getSupertypes(): Collection<CangJieType> {
        val superType =
//            if (projection.projectionKind == Variance.OUT_VARIANCE)
            projection.type
//        else
//            builtIns.nullableAnyType
        return listOf(superType)
    }

    override fun isFinal() = true
//
    override fun isDenotable() = false

    override fun getDeclarationDescriptor() = null

    override fun toString() = "CapturedTypeConstructor($projection)"

    override fun getBuiltIns(): CangJieBuiltIns = projection.type.constructor.builtIns

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: CangJieTypeRefiner) =
        CapturedTypeConstructorImpl(projection.refine(kotlinTypeRefiner))
}


class CapturedType(
    val typeProjection: TypeProjection,
    override val constructor: CapturedTypeConstructor = CapturedTypeConstructorImpl(typeProjection),
    override val isMarkedOption: Boolean = false,
    override val attributes: TypeAttributes = TypeAttributes.Empty
) : SimpleType(), SubtypingRepresentatives, CapturedTypeMarker {

    override val arguments: List<TypeProjection>
        get() = listOf()

    override val memberScope: MemberScope
        get() = ErrorUtils.createErrorScope(
            ErrorScopeKind.CAPTURED_TYPE_SCOPE,
            throwExceptions = true
        )

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        CapturedType(typeProjection.refine(cangjieTypeRefiner), constructor, isMarkedOption, attributes)

    override val subTypeRepresentative: CangJieType
        get() = representative(Variance.INVARIANT, builtIns.anyType)

    override val superTypeRepresentative: CangJieType
        get() = representative(Variance.INVARIANT, builtIns.nothingType)

    private fun representative(variance: Variance, default: CangJieType) =
        if (typeProjection.projectionKind == variance) typeProjection.type else default

    override fun sameTypeConstructor(type: CangJieType) = constructor === type.constructor

    override fun toString() = "Captured($typeProjection)" + if (isMarkedOption) "?" else ""

    override fun makeOptionalAsSpecified(newNullability: Boolean): CapturedType {
        if (newNullability == isMarkedOption) return this
        return CapturedType(typeProjection, constructor, newNullability, attributes)
    }

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        CapturedType(typeProjection, constructor, isMarkedOption, newAttributes)

//    @TypeRefinement
//    override fun refine(kotlinTypeRefiner: CangJieTypeRefiner) =
//        CapturedType(typeProjection.refine(kotlinTypeRefiner), constructor, isMarkedOption, attributes)
}
