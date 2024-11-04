package com.linqingying.cangjie.ide.lineMarkers.markers

import com.linqingying.cangjie.CangJieBundle
import com.intellij.codeInsight.daemon.GutterIconDescriptor
import com.intellij.icons.AllIcons



object CangJieLineMarkerOptions {
    val overriddenOption = GutterIconDescriptor.Option(
        "cangjie.overridden",
        CangJieBundle.message("highlighter.name.overridden.declaration"), AllIcons.Gutter.OverridenMethod
    )

    val implementedOption = GutterIconDescriptor.Option(
        "cangjie.implemented",
        CangJieBundle.message("highlighter.name.implemented.declaration"), AllIcons.Gutter.ImplementedMethod
    )

    val overridingOption = GutterIconDescriptor.Option(
        "cangjie.overriding",
        CangJieBundle.message("highlighter.name.overriding.declaration"), AllIcons.Gutter.OverridingMethod
    )

    val implementingOption =
        GutterIconDescriptor.Option(
            "cangjie.implementing",
            CangJieBundle.message("highlighter.name.implementing.declaration"),
            AllIcons.Gutter.ImplementingMethod
        )


//    val dslOption =
//        GutterIconDescriptor.Option("cangjie.dsl", CangJieBundle.message("highlighter.name.dsl.markers"), CangJieIcons.DSL_MARKER_ANNOTATION)

    val recursiveOption =
        GutterIconDescriptor.Option("cangjie.recursive",
            CangJieBundle.message("highlighter.tool.tip.text.recursive.call"),
            AllIcons.Gutter.RecursiveMethod)

    val options = arrayOf(
        overriddenOption, implementedOption,
        overridingOption, implementingOption,
//        actualOption, expectOption,    dslOption,
        recursiveOption
    )
}
