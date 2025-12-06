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

package org.cangnova.cangjie.types

/**
 * 增强投影类型枚举
 *
 * 表示类型投影的四种可能情况，用于类型系统中处理型变（variance）和类型参数投影。
 *
 * ## ⚠️ 当前状态说明
 *
 * **当前仓颉语言仅支持 INV（不变）类型投影**。
 * IN、OUT、STAR 投影类型是为未来语言扩展预留的设计，暂未启用。
 *
 * 这种设计允许：
 * - 类型系统具有扩展性，无需重构核心代码
 * - 为未来支持协变/逆变提供基础架构
 * - 代码编写时考虑未来的型变需求
 *
 * ## 投影类型说明（未来扩展）
 *
 * - **INV (不变/invariant)**: ✅ **当前支持**
 *   - 既可输入也可输出
 *   - 类型参数必须完全匹配
 *   ```
 *   MutableList<T>  // T 既可读也可写，必须是精确类型
 *   ```
 *
 * - **IN (逆变/contravariant)**: 🔮 **未来扩展**
 *   - 只能作为输入，如函数参数
 *   - 允许传入子类型
 *   ```
 *   Consumer<in T>  // 可以接受 T 或 T 的子类型
 *   ```
 *
 * - **OUT (协变/covariant)**: 🔮 **未来扩展**
 *   - 只能作为输出，如函数返回值
 *   - 允许返回父类型
 *   ```
 *   Producer<out T>  // 可以返回 T 或 T 的父类型
 *   ```
 *
 * - **STAR (星投影)**: 🔮 **未来扩展**
 *   - 未知类型的投影，最大灵活性但功能受限
 *   - 相当于协变的上界投影
 *   ```
 *   List<*>  // 可以读取为 Any，但不能写入具体类型
 *   ```
 *
 * ## 与 Variance 的关系
 *
 * EnrichedProjectionKind 是对 Variance 的扩展，增加了 STAR 类型：
 * - Variance: IN, OUT, INVARIANT
 * - EnrichedProjectionKind: IN, OUT, INV, STAR
 *
 * ## 未来演进路径
 *
 * 1. **第一阶段（当前）**: 仅支持 INV，所有类型参数都是不变的
 * 2. **第二阶段（规划中）**: 支持声明处型变（declaration-site variance）
 *    - 允许 `class Producer<out T>` 这样的声明
 * 3. **第三阶段（规划中）**: 支持使用处型变（use-site variance）
 *    - 允许 `List<out String>` 这样的使用
 * 4. **第四阶段（规划中）**: 支持星投影
 *    - 允许 `List<*>` 这样的类型通配
 *
 * @see Variance 型变枚举
 * @see getEffectiveProjectionKind 计算有效投影类型的方法（为未来准备）
 */
enum class EnrichedProjectionKind {
    /**
     * 逆变投影：只能作为输入（消费者）
     * 🔮 **未来扩展功能** - 当前仓颉语言暂不支持
     */
    IN,

    /**
     * 协变投影：只能作为输出（生产者）
     * 🔮 **未来扩展功能** - 当前仓颉语言暂不支持
     */
    OUT,

    /**
     * 不变投影：既可输入也可输出
     * ✅ **当前支持** - 仓颉语言目前仅支持此投影类型
     */
    INV,

    /**
     * 星投影：未知类型的投影
     * 🔮 **未来扩展功能** - 当前仓颉语言暂不支持
     */
    STAR;

