package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.error.ErrorScopeKind
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.DFS
import java.util.*
import java.util.stream.Collectors

object CommonSupertypes {
    private fun findCommonSupertype(types: Collection<CangJieType>, recursionDepth: Int, maxDepth: Int): CangJieType {
        assert(recursionDepth <= maxDepth) { "Recursion depth exceeded: $recursionDepth > $maxDepth for types $types" }
        var hasFlexible = false
        val upper: MutableList<SimpleType> = ArrayList(types.size)
        val lower: MutableList<SimpleType> = ArrayList(types.size)
        for (type in types) {
            val unwrappedType: UnwrappedType = type.unwrap()
            if (unwrappedType is FlexibleType) {
                if (unwrappedType.isDynamic()) {
                    return unwrappedType
                }
                hasFlexible = true
                upper.add(unwrappedType.upperBound)
                lower.add(unwrappedType.lowerBound)
            } else {
                val simpleType = unwrappedType as SimpleType
                upper.add(simpleType)
                lower.add(simpleType)
            }
        }

        if (!hasFlexible) return commonSuperTypeForInflexible(upper, recursionDepth, maxDepth)
        return CangJieTypeFactory.flexibleType( // mixing different factories is not supported
            commonSuperTypeForInflexible(lower, recursionDepth, maxDepth),
            commonSuperTypeForInflexible(upper, recursionDepth, maxDepth)
        )
    }

    private fun topologicallySortSuperclassesAndRecordAllInstances(
        type: SimpleType,
        constructorToAllInstances: MutableMap<TypeConstructor, MutableSet<SimpleType>>,
        visited: MutableSet<TypeConstructor>
    ): List<TypeConstructor> {
        return DFS.dfs(
            listOf(type),
            { current ->
                val substitutor: TypeSubstitutor = TypeSubstitutor.create(current)
                val supertypes: Collection<CangJieType> = current.constructor.getSupertypes()
                val result: MutableList<SimpleType> = ArrayList(supertypes.size)
                for (supertype in supertypes) {
                    if (visited.contains(supertype.constructor)) {
                        continue
                    }
                    result.add(
                        substitutor.safeSubstitute(
                            supertype,
                            Variance.INVARIANT
                        ).lowerIfFlexible(

                        )
                    )
                }
                result
            },
            { current -> visited.add(current.constructor) },
            object : DFS.NodeHandlerWithListResult<SimpleType, TypeConstructor>() {
                override fun beforeChildren(current: SimpleType): Boolean {
                    val instances =
                        constructorToAllInstances.computeIfAbsent(
                            current.constructor
                        ) { _: TypeConstructor -> LinkedHashSet<SimpleType>() }
                    instances.add(current)

                    return true
                }

                override fun afterChildren(current: SimpleType) {
                    result.addFirst(current.constructor)
                }
            }
        )
    }

    // Raw supertypes are superclasses w/o type arguments
    // @return TypeConstructor -> all instantiations of this constructor occurring as supertypes
    private fun computeCommonRawSupertypes(types: Collection<SimpleType>): Map<TypeConstructor, Set<SimpleType>> {
        assert(!types.isEmpty())

        val constructorToAllInstances: MutableMap<TypeConstructor, MutableSet<SimpleType>> = HashMap()
        var commonSuperclasses: MutableSet<TypeConstructor>? = null

        var order: List<TypeConstructor>? = null
        for (type in types) {
            val visited: MutableSet<TypeConstructor> = HashSet()
            order = topologicallySortSuperclassesAndRecordAllInstances(
                type,
                constructorToAllInstances,
                visited
            )


            if (commonSuperclasses?.retainAll(visited) == null) {
                commonSuperclasses = visited
            }
        }
        checkNotNull(order)

        val notSource: MutableSet<TypeConstructor> = HashSet()
        val result: MutableMap<TypeConstructor, Set<SimpleType>> = LinkedHashMap()
        for (superConstructor in order) {
            if (!commonSuperclasses!!.contains(superConstructor)) {
                continue
            }

            if (!notSource.contains(superConstructor)) {
                result[superConstructor] = constructorToAllInstances[superConstructor]!!
                markAll(superConstructor, notSource)
            }
        }

        return result
    }

    private fun markAll(typeConstructor: TypeConstructor, markerSet: MutableSet<TypeConstructor>) {
        markerSet.add(typeConstructor)
        for (type in typeConstructor.supertypes) {
            markAll(type.constructor, markerSet)
        }
    }

