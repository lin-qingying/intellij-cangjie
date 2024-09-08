package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.storage.LockBasedStorageManager
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.error.ErrorEntity
import com.huawei.cangjie.types.error.ErrorTypeKind
import kotlin.jvm.internal.Intrinsics


class ErrorTypeConstructor(val kind: ErrorTypeKind, vararg val formatParams: String) : TypeConstructor {
    private val debugText = ErrorEntity.ERROR_TYPE.debugText.format(kind.debugMessage.format(*formatParams))

    fun getParam(i: Int): String = formatParams[i]


    override fun getSupertypes(): Collection<CangJieType> = emptyList()
    override fun isFinal(): Boolean = false
    override fun isDenotable(): Boolean = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor = ErrorUtils.errorClass

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
        Intrinsics.checkNotNullParameter(cangjieTypeRefiner, "cangjieTypeRefiner")
        return this
    }

    override fun getParameters(): MutableList<TypeParameterDescriptor> = mutableListOf()

    //    override fun getBuiltIns(): CangJieBuiltIns = DefaultBuiltIns.Instance
    override fun toString(): String = debugText
    override fun getBuiltIns(): CangJieBuiltIns {
        return DefaultBuiltIns

    }
//    @TypeRefinement
//    override fun refine(kotlinTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
}


object DefaultBuiltIns : CangJieBuiltIns(null, LockBasedStorageManager("DefaultBuiltIns"))
