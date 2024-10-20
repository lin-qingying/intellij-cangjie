package com.huawei.cangjie.psi

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe

object CjNamedDeclarationUtil {
    fun getUnsafeFQName(namedDeclaration: CjNamedDeclaration): FqNameUnsafe? {
        val fqName = namedDeclaration.fqName
        return fqName?.toUnsafe()
    }

    fun getFQName(namedDeclaration: CjNamedDeclaration): FqName? {
        val name = namedDeclaration.nameAsName ?: return null

        val parentFqName = getParentFqName(namedDeclaration) ?: return null

        return parentFqName.child(name)
    }

    fun getParentFqName(namedDeclaration: CjNamedDeclaration): FqName? {
        var parent = namedDeclaration.parent
        if (parent is CjAbstractClassBody) {
            parent = parent.getParent()
        }

        if (parent is CjFile) {
            return parent.packageFqName
        } else if (namedDeclaration is CjParameter) {
            val constructorClass = CjPsiUtil.getClassIfParameterIsProperty(namedDeclaration)
            if (constructorClass != null) {
                return getFQName(constructorClass)
            }
        }else if (parent is CjExtend){
            return getParentFqName(parent)
        }



        return null
    }
}
