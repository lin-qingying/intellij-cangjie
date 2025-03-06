package cn.cangnova.cangjie.cjpm.project.model.toml

import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CjpmTomlParserTest {
    

    fun `test parse basic package config`() {
        val content = """
            [package]
            cjc-version = "0.55.3"
            compile-option = "-O2"
            description = "YAML解析工具"
            link-option = ""
            name = "yaml4cj"
            output-type = "dynamic"
            src-dir = "src"
            target-dir = ""
            version = "0.0.1"
            package-configuration = {}
        """.trimIndent()
        
        val config = CjpmTomlParser.parse(content)
        
        assertNotNull(config.`package`)
        with(config.`package`) {
            assertEquals("0.55.3", cjcVersion)
            assertEquals("-O2", compileOption)
            assertEquals("YAML解析工具", description)
            assertEquals("", linkOption)
            assertEquals("yaml4cj", name)
            assertEquals(OutputType.DYNAMIC, outputType)
            assertEquals("src", srcDir)
            assertEquals("", targetDir)
            assertEquals("0.0.1", version)
        }
    }
    

    fun `test parse dependencies`() {
        val content = """
            [dependencies]
            charset4cj = {branch = "v0.0.1.B002", git = "https://gitcode.com/Cangjie-TPC/charset4cj.git"}
            
            [package]
            name = "test"
            cjc-version = "0.55.3"
            version = "0.0.1"
            output-type = "dynamic"
        """.trimIndent()
        
        val config = CjpmTomlParser.parse(content)
        
        assertEquals(1, config.dependencies.size)
        with(config.dependencies["charset4cj"]!!) {
            assertEquals("v0.0.1.B002", branch)
            assertEquals("https://gitcode.com/Cangjie-TPC/charset4cj.git", git)
        }
    }
    

    fun `test parse target config`() {
        val content = """
            [target.aarch64-linux-ohos]
            compile-option = "-B\"${"\$"}{DEVECO_CANGJIE_HOME}/compiler/third_party/llvm/bin\""
            
            [target.aarch64-linux-ohos.bin-dependencies]
            path-option = ["${"\$"}{DEVECO_CANGJIE_HOME}/build/linux_ohos_aarch64_llvm/ohos"]
            package-option = {}
            
            [package]
            name = "test"
            cjc-version = "0.55.3"
            version = "0.0.1"
            output-type = "dynamic"
        """.trimIndent()
        
        val config = CjpmTomlParser.parse(content)
        
        val targetConfig = config.target["aarch64-linux-ohos"]
        assertNotNull(targetConfig)
        assertEquals("-B\"\${DEVECO_CANGJIE_HOME}/compiler/third_party/llvm/bin\"", targetConfig.compileOption)
        
        assertNotNull(targetConfig.binDependencies)
        assertEquals(1, targetConfig.binDependencies.pathOption?.size)
        assertEquals("\${DEVECO_CANGJIE_HOME}/build/linux_ohos_aarch64_llvm/ohos", 
            targetConfig.binDependencies.pathOption?.get(0))
    }
    

    fun `test parse from file`() {
        val file = File(javaClass.getResource("/project_toml_files/14/cjpm.toml")!!.file)
        val config = CjpmTomlParser.parse(file)
        
        assertNotNull(config.`package`)
        assertEquals("yaml4cj", config.`package`.name)
    }
    

    fun `test write config to string`() {
        val config = CjpmTomlConfig(
            `package` = PackageConfig(
                cjcVersion = "0.55.3",
                name = "test",
                version = "0.0.1",
                outputType = OutputType.DYNAMIC
            )
        )
        
        val tomlString = CjpmTomlParser.writeToString(config)
        
        // Parse back to verify
        val parsedConfig = CjpmTomlParser.parse(tomlString)
        assertEquals(config.`package`?.name, parsedConfig.`package`?.name)
    }
    

    fun `test invalid TOML content`() {
        val invalidContent = """
            [package
            name = test
        """.trimIndent()
        
        assertThrows<Exception> {
            CjpmTomlParser.parse(invalidContent)
        }
    }
    

    fun `test parse profile config`() {
        val content = """
            [profile]
            build = { incremental = false, lto = "" }
            customized-option = { debug = "-g", release = "--fast-math -O2" }
            
            [profile.test]
            no-color = true
            timeout-each = "30s"
            random-seed = 12345
            bench = false
            report-path = "./test-report"
            report-format = "xml"
            verbose = true
            
            [profile.test.build]
            compile-option = "-O0"
            lto = "thin"
            mock = "mock-config"
            
            [profile.test.env]
            TEST_VAR = { value = "test_value", splice-type = "replace" }
            
            [profile.bench]
            no-color = false
            report-path = "./bench-report"
            baseline-path = "./baseline"
            report-format = "csv"
            
            [package]
            name = "test"
            cjc-version = "0.55.3"
            version = "0.0.1"
            output-type = "dynamic"
        """.trimIndent()
        
        val config = CjpmTomlParser.parse(content)
        
        assertNotNull(config.profile)
        with(config.profile) {
            // 验证 build 配置
            assertNotNull(build)
            assertEquals(false, build.incremental)
            assertEquals("", build.lto)
            
            // 验证自定义选项
            assertNotNull(customizedOption)
            assertEquals("-g", customizedOption["debug"])
            assertEquals("--fast-math -O2", customizedOption["release"])
            
            // 验证测试配置
            assertNotNull(test)
            with(test) {
                assertEquals(true, noColor)
                assertEquals("30s", timeoutEach)
                assertEquals(12345, randomSeed)
                assertEquals(false, bench)
                assertEquals("./test-report", reportPath)
                assertEquals("xml", reportFormat)
                assertEquals(true, verbose)
                
                // 验证测试构建配置
                assertNotNull(build)
                assertEquals("-O0", build.compileOption)
                assertEquals("thin", build.lto)
                assertEquals("mock-config", build.mock)
                
                // 验证环境变量配置
                assertNotNull(env)
                val testVar = env["TEST_VAR"]
                assertNotNull(testVar)
                assertEquals("test_value", testVar.value)
                assertEquals(SpliceType.REPLACE, testVar.spliceType)
            }
            
            // 验证性能测试配置
            assertNotNull(bench)
            with(bench) {
                assertEquals(false, noColor)
                assertEquals("./bench-report", reportPath)
                assertEquals("./baseline", baselinePath)
                assertEquals("csv", reportFormat)
            }
        }
    }
    

    fun `test parse ffi config`() {
        val content = """
            [ffi.c]
            latex = { path = "./libs/arm64-v8a/" }
            openssl = { path = "./libs/openssl/" }
            
            [package]
            name = "test"
            cjc-version = "0.55.3"
            version = "0.0.1"
            output-type = "dynamic"
        """.trimIndent()
        
        val config = CjpmTomlParser.parse(content)
        
        assertNotNull(config.ffi?.c)
        assertEquals(2, config.ffi .c.size)
        assertEquals("./libs/arm64-v8a/", config.ffi.c["latex"]?.path)
        assertEquals("./libs/openssl/", config.ffi.c["openssl"]?.path)
    }
    

    fun `test parse complex target config`() {
        val content = """
            [target.x86_64-unknown-linux-gnu]
            compile-option = "-O2"
            override-compile-option = "--fast-math"
            link-option = "-static"
            
            [target.x86_64-unknown-linux-gnu.dependencies]
            dep1 = { path = "./dep1" }
            dep2 = { git = "https://example.com/dep2.git", branch = "main" }
            
            [target.x86_64-unknown-linux-gnu.test-dependencies]
            test-dep = { path = "./test-dep" }
            
            [target.x86_64-unknown-linux-gnu.bin-dependencies]
            path-option = ["/usr/local/lib", "/opt/lib"]
            package-option = { "pkg1" = "/path/to/pkg1", "pkg2" = "/path/to/pkg2" }
            
            [target.x86_64-unknown-linux-gnu.debug]
            compile-option = "-g"
            
            [target.x86_64-unknown-linux-gnu.release]
            compile-option = "-O3"
            
            [package]
            name = "test"
            cjc-version = "0.55.3"
            version = "0.0.1"
            output-type = "dynamic"
        """.trimIndent()
        
        val config = CjpmTomlParser.parse(content)
        
        val targetConfig = config.target["x86_64-unknown-linux-gnu"]
        assertNotNull(targetConfig)
        
        with(targetConfig) {
            // 基本配置
            assertEquals("-O2", compileOption)
            assertEquals("--fast-math", overrideCompileOption)
            assertEquals("-static", linkOption)
            
            // 依赖配置
            assertNotNull(dependencies)
            assertEquals(2, dependencies.size)
            assertEquals("./dep1", dependencies["dep1"]?.path)
            assertEquals("https://example.com/dep2.git", dependencies["dep2"]?.git)
            assertEquals("main", dependencies["dep2"]?.branch)
            
            // 测试依赖配置
            assertNotNull(testDependencies)
            assertEquals(1, testDependencies.size)
            assertEquals("./test-dep", testDependencies["test-dep"]?.path)
            
            // 二进制依赖配置
            assertNotNull(binDependencies)
            assertEquals(2, binDependencies.pathOption?.size)
            assertEquals("/usr/local/lib", binDependencies.pathOption?.get(0))
            assertEquals("/opt/lib", binDependencies.pathOption?.get(1))
            assertEquals(2, binDependencies.packageOption?.size)
            assertEquals("/path/to/pkg1", binDependencies.packageOption?.get("pkg1"))
            assertEquals("/path/to/pkg2", binDependencies.packageOption?.get("pkg2"))
            
            // debug 配置
            assertNotNull(debug)
            assertEquals("-g", debug.compileOption)
            
            // release 配置
            assertNotNull(release)
            assertEquals("-O3", release.compileOption)
        }
    }
    

    fun `test parse workspace config`() {
        val content = """
            [workspace]
            members = [
                "packages/*",
                "tools/package1",
                "tools/package2"
            ]
            build-members = [
                "packages/core",
                "tools/package1"
            ]
            test-members = [
                "packages/core"
            ]
            compile-option = "-O2"
            override-compile-option = "--fast-math"
            link-option = "-static"
            target-dir = "./target"
            
            [package]
            name = "test"
            cjc-version = "0.55.3"
            version = "0.0.1"
            output-type = "dynamic"
        """.trimIndent()
        
        val config = CjpmTomlParser.parse(content)
        
        assertNotNull(config.workspace)
        with(config.workspace) {
            // 验证成员列表
            assertEquals(3, members.size)
            assertTrue(members.contains("packages/*"))
            assertTrue(members.contains("tools/package1"))
            assertTrue(members.contains("tools/package2"))
            
            // 验证构建成员列表
            assertNotNull(buildMembers)
            assertEquals(2, buildMembers.size)
            assertTrue(buildMembers.contains("packages/core"))
            assertTrue(buildMembers.contains("tools/package1"))
            
            // 验证测试成员列表
            assertNotNull(testMembers)
            assertEquals(1, testMembers.size)
            assertTrue(testMembers.contains("packages/core"))
            
            // 验证编译选项
            assertEquals("-O2", compileOption)
            assertEquals("--fast-math", overrideCompileOption)
            assertEquals("-static", linkOption)
            assertEquals("./target", targetDir)
        }
    }
} 