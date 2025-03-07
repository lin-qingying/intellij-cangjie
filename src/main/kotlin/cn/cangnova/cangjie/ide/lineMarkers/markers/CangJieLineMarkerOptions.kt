/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.lineMarkers.markers

import cn.cangnova.cangjie.CangJieBundle
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
