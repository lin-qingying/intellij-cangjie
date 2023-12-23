# Platform Documentation

查看IDEA平台[文档][sdk-docs]。特别感兴趣的部分包括：
  * 自定义语言[教程][lang-tutorial]
  * 自定义语言支持[参考][lang-reference]。

如果您发现任何缺失或不清晰的SDK文档，请在[YouTrack][sdk-YouTrack]上提出问题。您也可以直接[贡献][sdk-contributing]到插件开发文档。

浏览现有插件也是非常有启发性的。查看[Erlang]和[Go]插件。还有[插件存储库]，当然还有[Kotlin插件]。

[Kotlin]: https://kotlinlang.org/
[sdk-docs]: https://www.jetbrains.org/intellij/sdk/docs/

[lang-reference]: https://www.jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support.html
[lang-tutorial]: https://www.jetbrains.org/intellij/sdk/docs/tutorials/custom_language_support_tutorial.html

[sdk-YouTrack]: https://youtrack.jetbrains.com/issues/IJSDK
[sdk-contributing]: https://github.com/JetBrains/intellij-sdk-docs/blob/master/CONTRIBUTING.md

[Erlang]: https://github.com/ignatov/intellij-erlang
[Go]: https://github.com/go-lang-plugin-org/go-lang-idea-plugin
[插件存储库]: https://github.com/JetBrains/intellij-plugins
[Kotlin插件]: https://github.com/JetBrains/kotlin
# Packages

插件由三个主要包组成：`org.rust.lang`、`org.rust.ide` 和 `org.rust.cargo`。

`lang` 包是插件的核心。它包括 Rust 语言的解析器、连接声明和用法的机制以及类型推断算法。完成和转到声明是使用 `lang` 包构建的。

The `cargo` package is used for integration with Cargo and rustup. Most importantly,
it describes the project model in `model` and `workspace` subpackages. The model
is roughly the data from `cargo metadata` command, but it also contains information 
about standard library and logic for automatic refresh based on `Cargo.toml` modifications.

`ide` 包使用 `cargo` 和 `lang` 包为用户提供有用的功能。它由许多子包组成。其中一些是

* `intentions`：用户可以使用 `Alt+Enter` 调用的操作，
* `inspections`：警告和快速修复，
* `navigation.goto`：利用 `lang.core.stubs.index` 提供 GoToSymbol 操作。

# Lexer

词法分析器在 `CangJieLexer.flex` 文件中指定。参考 [JFlex] 文档以了解其工作原理。调用 jflex 并从这个 `.jflex` 文件生成一个 Java 类。词法分析器很少改变，大部分时间都是完成的。

# Parser

The parser is generated from the BNF-like description of the language grammar in
the file `RustParser.bnf`. We use Intellij-specific parser generator [Grammar
Kit]. The corresponding gradle task is `generateRustParser`.

Grammar Kit [documentation][GK-docs] is on GitHub. You can also use
<kbd>Ctrl+Q</kbd> shortcut on any attribute in `RustGrammar.bnf` to read its
documentation.

At the high level, Grammar Kit generates a hand-written recursive descent
backtracking parser, which employs Pratt parser technique for parsing
left-recursive fragments of grammar. So, if you squint really hard, the
generated parser in `RustParser.java` looks like the rustc own parser in
`libsyntax`.

Besides the parser itself, Grammar Kit also generates the AST classes. Take a
look at the `StructItem` rule in the `RustParser.bnf` file and at the
corresponding `org.rust.lang.core.psi.RsStructItem` interface. You'll
see that each element at the right hand side of the `StructItem` rule has the
corresponding accessor method the interface. `?` modifier will cause the
accessor to be nullable, and `*` or `+` will result in the accessor returning a
`List` of elements.

As the IDE often works with incomplete code, a good parser recovery is
mandatory. The parser recovery consists of two parts.

Let's say that the user has typed `fn foo`. The parser must understand that this
is a function, despite the fact that argument list and body are missing. This is
handled with the `pin` attribute. For example, `pin = 'FN'` will cause parser to
produce a function AST node as soon as `fn` keyword is parsed. Consequently, the
accessor for the identifier in the AST interface will be nullable.

