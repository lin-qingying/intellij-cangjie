//package com.huawei.cangjie.descriptors.rendering
//
//import com.huawei.cangjie.descriptors.Errors.UNRESOLVED_REFERENCE
//import com.huawei.cangjie.diagnostics.UnboundDiagnostic
//import com.huawei.cangjie.descriptors.rendering.Renderers.ELEMENT_TEXT
//import com.huawei.cangjie.psi.CjReferenceExpression
//import com.huawei.cangjie.utils.firstNotNullResult
//
//
//object DefaultErrorMessages {
//    private var RENDERER_MAPS: List<DiagnosticFactoryToRendererMap>
//
//    fun render(diagnostic: UnboundDiagnostic): String {
//        val renderer: DiagnosticRenderer<*> = getRendererForDiagnostic(diagnostic)
//        if (renderer != null) {
//            return renderer.render(diagnostic)
//        }
//        return "$diagnostic (error: could not render message)"
//    }
//
//    fun getRendererForDiagnostic(diagnostic: UnboundDiagnostic): DiagnosticRenderer<  *>? {
//        // firstNotNullOfOrNull from stdlib can not be used here because it is InlineOnly function and can not be accessed from Java
//        val renderer =
//            RENDERER_MAPS.firstNotNullResult {
//                it[diagnostic.factory]
//            }
//        return renderer ?: diagnostic.factory.defaultRenderer
//    }
//
//    private val MAP = DiagnosticFactoryToRendererMap("Default")
//
//
//    init {
//        RENDERER_MAPS = listOf(MAP)
//    }
//
//
//    init {
//        MAP.put(
//            UNRESOLVED_REFERENCE,
//            "Unresolved reference: {0}",
//            ELEMENT_TEXT
//        )
//    }
//}
