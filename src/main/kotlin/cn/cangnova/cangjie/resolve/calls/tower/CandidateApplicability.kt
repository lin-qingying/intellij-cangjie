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

package cn.cangnova.cangjie.resolve.calls.tower


enum class CandidateApplicability {
    /**
     * 当解析为带有SAM转换的函数且数组没有作为vararg展开时使用。
     */
    RESOLVED_TO_SAM_WITH_VARARG,

    /**
     * 候选者因SinceCangJie版本较新或Deprecation级别为隐藏而从解析中移除。
     * 注意，SinceCangJie不会过滤出分类符符号和属性访问器。这些应导致API_NOT_AVAILABLE。
     * 引发UNRESOLVED_REFERENCE。
     */
    HIDDEN,

    /**
     * 候选者可能成功，但需要不支持的功能。
     * 对于引用本地变量的情况报告此错误。
     * 引发UNSUPPORTED。
     */
    UNSUPPORTED,

    /**
     * 候选者可能成功，但接收者不匹配。
     */
    INAPPLICABLE_WRONG_RECEIVER,

    /**
     * 候选者可能成功，但参数映射错误（即参数和实参数量不同）。
     */
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,

    /**
     * 候选者可能成功，但参数类型错误（或其他一般不适用性）。
     */
    INAPPLICABLE,

    /**
     * 候选者可能成功，但使用了没有伴生对象的非对象分类符作为变量。
     */
    NO_COMPANION_OBJECT,

    /**
     * 候选者可能成功，但需要从嵌套（非内部）类访问外部类。
     */
    IMPOSSIBLE_TO_GENERATE,

    // TODO: 考虑重新分配此诊断 (RUNTIME_ERROR)

    /**
     * 适用所有其他错误的捕获。
     */
    RUNTIME_ERROR,

    /**
     * 候选者不可见。引发INVISIBLE_REFERENCE。
     */
    VISIBILITY_ERROR,

    /**
     * 候选者可能成功，但接收者（或参数？）的可空性不匹配。
     */
    UNSAFE_CALL,

    /**
     * 候选者可能成功，但需要不稳定的智能类型转换。
     */
    UNSTABLE_SMARTCAST,

    /**
     * 候选者可能成功，但不符合约定。
     * 例如，缺少中缀/操作符/等（= 没有预期的修饰符）。
     */
    CONVENTION_ERROR,

    // 以下的所有适用性都有 isSuccess = true (RESOLVED_WITH_ERROR 是例外)

    /**
     * 候选者成功，但优先级较低。
     * 解析塔继续进行到下一层。
     */
    RESOLVED_LOW_PRIORITY,

    /**
     * 候选者成功，但将函数类型的属性用作操作符。
     * 解析塔继续进行到下一层。
     */
    PROPERTY_AS_OPERATOR,

    /**
     * 候选者成功，但使用了改变解析的新功能。
     * 解析塔继续进行到下一层。
     */
    RESOLVED_NEED_PRESERVE_COMPATIBILITY,

    // 以下的所有适用性都有 shouldStopResolve = true
    // （如果找到具有以下适用性的候选者，解析塔不会进一步进入作用域）

    /**
     * 成功但为合成候选者。
     * 在K2中用于（Java）合成区分在同一层级。
     */
    SYNTHETIC_RESOLVED,

    /**
     * 候选者有一些错误，但从解析角度来看仍然是成功的。
     * 这意味着解析塔在此适用性处停止。
     * 但是，错误将被报告。
     * 这是唯一一个停止解析但引发错误的适用性。
     */
    RESOLVED_WITH_ERROR,

    /**
     * 候选者成功或推断未完成（因此可能是成功的）。
     */
    RESOLVED,
}

/**
 * This property determines that tower resolve should stop on the candidate/group with this applicability
 * and should not go to further scope levels. Note that candidate can still have error(s).
 */
val CandidateApplicability.shouldStopResolve: Boolean
    get() = this >= CandidateApplicability. SYNTHETIC_RESOLVED
/**
 * This property determines that the considered candidate is "successful" in terms of having no resolve errors.
 * Note that it does not necessarily mean tower resolve should stop on this candidate.
 */
val CandidateApplicability.isSuccess: Boolean
    get() = this >= CandidateApplicability.RESOLVED_LOW_PRIORITY && this != CandidateApplicability.RESOLVED_WITH_ERROR
