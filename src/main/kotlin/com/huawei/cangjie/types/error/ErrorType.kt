package com.huawei.cangjie.types.error

import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.util.supertypes

//当具有多个超类型时，推导类型失败使用该类
class MultipleSupertypeTypeInferenceFailure(
    private val type: CangJieType

) : ErrorType(type.constructor,type.memberScope, ErrorTypeKind.MULIT_SMALL_COMMON_SUPERTYPES)
{
    val intersectedTypes:List<CangJieType> get() {
        return type.constructor.supertypes.toList()
    }
}
open class ErrorType @JvmOverloads internal constructor(
    override val constructor: TypeConstructor,
    override val memberScope: MemberScope,
    val kind: ErrorTypeKind,
    override val arguments: List<TypeProjection> = emptyList(),
    override val isMarkedOption: Boolean = false,
    private vararg val formatParams: String
) : SimpleType() {
    val debugMessage = String.format(kind.debugMessage, *formatParams)


    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType = this

//    fun replaceArguments(newArguments: List<TypeProjection>): ErrorType =
//        ErrorType(constructor, memberScope, kind, newArguments, isMarkedOption, *formatParams)

    override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType =
        ErrorType(constructor, memberScope, kind, arguments, newNullability, *formatParams)

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = this
}