    private fun classOfDeclarationDescriptor(type: CangJieType): Class<*>? {
        val descriptor = type.constructor.getDeclarationDescriptor()
        if (descriptor != null) {
            return descriptor::class.java
        }

        return null
    }

    private fun commonSuperTypeForInflexible(
        types: Collection<SimpleType>,
        recursionDepth: Int,
        maxDepth: Int
    ): SimpleType {
        assert(!types.isEmpty())
        val typeSet: MutableCollection<SimpleType> = mutableSetOf()

        // If any of the types is nullable, the result must be nullable
        // This also removed Nothing and Nothing? because they are subtypes of everything else
        var nullable = false
        val iterator = typeSet.iterator()
        while (iterator.hasNext()) {
            val type: CangJieType = checkNotNull(iterator.next())
            assert(!type.isFlexible()) { "Flexible type $type passed to commonSuperTypeForInflexible" }
            if (CangJieBuiltIns.isNothing(type)) {
                iterator.remove()
            }
            if (type.isError) {
                return ErrorUtils.createErrorType(ErrorTypeKind.SUPER_TYPE_FOR_ERROR_TYPE, type.toString())
            }
            nullable = nullable or type.isMarkedOption
        }

        // Everything deleted => it's Nothing or Nothing?
        if (typeSet.isEmpty()) {
            // TODO : attributes
            val builtIns = types.iterator().next().constructor.getBuiltIns()
            return /*if (nullable) builtIns.nullableNothingType else */builtIns.nothingType
        }

        if (typeSet.size == 1) {
            return TypeUtils.makeOptionalIfNeeded(typeSet.iterator().next(), nullable)
        }

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        var commonSupertypes: Map<TypeConstructor, Set<SimpleType>> =
            computeCommonRawSupertypes(typeSet)
        while (commonSupertypes.size > 1) {
            val merge: MutableSet<SimpleType> = LinkedHashSet()
            for (supertypes in commonSupertypes.values) {
                merge.addAll(supertypes)
            }
            commonSupertypes = computeCommonRawSupertypes(merge)
        }

        if (commonSupertypes.isEmpty()) {
            val info = StringBuilder()
            for (type in types) {
                val superTypes: String = TypeUtils.getAllSupertypes(type).stream()
                    .map { t -> "-- " + renderTypeFully(t) }
                    .collect(Collectors.joining("\n"))

                info
                    .append("Info about ").append(renderTypeFully(type)).append(": ").append('\n')
                    .append("- Supertypes: ").append('\n')
                    .append(superTypes).append('\n')
                    .append("- DeclarationDescriptor class: ")
                    .append(classOfDeclarationDescriptor(type)).append('\n')
                    .append('\n')
            }
            throw IllegalStateException("[Report version 3] There is no common supertype for: $types \n$info")
        }

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        val entry = commonSupertypes.entries.iterator().next()

        // Reconstructing type arguments if possible
        val result: SimpleType =
            computeSupertypeProjections(entry.key, entry.value, recursionDepth, maxDepth)
        return TypeUtils.makeOptionalIfNeeded(result, nullable)
    }

