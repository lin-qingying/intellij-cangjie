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

package org.cangnova.cangjie.ide.run.cjpm


class CjpmArgsParser private constructor(
    private val cjpmArgs: List<String>,
    private val optionsToArgsCountRange: Map<String, OptionArgsCountRange>
) {
    private val OPTION_CHAR_CLASS: String = "[a-zA-Z0-9]"
    private val OPTION_NAME_RE: Regex =
        Regex("^(-$OPTION_CHAR_CLASS)|(--$OPTION_CHAR_CLASS+([-_.]$OPTION_CHAR_CLASS+)*)$")

    private fun getActualOptionArgsCount(optionIdx: Int): Int {
        val option = cjpmArgs[optionIdx]
        // Unknown options are assumed to be the flagging options
        val (_, maxArgsCount) = optionsToArgsCountRange.getOrDefault(option, OptionArgsCountRange.ZERO)
        return cjpmArgs.asSequence()
            .drop(optionIdx + 1)
            .takeWhile { it != "--" && !OPTION_NAME_RE.matches(it) }
            .take(maxArgsCount)
            .count()
    }
    private fun splitArgs(): SplitCjpmArgs {
        val commandOptions = mutableListOf<String>()
        val positionalArguments = mutableListOf<String>()

        var i = 0
        optionLoop@ while (i < cjpmArgs.size) {
            val arg = cjpmArgs[i]
            i += when {
                arg == "--" -> { // End of options
                    // Collect remaining arguments as positional-only arguments
                    positionalArguments.addAll(cjpmArgs.subList(i, cjpmArgs.size))
                    break@optionLoop
                }
                arg.startsWith("-") -> { // An option
                    val optionArgsCount = getActualOptionArgsCount(i)
                    val optionWithArgs = cjpmArgs.subList(i, i + optionArgsCount + 1)
                    commandOptions.addAll(optionWithArgs)
                    optionWithArgs.size
                }
                else -> { // An positional arg
                    positionalArguments.add(arg)
                    1
                }
            }
        }

        return SplitCjpmArgs(commandOptions, positionalArguments)
    }
    companion object {
        fun parseArgs(commandName: String, cjpmArgs: List<String>): ParsedCjpmArgs =
            when (commandName) {
                "run" -> parseRunArgs(cjpmArgs)
                "test" -> parseTestArgs(cjpmArgs)
                else -> error("Unsupported command")
            }

        private val RUN_OPTIONS: Map<String, OptionArgsCountRange> =
            hashMapOf(
                "--bin" to OptionArgsCountRange.ONE,

                "--package" to OptionArgsCountRange.ONE,


                "--target" to OptionArgsCountRange.ONE,
                "--target-dir" to OptionArgsCountRange.ONE,

            )
        private val TEST_OPTIONS: Map<String, OptionArgsCountRange> =
            RUN_OPTIONS + hashMapOf(
                "--test" to OptionArgsCountRange.ONE,

            )
        private fun parseTestArgs(cjpmArgs: List<String>): ParsedCjpmArgs {
            val argsParser = CjpmArgsParser(cjpmArgs, TEST_OPTIONS)
            val (commandOptions, positionalArguments) = argsParser.splitArgs()
            val (positionalPre, positionalPost) = splitOnDoubleDash(positionalArguments)

            // Don't drop the last element of the `positionalPre` so that Cjpm will check the arguments
            val commandArguments = commandOptions + positionalPre

            // The last positional argument before `--` and all arguments after are passed to the test binary
            val executableArguments = listOfNotNull(positionalPre.lastOrNull()) + positionalPost

            return ParsedCjpmArgs(commandArguments, executableArguments)
        }
        private fun parseRunArgs(cjpmArgs: List<String>): ParsedCjpmArgs {
            val argsParser = CjpmArgsParser(cjpmArgs, RUN_OPTIONS)
            val (commandArguments, positionalArguments) = argsParser.splitArgs()
            val executableArguments = if (positionalArguments.isNotEmpty() && positionalArguments.first() == "--") {
                positionalArguments.drop(1)
            } else {
                positionalArguments
            }
            return ParsedCjpmArgs(commandArguments, executableArguments)
        }
    }
}
    private data class OptionArgsCountRange(val min: Int, val max: Int) {
        companion object {
            val ZERO = OptionArgsCountRange(0, 0)
            val ONE = OptionArgsCountRange(1, 1)
            val MANY = OptionArgsCountRange(1, Int.MAX_VALUE)
        }
    }
data class ParsedCjpmArgs(val commandArguments: List<String>, val executableArguments: List<String>)
private data class SplitCjpmArgs(val commandOptions: List<String>, val positionalArguments: List<String>)
fun splitOnDoubleDash(arguments: List<String>): Pair<List<String>, List<String>> {
    val idx = arguments.indexOf("--")
    if (idx == -1) return arguments to emptyList()
    return arguments.take(idx) to arguments.drop(idx + 1)
}
