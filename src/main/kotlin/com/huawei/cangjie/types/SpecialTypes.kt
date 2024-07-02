package com.huawei.cangjie.types

import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

fun SimpleType.withAbbreviation(abbreviatedType: SimpleType): SimpleType {
    if (isError) return this
    return AbbreviatedType(this, abbreviatedType)
}

class AbbreviatedType(override val delegate: SimpleType, val abbreviation: SimpleType) : DelegatingSimpleType() {
    val expandedType: SimpleType get() = delegate

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        AbbreviatedType(delegate.replaceAttributes(newAttributes), abbreviation)

    override fun makeNullableAsSpecified(newNullability: Boolean) =
        AbbreviatedType(delegate.makeNullableAsSpecified(newNullability), abbreviation.makeNullableAsSpecified(newNullability))

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = AbbreviatedType(delegate, abbreviation)

//    @TypeRefinement
//    @OptIn(TypeRefinement::class)
//    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): AbbreviatedType =
//        AbbreviatedType(
//            cangjieTypeRefiner.refineType(delegate) as SimpleType,
//            cangjieTypeRefiner.refineType(abbreviation) as SimpleType
//        )
}

abstract class DelegatingSimpleType : SimpleType() {
    protected abstract val delegate: SimpleType

    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

    @TypeRefinement
    abstract fun replaceDelegate(delegate: SimpleType): DelegatingSimpleType

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType =
        replaceDelegate(cangjieTypeRefiner.refineType(delegate) as SimpleType)
}

abstract class WrappedType : CangJieType() {
    open fun isComputed(): Boolean = true
    protected abstract val delegate: CangJieType

    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedNullable: Boolean get() = delegate.isMarkedNullable
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

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
class LazyWrappedType(
    private val storageManager: StorageManager,
    private val computation: () -> CangJieType
) : WrappedType() {
    private val lazyValue = storageManager.createLazyValue(computation)

    override val delegate: CangJieType get() = lazyValue()

    override fun isComputed(): Boolean = lazyValue.isComputed()
//
    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = LazyWrappedType(storageManager) {
        cangjieTypeRefiner.refineType(computation())
    }
}
