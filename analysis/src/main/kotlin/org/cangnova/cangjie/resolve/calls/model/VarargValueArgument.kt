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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument


class VarargValueArgument(
    arguments: List<ValueArgument> = emptyList()
) : ResolvedValueArgument {
    override val arguments = mutableListOf<ValueArgument>()

    init {
        this.arguments.addAll(arguments)
    }


    fun addArgument(argument: ValueArgument) {
        arguments.add(argument)
    }


    override fun toString(): String {
        val builder = StringBuilder("vararg:{")
        val iterator: Iterator<ValueArgument> = arguments.iterator()
        while (iterator.hasNext()) {
            val valueArgument: ValueArgument = iterator.next()
            val expression = valueArgument.getArgumentExpression()
            builder.append(if (expression == null) "no expression" else expression.text)
            if (iterator.hasNext()) {
                builder.append(", ")
            }
        }
        return builder.append("}").toString()
    }
}
