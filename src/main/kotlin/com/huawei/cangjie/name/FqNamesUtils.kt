package com.huawei.cangjie.name

fun FqName.parentOrNull(): FqName? = if (this.isRoot) null else parent()