    companion object {
        /**
         * 从型变转换为投影类型
         *
         * 将 Variance 枚举值转换为对应的 EnrichedProjectionKind。
         *
         * ## ⚠️ 当前实现说明
         *
         * 由于仓颉语言当前仅支持 INVARIANT，这个方法目前只会返回 INV。
         * IN 和 OUT 的映射是为未来扩展预留的代码路径。
         *
         * ## 映射关系（未来完整支持后）
         *
         * - INVARIANT -> INV ✅ 当前支持
         * - IN_VARIANCE -> IN 🔮 未来扩展
         * - OUT_VARIANCE -> OUT 🔮 未来扩展
         *
         * @param variance 型变类型
         * @return 对应的投影类型（当前始终返回 INV）
         */
        @JvmStatic
        fun fromVariance(variance: Variance): EnrichedProjectionKind {
            return when (variance) {
                Variance.INVARIANT -> INV
//                Variance.IN_VARIANCE -> IN
//                Variance.OUT_VARIANCE -> OUT
            }
        }

        /**
         * 获取有效的投影类型
         *
         * 根据类型参数的声明型变和类型实参的使用型变，计算最终的有效投影类型。
         *
         * ## ⚠️ 当前实现说明
         *
         * **由于仓颉语言目前仅支持 INVARIANT（不变）类型投影，这个方法当前总是返回 INV。**
         *
         * 下面的型变组合规则和实现逻辑是为未来语言扩展预留的设计，当仓颉语言支持
         * 协变（out）和逆变（in）时，这些规则将被启用。
         *
         * ## 型变组合规则（未来扩展）
         *
         * 当未来支持型变时，将遵循以下组合逻辑：
         *
         * ```
         * 类型参数 \ 类型实参 |  out  |  in   |  inv
         * ---------------------|-------|-------|-------
         *        out           |  out  |   *   |  out
         *        in            |   *   |  in   |  in
         *        inv           |  out  |  in   |  inv
         * ```
         *
         * ## 规则详解（未来扩展）
         *
         * ### 1. out * out = out (协变叠加)
         * ```kotlin
         * class Producer<out T>           // 声明协变
         * val p: Producer<out String>     // 使用协变
         * // 结果：out（仍然是协变）
         * ```
         *
         * ### 2. out * in = * (协变与逆变冲突)
         * ```kotlin
         * class Producer<out T>           // 声明协变
         * val p: Producer<in String>      // 使用逆变（矛盾！）
         * // 结果：*（星投影，类型不安全）
         * ```
         *
         * ### 3. out * inv = out (协变主导)
         * ```kotlin
         * class Producer<out T>           // 声明协变
         * val p: Producer<String>         // 使用不变
         * // 结果：out（保持协变）
         * ```
         *
         * ### 4. in * out = * (逆变与协变冲突)
         * ```kotlin
         * class Consumer<in T>            // 声明逆变
         * val c: Consumer<out String>     // 使用协变（矛盾！）
         * // 结果：*（星投影）
         * ```
         *
         * ### 5. in * in = in (逆变叠加)
         * ```kotlin
         * class Consumer<in T>            // 声明逆变
         * val c: Consumer<in String>      // 使用逆变
         * // 结果：in（仍然是逆变）
         * ```
         *
         * ### 6. in * inv = in (逆变主导)
         * ```kotlin
         * class Consumer<in T>            // 声明逆变
         * val c: Consumer<String>         // 使用不变
         * // 结果：in（保持逆变）
         * ```
         *
         * ### 7. inv * out = out (不变 + 协变)
         * ```kotlin
         * class Box<T>                    // 声明不变
         * val b: Box<out String>          // 使用协变
         * // 结果：out（采用使用处型变）
         * ```
         *
         * ### 8. inv * in = in (不变 + 逆变)
         * ```kotlin
         * class Box<T>                    // 声明不变
         * val b: Box<in String>           // 使用逆变
         * // 结果：in（采用使用处型变）
         * ```
         *
         * ### 9. inv * inv = inv (不变叠加) ✅ **当前支持**
         * ```kotlin
         * class Box<T>                    // 声明不变
         * val b: Box<String>              // 使用不变
         * // 结果：inv（保持不变）
         * ```
         *
         * ## 实现策略（未来扩展）
         *
         * 为了简化逻辑，采用以下策略：
         * 1. 如果都是 INVARIANT，直接返回 INV ✅ 当前生效
         * 2. 确保 b 不是 INVARIANT（必要时交换 a 和 b）🔮 未来生效
         * 3. 如果方向相反（一个 IN 一个 OUT），返回 STAR 🔮 未来生效
         * 4. 否则返回非 INVARIANT 的那个 🔮 未来生效
         *
         * @param typeParameterVariance 类型参数的声明型变（当前仅支持 INVARIANT）
         * @param typeArgumentVariance 类型实参的使用型变（当前仅支持 INVARIANT）
         * @return 有效的投影类型（当前始终返回 INV）
         *
         * @see Variance 型变枚举
         */
        @JvmStatic
        fun getEffectiveProjectionKind(
            typeParameterVariance: Variance,
            typeArgumentVariance: Variance
        ): EnrichedProjectionKind {
            // 1. 如果两者都是不变，直接返回 INV
            if (typeParameterVariance === Variance.INVARIANT &&
                typeArgumentVariance === Variance.INVARIANT) {
                return INV
            }

            var a = typeParameterVariance
            var b = typeArgumentVariance

            // 2. 确保 b 不是 INVARIANT（如果需要则交换）
            // 这样可以简化后续逻辑，因为我们只需要处理 b 是 IN 或 OUT 的情况
            if (b === Variance.INVARIANT) {
                val temp = a
                a = b
                b = temp
            }

            // 3. 此时 b 一定不是 INVARIANT（要么是 IN 要么是 OUT）
            // 检查是否方向相反
//            if (a === Variance.IN_VARIANCE && b === Variance.OUT_VARIANCE ||
//                a === Variance.OUT_VARIANCE && b === Variance.IN_VARIANCE) {
//                // 方向冲突：out * in = * 或 in * out = *
//                return STAR
//            }

            // 4. 如果不是相反方向，返回 b
            // 因为 b 要么等于 a（叠加），要么 a 是 inv 而 b 是 in/out（采用使用处型变）
            return fromVariance(b)
        }
    }
}