/*
<html>None of the following candidates is applicable:<br/>constructor(captureStatus: CaptureStatus, constructor: NewCapturedTypeConstructor, lowerType: UnwrappedType?, attributes: TypeAttributes = ..., isOption: Boolean = ..., isProjectionNotNull: Boolean = ...): NewCapturedType<br/>constructor(captureStatus: CaptureStatus, lowerType: UnwrappedType?, projection: TypeProjection, typeParameter: TypeParameterDescriptor): NewCapturedType * 仓颉类型系统特殊类型定义
 *
 * 本文件定义了仓颉语言类型系统中的特殊类型，包括：
 * - 类型缩写（AbbreviatedType）
 * - 明确非Option类型（DefinitelyNonOptionType）
 * - 各种类型包装与委托类型
 *
 * 主要用于类型推断、类型检查、类型属性替换、Option判定等场景。
 *
 * 仓颉语言无null/nullable概念，所有“可选”相关逻辑均用Option表达。
 */

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.checker.NewCapturedType
import cn.cangnova.cangjie.types.checker.NewTypeVariableConstructor
import cn.cangnova.cangjie.types.checker.OptionChecker
import cn.cangnova.cangjie.types.model.DefinitelyNonOptionTypeMarker

/**
 * 类型缩写（如类型别名）
 * 用于表示类型的缩写形式和展开形式
 * 例如：类型别名、简化类型显示等
 */
fun SimpleType.withAbbreviation(abbreviatedType: SimpleType): SimpleType {
    if (isError) return this
    return AbbreviatedType(this, abbreviatedType)
}

/**
 * 判断类型是否为明确非Option类型
 * 例如：类型推断、类型检查时用于判定类型是否绝不为Option
 */
val CangJieType.isDefinitelyNonOptionType: Boolean
    get() = unwrap() is DefinitelyNonOptionType

/**
 * 获取类型的缩写形式
 */
fun CangJieType.getAbbreviation(): SimpleType? = getAbbreviatedType()?.abbreviation
/**
 * 获取类型的缩写类型对象
 */
fun CangJieType.getAbbreviatedType(): AbbreviatedType? = unwrap() as? AbbreviatedType

/**
 * 类型缩写类型
 * @property delegate 展开后的类型
 * @property abbreviation 缩写形式
 */
class AbbreviatedType(override val delegate: SimpleType, val abbreviation: SimpleType) : DelegatingSimpleType() {
    val expandedType: SimpleType get() = delegate

    /**
     * 替换类型属性，返回新的缩写类型
     */
    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        AbbreviatedType(delegate.replaceAttributes(newAttributes), abbreviation)

    /**
     * 按指定Option状态转换类型
     */
    override fun makeOptionAsSpecified(isOption: Boolean) =
        AbbreviatedType(
            delegate.makeOptionAsSpecified(isOption),
            abbreviation.makeOptionAsSpecified(isOption)
        )
    /**
     * 替换委托类型
     */
    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = AbbreviatedType(delegate, abbreviation)
}

/**
 * 委托简单类型基类
 * 用于实现类型包装、属性委托等
 */
abstract class DelegatingSimpleType : SimpleType() {
    protected abstract val delegate: SimpleType
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isOption: Boolean get() = delegate.isOption
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

    /**
     * 替换委托类型
     */
    @TypeRefinement
    abstract fun replaceDelegate(delegate: SimpleType): DelegatingSimpleType

    /**
     * 类型精化，返回新的委托类型
     */
    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType =
        replaceDelegate(cangjieTypeRefiner.refineType(delegate) as SimpleType)
}

/**
 * 类型包装基类
 * 用于实现类型的延迟计算、属性委托等
 */
abstract class WrappedType : CangJieType() {
    open fun isComputed(): Boolean = true
    protected abstract val delegate: CangJieType
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isOption: Boolean get() = delegate.isOption
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

    /**
     * 解包类型，返回最内层未包装类型
     */
    final override fun unwrap(): UnwrappedType {
        var result = delegate
        while (result is WrappedType) {
            result = result.delegate
        }
        return result as UnwrappedType
    }

    override fun toString(): String {
        return if (isComputed()) {
            delegate.toString()
        } else {
            "<Not computed yet>"
        }
    }
}

/**
 * 延迟类型包装
 * 用于实现类型的惰性计算
 */
class LazyWrappedType(
    private val storageManager: StorageManager,
    private val computation: () -> CangJieType
) : WrappedType() {
    private val lazyValue = storageManager.createLazyValue(computation)
    override val delegate: CangJieType get() = lazyValue()
    override fun isComputed(): Boolean = lazyValue.isComputed()
    /**
     * 类型精化，返回新的延迟包装类型
     */
    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = LazyWrappedType(storageManager) {
        cangjieTypeRefiner.refineType(computation())
    }
}

/**
 * 明确非Option类型
 * 用于标记类型系统中明确不是Option的类型
 * 主要用于类型推断、类型检查、类型属性替换等场景
 */