    private fun renderTypeFully(type: CangJieType): String {
        return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type) + ", typeConstructor debug: " +
                renderTypeConstructorVerboseDebugInformation(type.constructor)
    }

    private fun renderTypeConstructorVerboseDebugInformation(typeConstructor: TypeConstructor): String {
        if (typeConstructor !is AbstractTypeConstructor) {
            return (typeConstructor.toString() + "[" + typeConstructor::class.simpleName) + "]"
        }

        val declarationDescriptor = typeConstructor.declarationDescriptor
        val moduleDescriptorString = Objects.toString(DescriptorUtils.getContainingModule(declarationDescriptor))
        return "descriptor=" + declarationDescriptor + "@" + Integer.toHexString(Objects.hashCode(declarationDescriptor)) +
                ", moduleDescriptor=" + moduleDescriptorString +
                ", " + typeConstructor.renderAdditionalDebugInformation()
    }

    // constructor - type constructor of a supertype to be instantiated
    // types - instantiations of constructor occurring as supertypes of classes we are trying to intersect
    private fun computeSupertypeProjections(
        constructor: TypeConstructor,
        types: Set<SimpleType>,
        recursionDepth: Int,
        maxDepth: Int
    ): SimpleType {
        // we assume that all the given types are applications of the same type constructor

        assert(types.isNotEmpty())

        if (types.size == 1) {
            return types.iterator().next()
        }

        val parameters: List<TypeParameterDescriptor> = constructor.parameters
        val newProjections: MutableList<TypeProjection> = ArrayList(parameters.size)
        for (parameterDescriptor in parameters) {
            val typeProjections: MutableSet<TypeProjection> = LinkedHashSet()
            for (type in types) {
                typeProjections.add(type.arguments[parameterDescriptor.getIndex()])
            }
            newProjections.add(
                computeSupertypeProjection(
                    parameterDescriptor,
                    typeProjections,
                    recursionDepth,
                    maxDepth
                )
            )
        }

        var nullable = false
        for (type in types) {
            nullable = nullable or type.isMarkedOption
        }

        val newScope: MemberScope = when (val classifier: ClassifierDescriptor? = constructor.declarationDescriptor) {
            is ClassDescriptor -> {
                classifier.getMemberScope(newProjections)
            }

            is TypeParameterDescriptor -> {
                classifier.getDefaultType().memberScope
            }

            else -> {
                ErrorUtils.createErrorScope(ErrorScopeKind.NON_CLASSIFIER_SUPER_TYPE_SCOPE, true)
            }
        }
        return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty,
            constructor,
            newProjections,
            nullable,
            newScope
        )
    }

    private fun depth(type: CangJieType): Int {
        return 1 + maxDepth(type.arguments.map { projection ->

            projection.getType()
        })
    }

    private fun computeSupertypeProjection(
        parameterDescriptor: TypeParameterDescriptor,
        typeProjections: Set<TypeProjection>,
        recursionDepth: Int, maxDepth: Int
    ): TypeProjection {
        val singleBestProjection = typeProjections.singleBestRepresentative()
        if (singleBestProjection != null) {
            return singleBestProjection
        }


//        var ins: MutableSet<CangJieType?>? = LinkedHashSet<CangJieType?>()
        var outs: MutableSet<CangJieType> = LinkedHashSet<CangJieType>()
//
        val variance = parameterDescriptor.variance
        when (variance) {
            Variance.INVARIANT -> {}

        }
        for (projection in typeProjections) {


            outs.add(projection.type)

        }
//
//        if (outs != null) {
//            assert(!outs.isEmpty()) { "Out projections is empty for parameter $parameterDescriptor, type projections $typeProjections" }
        val projectionKind = Variance.INVARIANT
        val superType: CangJieType = findCommonSupertype(outs, recursionDepth + 1, maxDepth)

        return TypeProjectionImpl(projectionKind, superType)
//        }
//        if (ins != null) {
//            assert(!ins.isEmpty()) { "In projections is empty for parameter $parameterDescriptor, type projections $typeProjections" }
//            val intersection: CangJieType = TypeIntersector.intersectTypes(ins)
//                ?: return TypeUtils.makeStarProjection(parameterDescriptor)
//            val projectionKind = if (variance === IN_VARIANCE) Variance.INVARIANT else IN_VARIANCE
//            return TypeProjectionImpl(projectionKind, intersection)
//        } else {
//            return TypeUtils.makeStarProjection(parameterDescriptor)
//        }


    }

    fun commonSupertype(types: Collection<CangJieType>): CangJieType {
        if (types.size == 1) return types.iterator().next()
        // Recursion should not be significantly deeper than the deepest type in question
        // It can be slightly deeper, though: e.g. when initial types are simple, but their supertypes are complex
        return findCommonSupertype(types, 0, maxDepth(types) + 3)
    }

    private fun maxDepth(types: Collection<CangJieType>): Int {
        var max = 0
        for (type in types) {
            val depth: Int = depth(type)
            if (max < depth) {
                max = depth
            }
        }
        return max
    }

    fun commonSupertypeForNonDenotableTypes(types: Collection<CangJieType>): CangJieType? {
        if (types.isEmpty()) return null
        if (types.size == 1) {
            val type: CangJieType = types.iterator().next()
            if (type.constructor is IntersectionTypeConstructor) {
                return commonSupertypeForNonDenotableTypes(type.constructor.getSupertypes())
            }
        }
        return commonSupertype(types)
    }

}