The second part of parser recovery is token skipping. Suppose the user added `fn
foo` to some existing code and got

```
fn foo
struct Bar {
    f: f32
}
```

Here, the parser should parse `Bar` as a struct despite the fact that the
preceding function is incomplete. This is handled with the `recoverWhile`
attribute which specifies the tokens to skip after completing (successfully or
not!) some rule. For example, `!(FN | STRUCT)` would work for language where
each declaration starts either with `fn` or with `struct`.

[JFlex]: https://www.jflex.de/
[Grammar Kit]: https://github.com/JetBrains/Grammar-Kit
[GK-docs]: https://github.com/JetBrains/Grammar-Kit/blob/master/HOWTO.md


# PSI

实际上，解析器生成的是称为PSI（程序结构接口）而不是AST（抽象语法树）。您可以将PSI视为AST，但它更通用。首先，它包括通常从AST中省略的内容：空格、注释和括号。PSI是程序结构的外观，可以有多个实现。使用“查看当前文件的PSI结构”操作来探索PSI，或安装PSI查看器插件并使用`Ctrl+Shift+Q`快捷键。

在插件中，PSI的组织方式相当复杂， 并且舍弃了最初使用语法生成工具的方式，而是使用手动编写。

PSI的组织方式如下：

* `com.huawei.cangjie.psi`：顶层的PSI接口，包括`CjFile`、`CjElement`、`CjNamedElement`  
阅读有关解析和PSI的更多信息，请参阅[sdk文档][psi-doc]

[psi-doc]: https://www.jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support/implementing_parser_and_psi.html
 
// 这是一个函数，函数是`NamedElement`。
fn foo() {}

fn bar() {
    // 这个`foo`实际上是对上面定义的函数的引用。
    // 因此，`this_foo.reference.resolve()`将返回`above_foo`
    foo()
}
```

名称解析的实现在Intellij-Rust和rustc中是不同的。编译器一次性解析整个crate，以自顶向下的方式遍历模块树。Intellij-Rust通过自下而上地遍历PSI树来延迟解析名称。这允许仅在当前在编辑器中打开的文件及其依赖项中进行解析，忽略大部分的crate。有关详细信息，请参阅`NameResolution.kt`。

解析的结果被缓存。缓存在每次PSI修改后被清除。也就是说，在编辑器中键入键后，所有名称解析信息都会被遗忘和重新计算，但仅适用于当前打开的文件。有关缓存的详细信息，请参阅`com.intellij.psi.util.CachedValuesManager`和`com.intellij.psi.util.PsiModificationTracker`。


# Type inference

Type inference is implemented in `org.rust.lang.core.types.infer` package. It is
mostly modeled after rustc type checking.

All inference happens at a function/constant level (at a PSI element that
implements `RsInferenceContextOwner`). We walk function/constant body top
down, processing every expression and statement. The aim is to
construct a map from expressions to their types
(`RsInferenceResult`). 

If the type of expression is obvious (is not generic), we record it
right away. However sometimes we can't infer the type of expression
precisely without context, for example:

```Rust
let mut a = 0; 
// We need this assignment to learn that `a: u64`
a += 92u64;
```

In this case, we create a fresh type variables for this type, and
record it in a special table called `UnificationTable` as a type to be
determined. Later, when we process `a += 92u64`, we learn the precise
type and record it.


More complex constraints appear when we process generics and traits,
for example:

```Rust
trait Foo<T> { }

struct S1;
struct S2;
impl Foo<S2> for S1 {}

fn foo<A: Foo<B>, B>(a: A) -> B

