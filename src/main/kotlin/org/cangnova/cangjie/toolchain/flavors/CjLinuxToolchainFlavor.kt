package org.cangnova.cangjie.toolchain.flavors

import com.intellij.openapi.util.SystemInfo.isLinux
import java.nio.file.Path
import kotlin.io.path.isDirectory

class CjLinuxToolchainFlavor : CjToolchainFlavor() {
    override fun getHomePathCandidates(): Sequence<Path> {
        val baseDir = Path.of("/opt")
        return if (baseDir.isDirectory()) {
            baseDir.list()

                .filter {
                    it.isDirectory() && (it.fileName.toString() == "cangjie"
                            || it.fileName.toString()
                        .startsWith("cangjie-"))
                }.filter { it.isDirectory() }
        } else {
            emptySequence()
        }
    }

    override fun isApplicable(): Boolean = isLinux
}