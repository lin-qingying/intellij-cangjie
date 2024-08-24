package com.huawei.cangjie.types.error

import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner


class ErrorType @JvmOverloads internal constructor(
    override val constructor: TypeConstructor,
    override val memberScope: MemberScope,
    val kind: ErrorTypeKind,
    override val arguments: List<TypeProjection> = emptyList(),
    override val isMarkedOption: Boolean = false,
    vararg val formatParams: String
) : SimpleType() {
    val debugMessage = String.format(kind.debugMessage, *formatParams)


    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType = this

//    fun replaceArguments(newArguments: List<TypeProjection>): ErrorType =
//        ErrorType(constructor, memberScope, kind, newArguments, isMarkedOption, *formatParams)

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType =
        ErrorType(constructor, memberScope, kind, arguments, newNullability, *formatParams)

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: CangJieTypeRefiner) = this
}
