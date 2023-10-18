package com.huawei.cangjie1.psi

import com.huawei.cangjie1.name.ClassId


interface CjClassLikeDeclaration : CjNamedDeclaration {

    fun getClassId(): ClassId?
}
