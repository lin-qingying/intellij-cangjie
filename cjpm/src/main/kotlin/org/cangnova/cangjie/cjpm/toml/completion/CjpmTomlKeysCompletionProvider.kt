package org.cangnova.cangjie.cjpm.toml.completion

import org.cangnova.cangjie.cjpm.toml.isDependencyListHeader
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.cangnova.cangjie.psi.psiUtil.ancestorStrict
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlArrayTable
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlHeaderOwner
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader

private val TomlKeySegment.topLevelTable: TomlKeyValueOwner?
    get() {
        val table = ancestorStrict<TomlKeyValueOwner>() ?: return null
        if (table.parent !is TomlFile) return null
        return table
    }

class CjpmTomlKeysCompletionProvider : CompletionProvider<CompletionParameters>() {
    private var cachedSchema: TomlSchema? = null

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val schema = cachedSchema
            ?: TomlSchema.parse(parameters.position.project, EXAMPLE_CJPM_TOML).also { cachedSchema = it }

        val key = parameters.position.parent as? TomlKeySegment ?: return
        val table = key.topLevelTable ?: return
        val variants = when (val parent = key.parent?.parent) {
            is TomlTableHeader -> {
                if (key != parent.key?.segments?.firstOrNull()) return
                val isArray = when (table) {
                    is TomlArrayTable -> true
                    is TomlTable -> false
                    else -> return
                }
                schema.topLevelKeys(isArray)
            }

            is TomlKeyValue -> {
                if (table !is TomlHeaderOwner) return
                if (table.header.isDependencyListHeader) return
                schema.keysForTable(table.name ?: return)
            }

            else -> return
        }

        result.addAllElements(variants.map {
            LookupElementBuilder.create(it)
        })
    }

}

private val TomlHeaderOwner.name: String?
    get() = header.key?.segments?.firstOrNull()?.name


@Language("TOML")
private val EXAMPLE_CJPM_TOML = """
[package] # 单模块配置字段，与 workspace 字段不能同时存在
  cjc-version = "0.49.1" # 所需 `cjc` 的最低版本要求，必需
  name = "demo" # 模块名及模块 root 包名，必需
  description = "nothing here" # 描述信息，非必需
  version = "1.0.0" # 模块版本信息，必需
  compile-option = "" # 额外编译命令选项，非必需
  override-compile-option = "" # 额外全局编译命令选项，非必需
  link-option = "" # 链接器透传选项，可透传安全编译命令，非必需
  output-type = "executable" # 编译输出产物类型，必需
  src-dir = "" # 指定源码存放路径，非必需
  target-dir = "" # 指定产物存放路径，非必需
  package-configuration = {} # 单包配置选项，非必需

[workspace] # 工作空间管理字段，与 package 字段不能同时存在
  name = ""
  members = [] # 工作空间成员模块列表，必需
  build-members = [] # 工作空间编译模块列表，需要是成员模块列表的子集，非必需
  test-members = [] # 工作空间测试模块列表，需要是编译模块列表的子集，非必需
  compile-option = "" # 应用于所有工作空间成员模块的额外编译命令选项，非必需
  override-compile-option = "" # 应用于所有工作空间成员模块的额外全局编译命令选项，非必需
  link-option = "" # 应用于所有工作空间成员模块的链接器透传选项，非必需
  target-dir = "" # 指定产物存放路径，非必需

[dependencies] # 源码依赖配置项
  coo = { git = "xxx",branch = "dev" , version = "1.0.0"} # 导入 `git` 依赖，`version`字段可缺省
  doo = { path = "./pro1" ,version = "1.0.0"} # 导入源码依赖，`version`字段可缺省

[test-dependencies] # 测试阶段的依赖配置项，格式同 dependencies

[ffi.c] # 导入 `c` 库依赖
  clib1.path = "xxx"
  hello = { path = "./src/" }

[profile] # 命令剖面配置项
  build = {}
  test = {}
  test.build = {}
  test.env = {}
  bench = {}
  bench.build = {}
  bench.env = {}
  customized-option = {}
  run = {}

[target.x86_64-unknown-linux-gnu] # 后端和平台隔离配置项
  compile-option = "value1" # 额外编译命令选项，适用于特定 target 的编译流程和指定该 target 作为交叉编译目标平台的编译流程，非必需
  override-compile-option = "value2" # 额外全局编译命令选项，适用于特定 target 的编译流程和指定该 target 作为交叉编译目标平台的编译流程，非必需
  link-option = "value3" # 链接器透传选项，适用于特定 target 的编译流程和指定该 target 作为交叉编译目标平台的编译流程，非必需

[target.x86_64-w64-mingw32.dependencies] # 适用于对应 target 的源码依赖配置项，非必需

[target.x86_64-w64-mingw32.test-dependencies] # 适用于对应 target 的测试阶段依赖配置项，非必需

[target.x86_64-unknown-linux-gnu.bin-dependencies] # 仓颉二进制库依赖，适用于特定 target 的编译流程和指定该 target 作为交叉编译目标平台的编译流程，非必需
  path-option = ["./test/pro0", "./test/pro1"]
[target.x86_64-unknown-linux-gnu.bin-dependencies.package-option]
  "pro0.xoo" = "./test/pro0/pro0.xoo.cjo"
  "pro0.yoo" = "./test/pro0/pro0.yoo.cjo"
  "pro1.zoo" = "./test/pro1/pro1.zoo.cjo"

"""