package com.huawei.cangjie.diagnostics

import com.intellij.psi.PsiElement


interface DiagnosticMarker {
    val psiElement: PsiElement
    val factoryName: String
}

interface DiagnosticWithParameters1Marker<A> : DiagnosticMarker {
    val a: A
}

interface DiagnosticWithParameters2Marker<A, B> : DiagnosticMarker {
    val a: A
    val b: B
}

interface DiagnosticWithParameters3Marker<A, B, C> : DiagnosticMarker {
    val a: A
    val b: B
    val c: C
}

interface DiagnosticWithParameters4Marker<A, B, C, D> : DiagnosticMarker {
    val a: A
    val b: B
    val c: C
    val d: D
}
