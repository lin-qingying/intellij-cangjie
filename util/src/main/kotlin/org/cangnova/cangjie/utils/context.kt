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

package org.cangnova.cangjie.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * 使用 2 个上下文参数执行代码块
 *
 * 该函数允许在代码块中同时使用两个接收者对象，
 * 第一个作为扩展接收者，第二个作为普通参数。
 *
 * 示例：
 * ```kotlin
 * withs(stringBuilder, list) { items ->
 *     append("Items: ")
 *     items.forEach { append(it) }
 * }
 * ```
 *
 * @param T1 第一个接收者类型（扩展接收者）
 * @param T2 第二个接收者类型（参数）
 * @param R 返回值类型
 * @param receiver1 第一个接收者（扩展接收者）
 * @param receiver2 第二个接收者（参数）
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T1, T2, R> withs(
    receiver1: T1,
    receiver2: T2,
    block: T1.(T2) -> R
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return receiver1.block(receiver2)
}

/**
 * 使用 3 个上下文参数执行代码块
 *
 * 该函数允许在代码块中同时使用三个接收者对象，
 * 第一个作为扩展接收者，其余两个作为普通参数。
 *
 * 示例：
 * ```kotlin
 * withs(printer, header, footer) { h, f ->
 *     println(h)
 *     println("Content")
 *     println(f)
 * }
 * ```
 *
 * @param T1 第一个接收者类型（扩展接收者）
 * @param T2 第二个接收者类型（参数）
 * @param T3 第三个接收者类型（参数）
 * @param R 返回值类型
 * @param receiver1 第一个接收者（扩展接收者）
 * @param receiver2 第二个接收者（参数）
 * @param receiver3 第三个接收者（参数）
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T1, T2, T3, R> withs(
    receiver1: T1,
    receiver2: T2,
    receiver3: T3,
    block: T1.(T2, T3) -> R
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return receiver1.block(receiver2, receiver3)
}

/**
 * 使用 4 个上下文参数执行代码块
 *
 * 该函数允许在代码块中同时使用四个接收者对象，
 * 第一个作为扩展接收者，其余三个作为普通参数。
 *
 * 示例：
 * ```kotlin
 * withs(builder, config, context, logger) { cfg, ctx, log ->
 *     log.info("Building with config: $cfg")
 *     build(ctx)
 * }
 * ```
 *
 * @param T1 第一个接收者类型（扩展接收者）
 * @param T2 第二个接收者类型（参数）
 * @param T3 第三个接收者类型（参数）
 * @param T4 第四个接收者类型（参数）
 * @param R 返回值类型
 * @param receiver1 第一个接收者（扩展接收者）
 * @param receiver2 第二个接收者（参数）
 * @param receiver3 第三个接收者（参数）
 * @param receiver4 第四个接收者（参数）
 * @param block 要执行的代码块
 * @return 代码块的返回值
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T1, T2, T3, T4, R> withs(
    receiver1: T1,
    receiver2: T2,
    receiver3: T3,
    receiver4: T4,
    block: T1.(T2, T3, T4) -> R
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return receiver1.block(receiver2, receiver3, receiver4)
}
