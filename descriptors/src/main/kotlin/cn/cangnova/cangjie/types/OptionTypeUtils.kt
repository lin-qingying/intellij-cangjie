package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.builtins.StandardNames.CORE_PACKAGE_NAME
import cn.cangnova.cangjie.builtins.StandardNames.OPTION

/**
 * Option类型工具类 - 简化设计，适合IDE插件
 * 提供Option类型的核心操作
 */
object OptionTypeUtils {

    /**
     * 检测类型是否为Option类型
     */
    fun isOptionType(type: CangJieType): Boolean {
        return type.isOption
    }

    /**
     * 解包Option类型
     */
    fun unboxOptionType(type: CangJieType): CangJieType {
        var currentType = type
        while (currentType.isOption) {
            currentType = currentType.unwrapOption()
        }
        return currentType
    }

    /**
     * 计算Option嵌套层级
     */
    fun countOptionNestedLevel(type: CangJieType): Int {
        return type.countOptionNestedLevel()
    }

    /**
     * 创建Option类型
     */
    fun createOptionType(innerType: CangJieType): OptionType {
        return OptionType(innerType)
    }

    /**
     * 创建嵌套的Option类型
     */
    fun createNestedOptionType(baseType: CangJieType, level: Int): CangJieType {
        if (level <= 0) return baseType

        var result = baseType
        repeat(level) {
            result = OptionType(result)
        }
        return result
    }

    /**
     * 检查是否为标准库Option类型
     */
    fun isCoreOptionType(type: CangJieType): Boolean {
        // 检查是否为标准库中的Option枚举
        return type.constructor.declarationDescriptor?.let { decl ->
            decl.name == OPTION &&
                    decl.containingDeclaration.name == CORE_PACKAGE_NAME
        } ?: false
    }

    /**
     * Option类型的子类型关系检查
     */
    fun isOptionSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean {
        return when {
            // 相同类型
            subtype::class == supertype::class -> {
                when (subtype) {
                    is OptionType -> isOptionSubtypeOf(subtype.innerType, (supertype as OptionType).innerType)
                    else -> true
                }
            }

            // 基础类型到Option类型（协变）
            supertype is OptionType -> isOptionSubtypeOf(subtype, supertype.innerType)

            // Option类型到基础类型（逆变）
            subtype is OptionType -> isOptionSubtypeOf(subtype.innerType, supertype)

            else -> false
        }
    }

    /**
     * Option类型的相等性检查
     */
    fun areOptionTypesEqual(type1: CangJieType, type2: CangJieType): Boolean {
        if (type1.isOption != type2.isOption) return false

        if (type1.isOption) {
            // 都是Option类型，比较内部类型
            return areOptionTypesEqual(type1.unwrapOption(), type2.unwrapOption())
        }

        // 都不是Option类型，比较基础类型
        return type1.constructor == type2.constructor
    }

    /**
     * 获取Option类型的内部类型
     */
    fun getOptionInnerType(type: CangJieType): CangJieType? {
        return if (type.isOption) {
            type.unwrapOption()
        } else {
            null
        }
    }

    /**
     * 创建Option类型的字符串表示
     */
    fun optionTypeToString(type: CangJieType): String {
        return when (type) {
            is OptionType -> {
                "Option<${optionTypeToString(type.innerType)}>"
            }

            else -> type.toString()
        }
    }

    /**
     * 语法糖字符串表示
     */
    fun optionTypeToSugarString(type: CangJieType): String {
        return when (type) {
            is OptionType -> {
                "${optionTypeToSugarString(type.innerType)}?"
            }

            else -> type.toString()
        }
    }
} 
