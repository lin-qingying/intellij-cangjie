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

package cn.cangnova.cangjie.ide.cache

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager


fun <T> Module.cacheByClassInvalidatingOnRootModifications(classForKey: Class<*>, provider: () -> T): T {
    return cacheByClass(classForKey, ProjectRootModificationTracker.getInstance(project), provider = provider)
}

fun <T> Module.cacheByClass(classForKey: Class<*>, vararg dependencies: Any, provider: () -> T): T {
    return CachedValuesManager.getManager(project).cache(this, dependencies, classForKey, provider)
}

private fun <T> CachedValuesManager.cache(
    holder: UserDataHolder,
    dependencies: Array<out Any>,
    classForKey: Class<*>,
    provider: () -> T
): T {
    return getCachedValue(
        holder,
        getKeyForClass(classForKey),
        { CachedValueProvider.Result.create(provider(), *dependencies) },
        false
    )
}