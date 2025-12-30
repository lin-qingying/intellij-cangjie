/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.formatter

import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.utils.ReflectionUtil
import com.intellij.openapi.editor.SoftWrap
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import java.lang.reflect.InvocationTargetException

class CangJieCommonCodeStyleSettings(val isTempForDeserialize: Boolean = false) :
    CommonCodeStyleSettings(CangJieLanguage) {

    @ReflectionUtil.SkipInEquals
    var CODE_STYLE_DEFAULTS: String? = null


    override fun readExternal(element: Element?) {
        if (isTempForDeserialize) {
            super.readExternal(element)
            return
        }
        val tempDeserialize = createForTempDeserialize()
        tempDeserialize.readExternal(element)
        applyCangJieCodeStyle(tempDeserialize.CODE_STYLE_DEFAULTS, this, true)
        super.readExternal(element)
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is CangJieCommonCodeStyleSettings) {
            return false
        }

        if (!ReflectionUtil.comparePublicNonFinalFieldsWithSkip(this, obj)) {
            return false
        }

        if (softMargins != obj.softMargins) {
            return false
        }

        val options = indentOptions
        if ((options == null && obj.indentOptions != null) ||
            (options != null && options != obj.indentOptions)
        ) {
            return false
        }

        return arrangementSettingsEqual(obj)
    }

    private fun serializeInto(softWraps: List<SoftWrap>, element: Element) {
        if (!softMargins.isEmpty()) {
            XmlSerializer.serializeInto(this, element)
        }
    }

    override fun clone(rootSettings: CodeStyleSettings): CommonCodeStyleSettings {

        val commonSettings = CangJieCommonCodeStyleSettings()
        copyPublicFields(this, commonSettings)
        // 反射设置根设置
        try {
            CommonCodeStyleSettings::class.java.getDeclaredMethod("setRootSettings", CodeStyleSettings::class.java).apply {
                isAccessible = true
            }.invoke(commonSettings, rootSettings)

        } catch (e: NoSuchMethodException) {
            throw IllegalStateException(e)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e)
        } catch (e: InvocationTargetException) {
            throw IllegalStateException(e)
        }

        // 设置强制排列菜单

        commonSettings.isForceArrangeMenuAvailable = this.isForceArrangeMenuAvailable

        //  处理缩进选项（使用安全调用操作符）
        indentOptions?.let { sourceIndent ->
            commonSettings.initIndentOptions().apply {
                copyFrom(sourceIndent)
            }
        }
        //  处理排列设置（使用安全调用和对象克隆）
        arrangementSettings?.let { it1 ->
            commonSettings.setArrangementSettings(it1.clone())


        }

        //   使用更简洁的反射查找方法
        try {
            CommonCodeStyleSettings::class.java.declaredMethods
                .find { it.name == "setSoftMargins" }
                ?.apply { isAccessible = true }
                ?.invoke(this, softMargins)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e)
        } catch (e: InvocationTargetException) {
            throw IllegalStateException(e)
        }
        return commonSettings

    }

    companion object {
        private const val INDENT_OPTIONS_TAG = "indentOptions"
        private const val ARRANGEMENT_ELEMENT_NAME = "arrangement"

        private fun createForTempDeserialize(): CangJieCommonCodeStyleSettings {
            return CangJieCommonCodeStyleSettings(true)
        }
    }


    override fun hashCode(): Int {
        var result = isTempForDeserialize.hashCode()
        result = 31 * result + (CODE_STYLE_DEFAULTS?.hashCode() ?: 0)
        return result
    }
}