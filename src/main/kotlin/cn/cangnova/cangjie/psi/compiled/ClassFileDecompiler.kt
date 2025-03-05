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

package cn.cangnova.cangjie.psi.compiled

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.project.DefaultProjectFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class ClassFileDecompiler : BinaryFileDecompiler {
    override fun decompile(file: VirtualFile): CharSequence {
        val decompiler: ClassFileDecompilers.Decompiler = ClassFileDecompilers.instance.find(
            file,
            ClassFileDecompilers.Decompiler::class.java,
        )
//        if (decompiler is ClsDecompilerImpl) {
//            return ClsFileImpl.decompile(file)
//        }

        if (decompiler is ClassFileDecompilers.Full) {
            val manager = PsiManager.getInstance(DefaultProjectFactory.getInstance().defaultProject)
            return decompiler.createFileViewProvider(file, manager, true).contents
        }

//        if (decompiler is ClassFileDecompilers.Light) {
//            try {
//                return decompiler.getText(file)
//            } catch (e: ClassFileDecompilers.Light.CannotDecompileException) {
//                LOG.warn("decompiler: " + decompiler::class, e)
//                return ClsFileImpl.decompile(file)
//            }
//        }

        throw IllegalStateException(
            decompiler::class.java.name +
                " should be on of " +
                ClassFileDecompilers.Full::class.java.name +
                " or " +
                ClassFileDecompilers.Light::class.java.name,
        )
    }

    companion object {
        private val LOG = Logger.getInstance(
            ClassFileDecompiler::class.java,
        )
    }
}
