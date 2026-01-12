/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.types.TypeCheckerState


/**
 * 创建经典类型检查器状态
 *
 * 仓颉语言简化版：由于类型系统简化，许多参数使用固定值
 *
 * @param isErrorTypeEqualsToAnything 错误类型是否等于任何类型
 * @param isStubTypeEqualsToAnything Stub 类型是否等于任何类型
 * @param typeSystemContext 类型系统上下文
 * @param cangjieTypePreparator 类型准备器
 * @param cangjieTypeRefiner 类型精化器
 * @return 类型检查器状态
 */
fun createClassicTypeCheckerState(
    isErrorTypeEqualsToAnything: Boolean = false,
    isStubTypeEqualsToAnything: Boolean = true,
    typeSystemContext: ClassicTypeSystemContext = SimpleClassicTypeSystemContext,
    cangjieTypePreparator: CangJieTypePreparator = CangJieTypePreparator.Default,
    cangjieTypeRefiner: CangJieTypeRefiner = CangJieTypeRefiner.Default
): TypeCheckerState {
    // 仓颉语言的 TypeCheckerState 构造
    // isDnnTypesEqualToFlexible: 仓颉没有 DNN 类型，设为 false
    // allowedTypeVariable: 在经典类型检查中不允许类型变量，设为 false
    return TypeCheckerState(
        isErrorTypeEqualsToAnything = isErrorTypeEqualsToAnything,
        isStubTypeEqualsToAnything = isStubTypeEqualsToAnything,
        isDnnTypesEqualToFlexible = false,
        allowedTypeVariable = false,
        typeSystemContext = typeSystemContext,
        cangjieTypePreparator = cangjieTypePreparator,
        cangjieTypeRefiner = cangjieTypeRefiner
    )
}
