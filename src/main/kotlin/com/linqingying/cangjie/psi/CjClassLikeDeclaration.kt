package com.linqingying.cangjie.psi

import com.linqingying.cangjie.name.ClassId


interface CjClassLikeDeclaration : CjNamedDeclaration {

    fun getClassId(): ClassId?
}
