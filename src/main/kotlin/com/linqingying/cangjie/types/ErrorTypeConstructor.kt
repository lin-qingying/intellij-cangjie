package com.linqingying.cangjie.types

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.storage.LockBasedStorageManager
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.error.ErrorEntity
import com.linqingying.cangjie.types.error.ErrorTypeKind
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
 }


object DefaultBuiltIns : CangJieBuiltIns(null, LockBasedStorageManager("DefaultBuiltIns"))
