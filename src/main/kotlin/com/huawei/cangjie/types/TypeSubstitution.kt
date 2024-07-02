package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.types.error.ErrorType

abstract class TypeSubstitution{
    companion object {
        @JvmField
        val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: CangJieType): Nothing? = null
            override fun isEmpty() = true
            override fun toString() = "Empty TypeSubstitution"
        }
    }

    abstract operator fun get(key: CangJieType): TypeProjection?
    fun buildSubstitutor(): TypeSubstitutor = TypeSubstitutor.create(this)

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
        fun create(kotlinType: CangJieType) = create(kotlinType.constructor, kotlinType.arguments)

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
        isMarkedNullable
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
