package com.linqingying.lsp.impl

import com.intellij.openapi.diagnostic.Logger
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.regex.PatternSyntaxException




fun getPathMatcher(globPattern: String): PathMatcher {
    val patterns = processGlobPattern(globPattern)
    val pathMatchers = patterns.mapNotNull { pattern ->
        try {
            FileSystems.getDefault().getPathMatcher("glob:$pattern")
        } catch (e: PatternSyntaxException) {
            Logger.getInstance("#com.linqingying.lsp.impl")
                .warn("Failed to compile glob pattern $pattern: ${e.message}")
            null
        }
    }
    return if (pathMatchers.size == 1) pathMatchers[0] else PathMatcher { path ->
        matchesAnyPathMatcher(pathMatchers, path)
    }
}

fun processGlobPattern(globPattern: String): List<String> {
    val braceIndex = globPattern.indexOf('{')
    val prefix = if (braceIndex == -1) globPattern else globPattern.substring(0, braceIndex)
    val suffix = globPattern.substring(prefix.length)
    val processedPrefix = prefix.replace("**/", "{**/,}")
    val result = mutableListOf(processedPrefix + suffix)
    var asteriskCount = 0
    var asteriskIndex = suffix.indexOf("**/")
    while (asteriskIndex >= 0) {
        asteriskCount++
        val processedSuffix = suffix.replaceRange(asteriskIndex, asteriskIndex + 3, "")
        result.add(processedPrefix + processedSuffix)
        asteriskIndex = suffix.indexOf("**/", asteriskIndex + 1)
    }
    if (asteriskCount > 1) {
        result.add(processedPrefix + suffix.replace("**/", ""))
    }
    return result
}

/**
 * 检查给定的路径是否匹配列表中的任何一个PathMatcher
 */
fun matchesAnyPathMatcher(matchers: List<PathMatcher>, path: Path): Boolean {
    return matchers.any { it.matches(path) }
}
