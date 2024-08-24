package com.huawei.cangjie.types

import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.NewTypeVariableConstructor
import com.huawei.cangjie.types.error.ErrorScopeKind
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.model.StubTypeMarker


abstract class AbstractStubType(
    val originalTypeVariable: NewTypeVariableConstructor,
    override val isMarkedOption: Boolean
) : SimpleType() {
    override val memberScope: MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.STUB_TYPE_SCOPE, originalTypeVariable.toString())

    override val arguments: List<TypeProjection>
        get() = emptyList()

    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType = this

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        return if (newNullability == isMarkedOption) this else materialize(newNullability)
    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = this

    abstract fun materialize(newNullability: Boolean): AbstractStubType

    companion object {
        fun createConstructor(originalTypeVariable: NewTypeVariableConstructor) =
            ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.STUB_TYPE, originalTypeVariable.toString())
    }
}

class StubTypeForTypeVariablesInSubtyping(
    originalTypeVariable: NewTypeVariableConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType =
        StubTypeForTypeVariablesInSubtyping(originalTypeVariable, newNullability, constructor)

    override fun toString(): String {
        return "Stub (subtyping): $originalTypeVariable${if (isMarkedOption) "?" else ""}"
    }
}


class StubTypeForBuilderInference(
    originalTypeVariable: NewTypeVariableConstructor,
    isMarkedNullable: Boolean,
    override val constructor: TypeConstructor = createConstructor(originalTypeVariable)
) : AbstractStubType(originalTypeVariable, isMarkedNullable), StubTypeMarker {
    override fun materialize(newNullability: Boolean): AbstractStubType =
        StubTypeForBuilderInference(originalTypeVariable, newNullability, constructor)

    override val memberScope: MemberScope = originalTypeVariable.builtIns.anyType.memberScope

    override fun toString(): String {
        // BI means builder inference
        return "Stub (BI): $originalTypeVariable${if (isMarkedOption) "?" else ""}"
    }
}
