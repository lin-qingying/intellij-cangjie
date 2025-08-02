/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.resolve.call.inference

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.storage.LockBasedStorageManager
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.error.ErrorScopeKind
import cn.cangnova.cangjie.types.model.CapturedTypeConstructorMarker
import cn.cangnova.cangjie.types.model.CapturedTypeMarker

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
//        assert(projection.projectionKind != Variance.INVARIANT) {
//            "Only nontrivial projections can be captured, not: $projection"
//        }
    }

    override val parameters: List<TypeParameterDescriptor> = listOf()


    override val supertypes: Collection<CangJieType>
        get() {

            val superType =
//            if (projection.projectionKind == Variance.OUT_VARIANCE)
                projection.type
//        else
//            builtIns.nullableAnyType
            return listOf(superType)
        }

    override val isFinal: Boolean
        get() = true
    override val isDenotable: Boolean
        get() = false
    override val declarationDescriptor: ClassifierDescriptor?
        get() = null
    override fun toString() = "CapturedTypeConstructor($projection)"

    override val builtIns: CangJieBuiltIns
        get() =  projection.type.constructor.builtIns
    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        CapturedTypeConstructorImpl(projection.refine(cangjieTypeRefiner))
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


}
