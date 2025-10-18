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

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


// 2个上下文参数
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

// 3个上下文参数
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

// 4个上下文参数
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
