package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.SupertypeLoopChecker
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
interface EnumTypeConstructor : TypeConstructor {

    override val declarationDescriptor: EnumDescriptor

}

/**
 * 枚举类型构造函数
 *
 * 表示枚举类型的构造函数，负责创建枚举类型实例。
 *
 * 特点：
 * - 包含枚举描述符
 * - 支持类型参数
 * - 支持类型参数声明
 * - 支持声明描述符
 *
 * 示例：
 * ```kotlin
 * val enumConstructor = EnumTypeConstructor(enumDescriptor)
 * val enumType = enumConstructor.createType(typeArguments)
 * ```
 */
class EnumTypeConstructorImpl(
     private val enumDescriptor: EnumDescriptor,
    parameters: List<TypeParameterDescriptor>,
    override val  supertypes: List<CangJieType>,
    storageManager: StorageManager
) :  AbstractClassTypeConstructor(storageManager), EnumTypeConstructor {

    /**
     * 声明描述符
     */
    override val declarationDescriptor: EnumDescriptor = enumDescriptor

    /**
     * 内置类型信息
     */
    override val builtIns: CangJieBuiltIns
        get() = enumDescriptor.builtIns

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
        return this
    }

    override fun computeSupertypes(): Collection<CangJieType> = supertypes
    override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> = emptyList()


    override val supertypeLoopChecker: SupertypeLoopChecker = SupertypeLoopChecker.EMPTY

    override val parameters: List<TypeParameterDescriptor> = parameters.toList()


    /**
     * 是否最终
     */
    override val isFinal: Boolean = true

    /**
     * 是否拒绝
     */
    override val isDenotable: Boolean = true



    /**
     * 字符串表示
     *
     * @return 枚举类型构造函数的字符串表示
     */
    override fun toString(): String {
        return "EnumTypeConstructor(${enumDescriptor.name})"
    }

    /**
     * 相等性比较
     *
     * @param other 要比较的对象
     * @return true如果相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnumTypeConstructor) return false

        return declarationDescriptor == other.declarationDescriptor
    }

    /**
     * 哈希码
     *
     * @return 哈希码
     */
    override fun hashCode(): Int {
        return enumDescriptor.hashCode()
    }
} 