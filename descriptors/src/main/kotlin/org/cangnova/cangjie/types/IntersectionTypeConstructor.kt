/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types


import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.TypeIntersectionScope
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.model.IntersectionTypeConstructorMarker


//类型推导
class IntersectionTypeConstructor(typesToIntersect: Collection<CangJieType>) : TypeConstructor,
    IntersectionTypeConstructorMarker {
    private var alternative: CangJieType? = null

    private constructor(
        typesToIntersect: Collection<CangJieType>,
        alternative: CangJieType?,
    ) : this(typesToIntersect) {
        this.alternative = alternative
    }

    init {
        assert(!typesToIntersect.isEmpty()) { "Attempt to create an empty intersection" }
    }

    private val intersectedTypes = LinkedHashSet(typesToIntersect)
    private val hashCode = intersectedTypes.hashCode()

    override val parameters: List<TypeParameterDescriptor>
        get() = emptyList()
    override val supertypes: Collection<CangJieType>
        get() = intersectedTypes
    // Type should not be rendered in scope's debug name. This may cause performance issues in case of complicated intersection types.
    fun createScopeForCangJieType(): MemberScope =
        TypeIntersectionScope.create("member scope for intersection type", intersectedTypes)

    override val isFinal : Boolean = false

    override val isDenotable : Boolean = false

    override val declarationDescriptor: ClassifierDescriptor?
        get() = null

    override val builtIns: CangJieBuiltIns
        get() = intersectedTypes.iterator().next().constructor.builtIns

    override fun toString(): String = makeDebugNameForIntersectionType()

    fun makeDebugNameForIntersectionType(getProperTypeRelatedToStringify: (CangJieType) -> Any = { it.toString() }): String {
        return intersectedTypes.sortedBy { getProperTypeRelatedToStringify(it).toString() }
            .joinToString(
                separator = " & ",
                prefix = "{",
                postfix = "}"
            ) { getProperTypeRelatedToStringify(it).toString() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntersectionTypeConstructor) return false

        return intersectedTypes == other.intersectedTypes
    }

    @OptIn(TypeRefinement::class)
    fun createType(): SimpleType =
        CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty, this, listOf(), false, this.createScopeForCangJieType()
        ) { cangjieTypeRefiner ->
            this.refine(cangjieTypeRefiner).createType()
        }

    override fun hashCode(): Int = hashCode

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) =
        transformComponents { it.refine(cangjieTypeRefiner) } ?: this

    fun setAlternative(alternative: CangJieType?): IntersectionTypeConstructor {
        return IntersectionTypeConstructor(intersectedTypes, alternative)
    }

    fun getAlternativeType(): CangJieType? = alternative
}

inline fun IntersectionTypeConstructor.transformComponents(
    predicate: (CangJieType) -> Boolean = { true },
    transform: (CangJieType) -> CangJieType
): IntersectionTypeConstructor? {
    var changed = false
    val newSupertypes = supertypes.map {
        if (predicate(it)) {
            changed = true
            transform(it)
        } else {
            it
        }
    }

    if (!changed) return null

    val updatedAlternative = getAlternativeType()?.let { alternative ->
        if (predicate(alternative)) transform(alternative) else alternative
    }

    return IntersectionTypeConstructor(newSupertypes).setAlternative(updatedAlternative)
}
