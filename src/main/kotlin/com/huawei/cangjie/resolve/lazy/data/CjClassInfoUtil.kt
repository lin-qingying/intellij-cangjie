package com.huawei.cangjie.resolve.lazy.data

import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.psi.*


object CjClassInfoUtil {
    //    @Deprecated(message = "Use createClassOrObjectInfo(KtClassOrObject) instead", level = DeprecationLevel.ERROR)
//    @Deprecated("use {@link #createClassOrObjectInfo(KtClassOrObject)} instead.")
    fun createClassLikeInfo(typeStatement: CjTypeStatement): CjClassLikeInfo {
        return createTypeStatementInfo(typeStatement)
    }

    fun createTypeStatementInfo(typeStatement: CjTypeStatement): CjTypeStatementInfo<out CjTypeStatement> {
        if (typeStatement is CjClass) {
            return CjClassInfo(typeStatement, ClassKind.CLASS)
        } else if (typeStatement is CjEnum) {
            return CjClassInfo(typeStatement, ClassKind.ENUM)
        } else if (typeStatement is CjStruct) {
            return CjClassInfo(typeStatement, ClassKind.STRUCT)
        } else if (typeStatement is CjInterface) {
            return CjClassInfo(typeStatement, ClassKind.INTERFACE)
        }

        throw IllegalArgumentException("Unknown declaration type: " + typeStatement + typeStatement.text)
    }
}
