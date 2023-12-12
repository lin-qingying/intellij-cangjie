package com.huawei.cangjie.idea.run


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
                "test", "bench" -> parseTestArgs(cjpmArgs)
                else -> error("Unsupported command")
            }

        private val RUN_OPTIONS: Map<String, OptionArgsCountRange> =
            hashMapOf(
                "--bin" to OptionArgsCountRange.ONE,
                "--example" to OptionArgsCountRange.ONE,
                "-p" to OptionArgsCountRange.ONE,
                "--package" to OptionArgsCountRange.ONE,
                "-j" to OptionArgsCountRange.ONE,
                "--jobs" to OptionArgsCountRange.ONE,
                "--color" to OptionArgsCountRange.ONE,
                "--profile" to OptionArgsCountRange.ONE,
                "-F" to OptionArgsCountRange.MANY,
                "--features" to OptionArgsCountRange.MANY,
                "--config" to OptionArgsCountRange.ONE,
                "-Z" to OptionArgsCountRange.ONE,
                "--target" to OptionArgsCountRange.ONE,
                "--target-dir" to OptionArgsCountRange.ONE,
                "--manifest-path" to OptionArgsCountRange.ONE,
                "--message-format" to OptionArgsCountRange.ONE
            )
        private val TEST_OPTIONS: Map<String, OptionArgsCountRange> =
            RUN_OPTIONS + hashMapOf(
                "--test" to OptionArgsCountRange.ONE,
                "--bench" to OptionArgsCountRange.ONE,
                "--exclude" to OptionArgsCountRange.ONE
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
