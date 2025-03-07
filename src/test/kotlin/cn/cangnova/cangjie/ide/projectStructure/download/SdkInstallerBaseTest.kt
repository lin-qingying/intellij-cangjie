package cn.cangnova.cangjie.ide.projectStructure.download

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.Path

class SdkInstallerBaseTest {
    @Test
    fun installSdk() {
        SdkInstaller().installSdk(
            SdkInstallRequestInfo(
                SdkItem(
                    true,
                    "windows",
                    "x64",
                    "1.0.0",
                    "",
                    "", SdkPackageType.ZIP, "zip", "", saveToFile = {

                    }

                ), Paths.get("C:\\Users\\27439\\.cangjie\\sdks")
            ), null, null
        )


    }


    @Test
    fun testZip() {

    }
}