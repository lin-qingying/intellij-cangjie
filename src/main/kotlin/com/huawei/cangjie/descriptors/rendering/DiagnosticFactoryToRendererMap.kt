package com.huawei.cangjie.descriptors.rendering

import com.huawei.cangjie.descriptors.DiagnosticFactory
import com.huawei.cangjie.descriptors.DiagnosticFactory1
import com.intellij.psi.PsiElement

//class DiagnosticFactoryToRendererMap(val name:String = "<unnamed>") {
//    val map = mutableMapOf<DiagnosticFactory<*>, DiagnosticRenderer<*>>()
//
//    operator fun get(factory: DiagnosticFactory<*>): DiagnosticRenderer<*>? {
//        return map[factory]
//
//    }
//    fun <E : PsiElement, A> put(
//        factory: DiagnosticFactory1<E, A>,
//        message: String,
//        rendererA:DiagnosticParameterRenderer<in A>?
//    ) {
//        checkMutability()
//        map[factory] = DiagnosticWithParameters1Renderer<A>(message, rendererA)
//    }
//
//
//    fun <E : PsiElement, A> put(
//        factory: DiagnosticFactory1<E, A>,
//        message: String,
//        rendererA: DiagnosticParameterRenderer<A>
//    ) {
//        checkMutability()
//        map[factory] = DiagnosticWithParameters1Renderer<A>(message, rendererA)
//    }
//
//}