package com.linqingying.cangjie.types

import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.annotations.FilteredAnnotations

abstract class TypeSubstitution{
    companion object {
        @JvmField
        val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: CangJieType): Nothing? = null
            override fun isEmpty() = true
            override fun toString() = "Empty TypeSubstitution"
        }
    }
    // This can be used to perform preliminary manipulations with top-level types
    open fun prepareTopLevelType(topLevelType: CangJieType, position: Variance): CangJieType = topLevelType
    open fun filterAnnotations(annotations: Annotations) = annotations

    abstract operator fun get(key: CangJieType): TypeProjection?
    fun buildSubstitutor(): TypeSubstitutor = TypeSubstitutor.create(this)
    fun replaceWithNonApproximating() = object : TypeSubstitution() {
        override fun get(key: CangJieType) = this@TypeSubstitution[key]
        override fun approximateCapturedTypes() = false
        override fun approximateContravariantCapturedTypes() = false
        override fun filterAnnotations(annotations: Annotations) = this@TypeSubstitution.filterAnnotations(annotations)
        override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) =
            this@TypeSubstitution.prepareTopLevelType(topLevelType, position)

        override fun isEmpty() = this@TypeSubstitution.isEmpty()
    }
    open fun isEmpty(): Boolean = false

    open fun approximateCapturedTypes(): Boolean = false
    open fun approximateContravariantCapturedTypes(): Boolean = false
}


abstract class TypeConstructorSubstitution : TypeSubstitution() {
    override fun get(key: CangJieType) = get(key.constructor)

    abstract fun get(key: TypeConstructor): TypeProjection?

    companion object {
        @JvmStatic
        @JvmOverloads
        fun createByConstructorsMap(
            map: Map<TypeConstructor, TypeProjection>,
            approximateCapturedTypes: Boolean = false
        ): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key]
                override fun isEmpty() = map.isEmpty()
                override fun approximateCapturedTypes() = approximateCapturedTypes
            }

        @JvmStatic
        fun createByParametersMap(map: Map<TypeParameterDescriptor, TypeProjection>): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key.declarationDescriptor]
                override fun isEmpty() = map.isEmpty()
            }

        @JvmStatic
        fun create(CangJieType: CangJieType) = create(CangJieType.constructor, CangJieType.arguments)

        @JvmStatic
        fun create(typeConstructor: TypeConstructor, arguments: List<TypeProjection>): TypeSubstitution {
            val parameters = typeConstructor.parameters

            if (parameters.lastOrNull()?.isCapturedFromOuterDeclaration == true) {
                return createByConstructorsMap(typeConstructor.parameters.map { it.typeConstructor }.zip(arguments).toMap())
            }

            return IndexedParametersSubstitution(parameters, arguments)
        }
    }
}

class IndexedParametersSubstitution(
    val parameters: Array<TypeParameterDescriptor>,
    val arguments: Array<TypeProjection>,
    private val approximateContravariantCapturedTypes: Boolean = false
) : TypeSubstitution() {
    init {
        assert(parameters.size <= arguments.size) {
            "Number of arguments should not be less than number of parameters, but: parameters=${parameters.size}, args=${arguments.size}"
        }
    }

    constructor(
        parameters: List<TypeParameterDescriptor>,
        argumentsList: List<TypeProjection>
    ) : this(parameters.toTypedArray(), argumentsList.toTypedArray())

    override fun isEmpty(): Boolean = arguments.isEmpty()

    override fun approximateContravariantCapturedTypes() = approximateContravariantCapturedTypes

    override fun get(key: CangJieType): TypeProjection? {
        val parameter = key.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null
        val index = parameter.index

        if (index < parameters.size && parameters[index].typeConstructor == parameter.typeConstructor) {
            return arguments[index]
        }

        return null
    }
}
@JvmOverloads
fun SimpleType.replace(
    newArguments: List<TypeProjection> = arguments,
    newAttributes: TypeAttributes = attributes
): SimpleType {
    if (newArguments.isEmpty() && newAttributes === attributes) return this

    if (newArguments.isEmpty()) {
        return replaceAttributes(newAttributes)
    }

//    if (this is ErrorType) {
//        return replaceArguments(newArguments)
//    }

    return CangJieTypeFactory.simpleType(
        newAttributes,
        constructor,
        newArguments,
        isMarkedOption
    )
}

open class DelegatedTypeSubstitution(val substitution: TypeSubstitution) : TypeSubstitution() {
    override fun get(key: CangJieType) = substitution[key]
//    override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) =
//        substitution.prepareTopLevelType(topLevelType, position)

    override fun isEmpty() = substitution.isEmpty()

    override fun approximateCapturedTypes() = substitution.approximateCapturedTypes()
    override fun approximateContravariantCapturedTypes() = substitution.approximateContravariantCapturedTypes()

//    override fun filterAnnotations(annotations: Annotations) = substitution.filterAnnotations(annotations)
}
@JvmOverloads
fun CangJieType.replace(
    newArguments: List<TypeProjection> = arguments,
    newAnnotations: Annotations = annotations,
    newArgumentsForUpperBound: List<TypeProjection> = newArguments
): CangJieType {
    if ((newArguments.isEmpty() || newArguments === arguments) && newAnnotations === annotations) return this

    val newAttributes = attributes.replaceAnnotations(
        // Specially handle FilteredAnnotations here due to FilteredAnnotations.isEmpty()
        if (newAnnotations is FilteredAnnotations && newAnnotations.isEmpty()) Annotations.EMPTY else newAnnotations
    )

    return when (val unwrapped = unwrap()) {
        is FlexibleType -> CangJieTypeFactory.flexibleType(
            unwrapped.lowerBound.replace(newArguments, newAttributes),
            unwrapped.upperBound.replace(newArgumentsForUpperBound, newAttributes)
        )
        is SimpleType -> unwrapped.replace(newArguments, newAttributes)
    }
}

class SubstitutionWithCapturedTypeApproximation(substitution: TypeSubstitution) : DelegatedTypeSubstitution(substitution) {
    override fun approximateCapturedTypes() = true
}
