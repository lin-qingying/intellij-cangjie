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

package cn.cangnova.cangjie.analyzer

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.metadata.decompiler.CangJieMetadataFinder

interface MetadataFinderFactory {
    fun create(scope: GlobalSearchScope): CangJieMetadataFinder
    fun create(project: Project, module: ModuleDescriptor): CangJieMetadataFinder
}
//interface VirtualFileFinderFactory : MetadataFinderFactory {
//    override fun create(scope: GlobalSearchScope): VirtualFileFinder
//    override fun create(project: Project, module: ModuleDescriptor): VirtualFileFinder
//
//    companion object SERVICE {
//        fun getInstance(project: Project): VirtualFileFinderFactory =
//            project.getService(VirtualFileFinderFactory::class.java)
//    }
//}