fn main() {
	let s2 = foo(S1);
}
```

For them, the basic algorithm is the same: type variables are
constructed for unknown types, constraints are recorded into a special
data structure, `ObligationForest`, and, once the constraints are
resolved, the results are recorded via `UnificationTable`.


# Indexing

Intellij provides a powerful API for indexing source code. Roughly speaking, you
can build an arbitrary map from some keys to some values using all the source
code in the project and then query the map. The Intellij will make sure that the
mapping always stays fresh. The crucial restriction is that for each file the
mapping must be computed independently. That is, you can store a mapping from
struct names to struct definitions, but you can't map a struct to the
grandparent module. This restriction allows fast recalculation of the index:
only the part corresponding to the changed files needs to be flushed. This also
allows to persist indexes to disk between IDE invocations.

These indexes power go to class and go to symbol functionality. They are also
used during resolve to find the parent module for a file and to get the list of
`impl`s for a type.

## Stubs

The main use of indexes is for building a stub tree. Stub tree is a condensed
AST, which includes information necessary for the resolve and nothing more. That
is, `struct`s and functions declarations are present in stubs, but function
bodies and local variables are omitted. That way, you can list declarations
inside a file without parsing it, which saves a lot of CPU time, because stubs
are stored in the compact binary format.

The cool thing is that PSI can dynamically [switch](stub-switch) between stub
based and AST based implementation. It provides a nice unified programming API
(as opposed to separate APIs for AST and stub-based implementation), but means
that you can accidentally cause a file reparse if you use some API which is
implemented only by AST.

Rust stubs are in defined `org.rust.lang.core.stubs` package.

All other indexes are implemented on top of the stubs. When constructing a stub
tree, you may associated current stub-based PSI element with some key. Latter,
you can use this key to retrieve the element.

## RsModulesIndex

RsModulesIndex is an example of simple but useful stub-based index. It is used
to answer the question: "given the `foo.rs` file, what is its parent
module?". Search for the usages of `RsModulesIndex.KEY` to see how the index
is populated and queried.

The naive solution is to find a `mod.rs` file in the containing directory, but
this won't always work because of the `#[path]` attributes, which can associate
`foo.rs` with arbitrary mod declaration.

The working brute force solution is to go through all the mod declarations in
the project and find the one that points to `foo.rs`, either implicitly or via
the `path` attribute. To make this solution faster, we need to employ the index.

The first attempt at indexing might look like this: "let's associate each mod
declaration with the file it refers to". This doesn't quite work because it
violates the prime contract of indexes: you can only use one file. If you
actually implement this, you'll see stale information in the index after you
edit some files.

The current implementation uses the following trick. When indexing `mod foo;`,
we associated the declaration with the potential name of the file. In this case,
it would be `"foo"`, for `#[path="bar/baz.rs"]` it would be `baz`. Then, when we
want to find the parent of the `foo.rs` or `foo/mod.rs` file, we query the index
for all mod decls with the `foo` key. This may give us some false positives, if
there are several `foo` modules in the different parts of the project, but it
will definitely include the correct answer. To find the true mod decl, we then
resolve each candidate and check if it indeed points to our file.

Read more about [indexing].

[indexing]: https://www.jetbrains.org/intellij/sdk/docs/basics/indexing_and_psi_stubs.html
[stub-switch]: https://github.com/intellij-rust/intellij-rust/blob/1cc9e40248bd36e43cc016d008270d0e0f4d7f8a/src/main/kotlin/org/rust/lang/core/psi/impl/RustStubbedNamedElementImpl.kt#L27


# Project model

Each Rust project in IDE consists of multiple [CargoProject]s. 

```
              [CargoProject]
                    |
            [CargoWorkspace]
              /          \
         [Package]   [Package]
           /   \         |
    [Target] [Target] [Target]
   (main.rs) (lib.rs) (lib.rs)
```

## CargoProject

Basically, [CargoProject] corresponds to a `Cargo.toml` file inside a project
source folder that is linked to the current IDE project via [attachCargoProject].
Each valid [CargoProject] contains exactly one [CargoWorkspace] (see
[CargoProject.workspace]). A workspace may be null if project is not valid
(project is in updating state, Cargo is not installed, broken `Cargo.toml`, etc).

## CargoWorkspace

[CargoWorkspace] is attached to each valid [CargoProject] and stores info
about its packages (see [CargoWorkspace.packages]). A workspace is acquired
from Cargo itself via `cargo metadata` command.

## Package

