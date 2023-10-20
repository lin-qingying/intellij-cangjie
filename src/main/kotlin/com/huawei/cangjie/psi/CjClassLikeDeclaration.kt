package com.huawei.cangjie.psi

import com.huawei.cangjie.name.ClassId


interface CjClassLikeDeclaration : CjNamedDeclaration {

    fun getClassId(): ClassId?
}