class DefinitelyNonOptionType private constructor(
    val original: SimpleType,
    private val useCorrectedOptionForTypeParameters: Boolean
) : DelegatingSimpleType(), CustomTypeParameter,
    DefinitelyNonOptionTypeMarker {

    companion object {
        /**
         * 创建明确非Option类型
         * @param type 原始类型
         * @param useCorrectedOptionForTypeParameters 是否修正类型参数的Option状态
         * @param avoidCheckingActualTypeOption 是否跳过实际Option检查
         */
        @JvmOverloads
        fun makeDefinitelyNonOption(
            type: UnwrappedType,
            useCorrectedOptionForTypeParameters: Boolean = false,
            avoidCheckingActualTypeOption: Boolean = false,
        ): DefinitelyNonOptionType? {
            return when {
                type is DefinitelyNonOptionType -> type
                avoidCheckingActualTypeOption || makesSenseToBeDefinitelyNonOption(
                    type,
                    useCorrectedOptionForTypeParameters
                ) -> {
                    if (type is FlexibleType) {
                        assert(type.lowerBound.constructor == type.upperBound.constructor) {
                            "DefinitelyNonOptionType 只能用于上下界构造器一致的灵活类型"
                        }
                    }
                    DefinitelyNonOptionType(
                        type.lowerIfFlexible().makeOptionAsSpecified(false),
                        useCorrectedOptionForTypeParameters
                    )
                }
                else -> null
            }
        }
        /**
         * 判断类型是否适合被标记为明确非Option
         */
        private fun makesSenseToBeDefinitelyNonOption(
            type: UnwrappedType,
            useCorrectedOptionForFlexibleTypeParameters: Boolean
        ): Boolean {
            if (!type.canHaveUndefinedOption()) return false
            if (type is StubTypeForBuilderInference) return TypeUtils.isOptionType(type)
            if ((type.constructor.declarationDescriptor as? TypeParameterDescriptorImpl)?.isInitialized == false) {
                return true
            }
            if (useCorrectedOptionForFlexibleTypeParameters && type.constructor.declarationDescriptor is TypeParameterDescriptor) {
                return TypeUtils.isOptionType(type)
            }
            return !OptionChecker.isSubtypeOfAny(type)
        }
        /**
         * 判断类型是否可能为未确定Option状态
         */
        private fun UnwrappedType.canHaveUndefinedOption(): Boolean =
            constructor is NewTypeVariableConstructor
                    || constructor.declarationDescriptor is TypeParameterDescriptor
                    || this is NewCapturedType
                    || this is StubTypeForBuilderInference
    }
    override val delegate: SimpleType
        get() = original
    override val isOption: Boolean
        get() = false
    override val isTypeParameter: Boolean
        get() = delegate.constructor is NewTypeVariableConstructor ||
                delegate.constructor.declarationDescriptor is TypeParameterDescriptor
    /**
     * 类型替换，返回新的明确非Option类型
     */
    override fun substitutionResult(replacement: CangJieType): CangJieType =
        replacement.unwrap().makeDefinitelyNonOptionOrNonOption(useCorrectedOptionForTypeParameters)
    /**
     * 替换类型属性，返回新的明确非Option类型
     */
    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        DefinitelyNonOptionType(delegate.replaceAttributes(newAttributes), useCorrectedOptionForTypeParameters)
    /**
     * 按指定Option状态转换类型
     */
    override fun makeOptionAsSpecified(isOption: Boolean): SimpleType =
        if (isOption) delegate.makeOptionAsSpecified(isOption) else this
    override fun toString(): String = "$delegate & Any"
    /**
     * 替换委托类型
     */
    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) =
        DefinitelyNonOptionType(delegate, useCorrectedOptionForTypeParameters)
}

/**
 * 将类型转换为明确非Option类型
 */
fun SimpleType.makeSimpleTypeDefinitelyNonOptionOrNonOption(useCorrectedOptionForTypeParameters: Boolean = false): SimpleType =
    DefinitelyNonOptionType.makeDefinitelyNonOption(this, useCorrectedOptionForTypeParameters)
        ?: makeIntersectionTypeDefinitelyNonOptionOrNonOption()
        ?: makeOptionAsSpecified(false)


/**
 * 将类型转换为明确非Option类型（适用于未包装类型）
 */
fun UnwrappedType.makeDefinitelyNonOptionOrNonOption(useCorrectedOptionForTypeParameters: Boolean = false): UnwrappedType =
    DefinitelyNonOptionType.makeDefinitelyNonOption(this, useCorrectedOptionForTypeParameters)
        ?: makeIntersectionTypeDefinitelyNonOptionOrNonOption()
        ?: makeOptionAsSpecified(false)

/**
 * 交集类型转换为明确非Option类型
 */
private fun IntersectionTypeConstructor.makeDefinitelyNonOptionOrNonOption(): IntersectionTypeConstructor? {
    return transformComponents({ TypeUtils.isOptionType(it) }, { it.unwrap().makeDefinitelyNonOptionOrNonOption() })
}

/**
 * 将类型转换为交集类型的明确非Option类型
 */
private fun CangJieType.makeIntersectionTypeDefinitelyNonOptionOrNonOption(): SimpleType? {
    val typeConstructor = constructor as? IntersectionTypeConstructor ?: return null
    val definitelyNonOptionConstructor = typeConstructor.makeDefinitelyNonOptionOrNonOption() ?: return null
    return definitelyNonOptionConstructor.createType()
}

