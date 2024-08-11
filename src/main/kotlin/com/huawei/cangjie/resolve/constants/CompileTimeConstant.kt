package com.huawei.cangjie.resolve.constants

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.findClassAcrossModuleDependencies
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.util.TypeUtils

fun hasUnsignedTypesInModuleDependencies(module: ModuleDescriptor): Boolean {
    return module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt32ClassId) != null
}
interface CompileTimeConstant<out T>{
    val isError: Boolean
        get() = false
    val parameters: Parameters
    val moduleDescriptor: ModuleDescriptor
    fun toConstantValue(expectedType: CangJieType): ConstantValue<T>

    data class Parameters(
        val canBeUsedInAnnotation: Boolean,
        val isPure: Boolean,
        // `isUnsignedNumberLiteral` means that this constant represents simple number literal with `u` suffix (123u, 0xFEu)
        val isUnsignedNumberLiteral: Boolean,
        // `isUnsignedLongNumberLiteral` means that this constant represents simple number literal with `{uU}{lL}` suffix (123uL, 0xFEUL)
        val isUnsignedLongNumberLiteral: Boolean,
        val usesVariableAsConstant: Boolean,
        val usesNonConstValAsConstant: Boolean,
        // `isConvertableConstVal` means that this is `const val` that can participate in signed to unsigned conversion
        // see LanguageFeature.ImplicitSignedToUnsignedIntegerConversion
        val isConvertableConstVal: Boolean
    )
}
class IntegerValueTypeConstant(
    private val value: Number,
    override val moduleDescriptor: ModuleDescriptor,
    override val parameters: CompileTimeConstant.Parameters,
    private val newInferenceEnabled: Boolean,
    val convertedFromSigned: Boolean = false
) : CompileTimeConstant<Number> {
    companion object {
        @JvmStatic
        fun IntegerValueTypeConstant.convertToUnsignedConstant(module: ModuleDescriptor): IntegerValueTypeConstant {
            val newParameters = CompileTimeConstant.Parameters(
                parameters.canBeUsedInAnnotation,
                parameters.isPure,
                isUnsignedNumberLiteral = true,
                isUnsignedLongNumberLiteral = parameters.isUnsignedLongNumberLiteral,
                usesVariableAsConstant = parameters.usesVariableAsConstant,
                usesNonConstValAsConstant = parameters.usesNonConstValAsConstant,
                isConvertableConstVal = parameters.isConvertableConstVal
            )

            return IntegerValueTypeConstant(value, module, newParameters, newInferenceEnabled, convertedFromSigned = true)
        }

        fun IntegerValueTypeConstant.convertToSignedConstant(module: ModuleDescriptor): IntegerValueTypeConstant {
            val newParameters = CompileTimeConstant.Parameters(
                parameters.canBeUsedInAnnotation,
                parameters.isPure,
                isUnsignedNumberLiteral = false,
                isUnsignedLongNumberLiteral = parameters.isUnsignedLongNumberLiteral,
                usesVariableAsConstant = parameters.usesVariableAsConstant,
                usesNonConstValAsConstant = parameters.usesNonConstValAsConstant,
                isConvertableConstVal = parameters.isConvertableConstVal
            )

            return IntegerValueTypeConstant(value, module, newParameters, newInferenceEnabled, convertedFromSigned = true)
        }
    }

    private val typeConstructor =
        if (newInferenceEnabled) {
            IntegerLiteralTypeConstructor(value.toLong(), moduleDescriptor, parameters)
        } else {
            IntegerValueTypeConstructor(value.toLong(), moduleDescriptor, parameters)
        }

    override fun toConstantValue(expectedType: CangJieType): ConstantValue<Number> {
        val type = getType(expectedType)
//     TODO   转为常量
        return when {
            CangJieBuiltIns.isInt32(type) -> Int32Value(value.toInt())
//            CangJieBuiltIns.isByte(type) -> ByteValue(value.toByte())
//            CangJieBuiltIns.isShort(type) -> ShortValue(value.toShort())
//            CangJieBuiltIns.isLong(type) -> LongValue(value.toLong())

//            CangJieBuiltIns.isUInt(type) -> UIntValue(value.toInt())
//            CangJieBuiltIns.isUByte(type) -> UByteValue(value.toByte())
//            CangJieBuiltIns.isUShort(type) -> UShortValue(value.toShort())
//            CangJieBuiltIns.isULong(type) -> ULongValue(value.toLong())

            else -> Int32Value(value.toInt())
        }
    }
//
//    val unknownIntegerType = CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
//        TypeAttributes.Empty, typeConstructor, emptyList(), false,
//        ErrorUtils.createErrorScope(ErrorScopeKind.INTEGER_LITERAL_TYPE_SCOPE, throwExceptions = true, typeConstructor.toString())
//    )

    fun getType(expectedType: CangJieType): CangJieType =
        if (newInferenceEnabled) {
            TypeUtils.getPrimitiveNumberType(typeConstructor as IntegerLiteralTypeConstructor, expectedType)
        } else {
            TypeUtils.getPrimitiveNumberType(typeConstructor as IntegerValueTypeConstructor, expectedType)
        }

    override fun toString() = typeConstructor.toString()

    override fun equals(other: Any?) = other is IntegerValueTypeConstant && value == other.value && parameters == other.parameters

    override fun hashCode() = 31 * value.hashCode() + parameters.hashCode()

//    override val hasIntegerLiteralType: Boolean
//        get() = true
}
class TypedCompileTimeConstant<out T>(
    val constantValue: ConstantValue<T>,
    override val moduleDescriptor: ModuleDescriptor,
    override val parameters: CompileTimeConstant.Parameters
) : CompileTimeConstant<T> {

    override val isError: Boolean
        get() = constantValue is ErrorValue

    val type: CangJieType = constantValue.getType(moduleDescriptor)

    override fun toConstantValue(expectedType: CangJieType): ConstantValue<T> = constantValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypedCompileTimeConstant<*>) return false
        if (isError) return other.isError
        if (other.isError) return false
        return constantValue.value == other.constantValue.value && type == other.type
    }

    override fun hashCode(): Int {
        if (isError) return 13
        var result = constantValue.value?.hashCode() ?: 0
        result = 31 * result + type.hashCode()
        return result
    }

//    override val hasIntegerLiteralType: Boolean
//        get() = false
}
