package com.huawei.cangjie.psi

import com.huawei.cangjie.test.CangJieTestWithEnvironment


class CjPsiFactoryTest : CangJieTestWithEnvironment() {

    val factory = CjPsiFactory(project)

    fun testCreateVariable() {

factory.createVariable("let a = 1")
    }

    fun testCreateProperty() {
    }
}
