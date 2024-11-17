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

package com.linqingying.cangjie.psi.compiled

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager


/**
 * An API to extend default IDEA .class file decompiler and handle files compiled from sources other than Java.
 */
@Service
class ClassFileDecompilers private constructor() {
    /**
     * Actual implementations should extend either [Light] or [Full] classes -
     * those that don't are silently ignored.
     */
    interface Decompiler {
        fun accepts(file: VirtualFile): Boolean
    }

    /**
     *
     * "Light" decompilers are intended for augmenting file text constructed by standard IDEA decompiler
     * without changing it's structure - i.e. providing additional information in comments,
     * or replacing standard "compiled code" method body comment with something more meaningful.
     *
     *
     * If a plugin by somewhat reason cannot decompile a file it can throw [Light.CannotDecompileException]
     * and thus make IDEA to fall back to a built-in decompiler implementation.
     *
     *
     * Plugins registering extension of this type normally should accept all files and use `order="last"`
     * attribute to avoid interfering with other decompilers.
     */
    abstract class Light : Decompiler {
        class CannotDecompileException(message: String?, cause: Throwable?) :
            RuntimeException(message, cause)

        @Throws(CannotDecompileException::class)
        abstract fun getText(file: VirtualFile): CharSequence
    }

    /**
     *
     * "Full" decompilers are designed to provide extended support for languages significantly different from Java.
     * Extensions of this type should take care of building file stubs and properly indexing them -
     * in return they have an ability to represent decompiled file in a way natural for original language.
     */
    abstract class Full : Decompiler {
        abstract val stubBuilder: ClsStubBuilder

        /**
         * <h5>Notes for implementers</h5>
         *
         *
         * 1. Return a correct language from [FileViewProvider.getBaseLanguage].
         *
         *
         * 2. This method is called for both PSI file construction and obtaining document text.
         * In the latter case the PsiManager is based on default project, and the only method called
         * on a resulting view provider is [FileViewProvider.getContents].
         *
         *
         * 3. A language compiler may produce auxiliary .class files which should be handled as part of their parent classes.
         * A standard practice is to hide such files by returning `null` from
         * [FileViewProvider.getPsi].
         */
        abstract fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider
    }

    val EP_NAME: ExtensionPointName<Decompiler> = ExtensionPointName("com.linqingying.cangjie.classFileDecompiler")

    init {
        EP_NAME.addChangeListener({ BinaryFileTypeDecompilers.getInstance().notifyDecompilerSetChange() }, null)
    }

    fun <D : Decompiler > find(file: VirtualFile, decompilerClass: Class<D>): D  {
        return EP_NAME.findFirstSafe({ d: Decompiler -> decompilerClass.isInstance(d) && d.accepts(file) }) as D
    }

    companion object {
        val instance: ClassFileDecompilers
            get() = ApplicationManager.getApplication().getService(
                ClassFileDecompilers::class.java
            )
    }
}
