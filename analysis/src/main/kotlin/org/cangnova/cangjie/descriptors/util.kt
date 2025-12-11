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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.psi.CjImportDirectiveItem
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.doNotAnalyze
import org.cangnova.cangjie.psi.psiUtil.getQualifiedElementSelector
import org.cangnova.cangjie.references.mainReference
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.caches.getResolutionFacade

fun CjImportDirectiveItem.getPackageDatas(resolutionFacade: ResolutionFacade = this.getResolutionFacade()): List<PackageData> {
    val targets = targetDescriptors(resolutionFacade)


    return targets.mapNotNull {
        var _target: DeclarationDescriptor? = it
        while (_target !is PackageData && _target != null) {
            _target = _target.containingDeclaration
        }
        _target as? PackageData
    }


}

fun CjImportDirectiveItem.targetPackageView(resolutionFacade: ResolutionFacade = this.getResolutionFacade()): PackageViewDescriptor? {
    return when (val packageData = getPackageDatas(resolutionFacade).firstOrNull()) {
        is PackageViewDescriptor -> packageData
        is PackageFragmentDescriptor -> packageData.getPackageViewDescriptor()
        else -> null
    }

}

/**
 * 获取导入指令的目标描述符集合
 *
 * 此函数旨在解析导入指令所指向的描述符集合，以支持代码分析和理解
 * 它通过解析导入指令中的引用表达式来实现，如果当前文件不应进行分析或导入指令不包含有效引用，则返回空集合
 *
 * @param resolutionFacade 分析表达式所用的解析工具，如果未显式提供，则使用当前指令的解析工具
 * @return 解析得到的描述符集合，如果解析失败或不应分析，则返回空集合
 */
fun CjImportDirectiveItem.targetDescriptors(resolutionFacade: ResolutionFacade = this.getResolutionFacade()): Collection<DeclarationDescriptor> {
    // For codeFragments imports are created in dummy file
    // 如果当前文件不应进行分析（例如，代码片段），直接返回空集合，避免不必要的处理
    if (this.getContainingCjFile().doNotAnalyze != null) return emptyList()

    // 尝试获取导入指令中的选择器表达式，如果获取失败或表达式类型不匹配，则返回空集合
    // 这一步是为了确保我们能够正确解析导入指令的目标
    val nameExpression =
        importedReference?.getQualifiedElementSelector() as? CjSimpleNameExpression ?: return emptyList()

    // 解析表达式，获取其对应的描述符集合
    // 这是核心逻辑，通过解析表达式来确定导入指令实际指向的声明描述符
    return nameExpression.mainReference.resolveToDescriptors(resolutionFacade.analyze(nameExpression))
}
