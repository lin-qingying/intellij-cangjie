package com.linqingying.cangjie.psi

import com.linqingying.cangjie.test.CangJieTestWithEnvironment


class CjPsiFactoryTest : CangJieTestWithEnvironment() {

    val factory = CjPsiFactory(project)

    fun testCreateVariable() {

factory.createVariable("let a = 1")
    }

    fun testCreateProperty() {
    }
}
