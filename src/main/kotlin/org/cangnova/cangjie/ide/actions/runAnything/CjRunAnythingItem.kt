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

package org.cangnova.cangjie.ide.actions.runAnything

import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.execution.ParametersListUtil
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.Icon
import javax.swing.JPanel

abstract class CjRunAnythingItem(command: String, icon: Icon) : RunAnythingItemBase(command, icon){

    abstract val helpCommand: String

    abstract val commandDescriptions: Map<String, String>

    abstract fun getOptionsDescriptionsForCommand(commandName: String): Map<String, String>?

    override fun createComponent(pattern: String?, isSelected: Boolean, hasFocus: Boolean): Component =
        super.createComponent(pattern, isSelected, hasFocus).also(this::customizeComponent)

    private fun customizeComponent(component: Component) {
        if (component !is JPanel) return

        val params = ParametersListUtil.parse(StringUtil.trimStart(command, helpCommand))
        @Suppress("HardCodedStringLiteral") val description = when (params.size) {
            0 -> null
            1 -> commandDescriptions[params.last()]
            else -> {
                val optionsDescriptions = getOptionsDescriptionsForCommand(params.first())
                optionsDescriptions?.get(params.last())
            }
        } ?: return
        val descriptionComponent = SimpleColoredComponent()
        descriptionComponent.append(
            StringUtil.shortenTextWithEllipsis(" $description.", 200, 0),
            SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
        )
        component.add(descriptionComponent, BorderLayout.EAST)
    }
}