[CargoWorkspace.Package] is a thing that can be considered as a dependency (has
a name and version) and can have dependencies. A package may contain one library
target and/or multiple targets of other types. Package is described by `[package]`
section of `Cargo.toml`

## Target

[CargoWorkspace.Target] is a thing that can be compiled to some binary artifact,
e.g. executable binary, library, example library/executable, test executable, etc.
Each target has crate root (see [CargoWorkspace.Target.crateRoot]). For example,
`main.rs` or `lib.rs`.

## Project model FAQ

Q: What's the difference between [CargoProject] and [CargoWorkspace]?

A: They are mostly the same, i.e. they are linked to each other. The most
  difference is that [CargoWorkspace] is acquired from external tool,
  and so may be `null` sometimes, while [CargoProject] is always persisted.

Q: How does [CargoWorkspace] relate to `[workspace]` section in `Cargo.toml`?

A: Each Cargo project has a workspace even if there is no `[workspace]` in
  the `Cargo.toml`. With `[workspace]` the [CargoWorkspace] contains all
  `[workspace.members]` packages.

Q: What's the difference between [CargoWorkspace.Package] and [CargoWorkspace.Target]?

A: [CargoWorkspace.Package] may contain multiple [CargoWorkspace.Target]s.
  E.g. a package may contain common library target, multiple binary
  targets that use it, test targets, benchmark targets, etc.

Q: What is `Cargo.toml` dependency in the terms of this model?

A: Dependency (that is `foo = "1.0"` in `Cargo.toml`) is a
  [CargoWorkspace.Package] (of specified name and version) with one
  _library_ target. See [CargoWorkspace.Package.dependencies]

Q: What is a Rust crate in the terms of this model?

A: It is always [CargoWorkspace.Target]. In the case of `extern crate foo;`
  it is a library [CargoWorkspace.Target] (with a name `foo`) of some
  dependency package of the current package.

Q: What's the difference between [CargoWorkspace.Package.name] and [CargoWorkspace.Target.name]?

A: [CargoWorkspace.Package.name] is a name of a dependency that should be mentioned in
  `[dependencies]` section of `Cargo.toml`. [CargoWorkspace.Target.name] is a name
  that visible in the Rust code, e.g. in `extern crate` syntax. Usually they are equal.
  A name of a package can be specified by `[package.name]` property in `Cargo.toml`.
  A name of a target can be specified in sections like `[lib]`, `[[bin]]`, etc.
  Also, name of a dependency target can be changed.
  Note that if a name of a target appears in the rust code, all `-` symbols are replaced with `_`.
  To get such replaced names, use [CargoWorkspace.Target.normName] or [CargoWorkspace.Package.normName].
  See [reference](https://doc.rust-lang.org/cargo/reference/specifying-dependencies.html#renaming-dependencies-in-cargotoml)

See Cargo's [workspace.rs], [package.rs] and [manifest.rs].

[attachCargoProject]:                  https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/model/CargoProjectService.kt#L45
[CargoProject]:                        https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/model/CargoProjectService.kt#L90
[CargoProject.workspace]:              https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/model/CargoProjectService.kt#L97
[CargoWorkspace]:                      https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L28
[CargoWorkspace.packages]:             https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L36
[CargoWorkspace.Package]:              https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L51
[CargoWorkspace.Package.dependencies]: https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L66
[CargoWorkspace.Package.name]:         https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L55
[CargoWorkspace.Package.normName]:     https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L56
[CargoWorkspace.Target]:               https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L78
[CargoWorkspace.Target.name]:          https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L79
[CargoWorkspace.Target.normName]:      https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L82
[CargoWorkspace.Target.crateRoot]:     https://github.com/intellij-rust/intellij-rust/blob/1a8cc0ee1/src/main/kotlin/org/rust/cargo/project/workspace/CargoWorkspace.kt#L95
[workspace.rs]: https://github.com/rust-lang/cargo/blob/d0f82841/src/cargo/core/workspace.rs
[package.rs]: https://github.com/rust-lang/cargo/blob/d0f82841/src/cargo/core/package.rs
[manifest.rs]: https://github.com/rust-lang/cargo/blob/d0f82841/src/cargo/core/manifest.rs#L228


