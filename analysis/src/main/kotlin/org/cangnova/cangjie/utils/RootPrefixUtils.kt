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

package org.cangnova.cangjie.utils

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.parentOrNull
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjImportDirectiveItem
import org.cangnova.cangjie.psi.CjPackageDirective
import org.cangnova.cangjie.psi.psiUtil.getParentOfTypes2
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolver

// 判断FqName是否可以添加根前缀
fun FqName.canAddRootPrefix(): Boolean {
    // 如果FqName的字符串表示不以QualifiedExpressionResolver.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT开头，并且FqName的父级不是根级
    return !asString().startsWith(QualifiedExpressionResolver.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT)
            && parentOrNull()?.isRoot == false
}
// 判断当前元素是否可以添加根前缀
fun CjElement.canAddRootPrefix(): Boolean {
    // 获取当前元素的父元素，如果父元素是CjImportDirectiveItem或CjPackageDirective类型，则返回false，否则返回true
    return getParentOfTypes2<CjImportDirectiveItem, CjPackageDirective>() == null
}

// 如果需要添加根前缀，则返回带有根前缀的FqName
fun FqName.withRootPrefixIfNeeded(targetElement: CjElement? = null): FqName {
    // 如果当前FqName可以添加根前缀，并且目标元素也可以添加根前缀
    if (canAddRootPrefix() && targetElement?.canAddRootPrefix() != false) {
        // 返回带有根前缀的FqName
        return FqName(QualifiedExpressionResolver.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT + asString())
    }

    // 否则返回当前FqName
    return this
}