package com.linqingying.cangjie.types


import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.resolve.scopes.TypeIntersectionScope
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.model.IntersectionTypeConstructorMarker


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

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSupertypes(): Collection<CangJieType> = intersectedTypes

    // Type should not be rendered in scope's debug name. This may cause performance issues in case of complicated intersection types.
    fun createScopeForCangJieType(): MemberScope =
        TypeIntersectionScope.create("member scope for intersection type", intersectedTypes)

    override fun isFinal(): Boolean = false

    override fun isDenotable(): Boolean = false

    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns(): CangJieBuiltIns =
        intersectedTypes.iterator().next().constructor.builtIns


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
