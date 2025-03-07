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

package cn.cangnova.cangjie.ide.quickfix

import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.UserDataProperty
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly

abstract class CangJieImportQuickFixAction<out T : CjElement>(element: T): CangJieQuickFixAction<T>(element) {
    /**
     * @return import action if quick fix element is still valid, null, otherwise.
     * Note that provided action might require addition actions by the user (e.g. when there are multiple available imports to choose from).
     */
    abstract fun createImportAction(
        editor: Editor,
        file: CjFile,
    ): QuestionAction?

    /**
     * @return import action if quick fix can be applied without any additional actions by the user, null, otherwise.
     */
    abstract fun createAutoImportAction(
        editor: Editor,
        file: CjFile,
        filterSuggestions: (Collection<FqName>) -> Collection<FqName> = { suggestions -> suggestions },
    ): QuestionAction?
}
object CangJieAddImportActionInfo {
    interface ExecuteListener {
        fun onExecute(variants: List<AutoImportVariant>)
    }

    var PsiFile.executeListener: ExecuteListener? by UserDataProperty(Key("CANGJIE_IMPORT_EXECUTE_LISTENER"))

    @TestOnly
    fun setExecuteListener(file: PsiFile, disposable: Disposable, listener: ExecuteListener) {
        assert(file.executeListener == null)
        file.executeListener = listener
        Disposer.register(disposable) { file.executeListener = null }
    }
}
