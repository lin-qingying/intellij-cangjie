package com.huawei.cangjie.lang.core.psi.ext

import com.huawei.cangjie.lang.core.psi.CjLifetimeParameter

interface CjGenericDeclaration : CjElement {
    //约束
//    val typeParameterList: CjTypeParameterList?
//    where语句
//    val whereClause: CjWhereClause?
}
//
//val CjGenericDeclaration.lifetimeParameters: List<CangJieLifetimeParameter>
//    get() = typeParameterList?.lifetimeParameterList.orEmpty()
