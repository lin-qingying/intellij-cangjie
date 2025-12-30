# Stub 列表与 Node 列表差异分析报告

## 概述

在 CangJie 语言 PSI 实现中，发现 Stub 列表（4671 个元素）与 Node 列表（3666 个元素）存在 1005 个元素的差异。本文档分析产生这种差异的原因。

## 差异统计

| 列表类型 | 元素数量 |
|---------|---------|
| Stub 列表 | 4671 |
| Node 列表 | 3666 |
| **差异** | **1005** |

## 关键差异点

### 1. Stub 中存在但 Node 中不存在的类型

| 元素类型 | 说明 | 来源 |
|---------|------|------|
| `PROPERTY` | 属性声明 | `PropertyClsStubBuilder` |
| `PROPERTY_ACCESSOR` | 属性访问器（getter/setter） | `PropertyClsStubBuilder` |
| `INTERFACE_BODY` | 接口体 | `ClassClsStubBuilder` |

### 2. Node 中存在但 Stub 中不存在的类型（已修复）

| 元素类型 | 说明 | 修复状态 |
|---------|------|---------|
| `ANNOTATIONS` | 注解列表 | ✅ 已修复 |

## 根本原因分析

### 1. 属性处理差异

**源码位置**: `analysis/decompiler-to-psi/src/main/kotlin/org/cangnova/cangjie/decompiler/stub/CallableClsStubBuilder.kt:586-626`

反编译器中的 `PropertyClsStubBuilder` 会为每个属性创建：
- 1 个 `PROPERTY` Stub
- 1 个 `PROPERTY_ACCESSOR` Stub（如果有 getter）
- 1 个 `PROPERTY_ACCESSOR` Stub（如果有 setter）

### 2. 接口体类型不一致

**源码位置**: `analysis/decompiler-to-psi/src/main/kotlin/org/cangnova/cangjie/decompiler/stub/ClassClsStubBuilder.kt:359-372`

反编译器根据类的类型创建不同的类体 Stub：
- `ClassKind.INTERFACE` → `INTERFACE_BODY`
- `ClassKind.ENUM` → `ENUM_BODY`
- 其他 → `CLASS_BODY`

### 3. 注解处理（已修复）

**修复说明**: 反编译器现在会为声明创建 `ANNOTATIONS` Stub，保持与源码 PSI 结构的一致性。

**PSI 结构**:
```
CjClass (或其他声明)
  ├─ CjAnnotations  <-- 注解在修饰符列表之前
  │   └─ CjAnnotation
  ├─ CjModifierList
  └─ ... 其他子元素
```

**修改的文件**:
- `clsStubBuilding.kt`: 添加 `createAnnotationsStub()` 函数
- `ClassClsStubBuilder.kt`: 在类声明中添加注解 Stub 创建
- `CallableClsStubBuilder.kt`: 在函数、宏、Main 函数声明中添加注解 Stub 创建

## 两个相关包的职责

### `org.cangnova.cangjie.psi.stubs`

负责定义 Stub 元素类型和源码 Stub 构建：

- `CjStubElementTypes` - 定义所有 Stub 元素类型
- `CjFileStubBuilder` - 从源文件构建 Stub

### `org.cangnova.cangjie.decompiler.stub`

负责从编译后的元数据（.cjo 文件）构建 Stub：

- `ClassClsStubBuilder` - 类/接口/结构体/枚举 Stub 构建
- `CallableClsStubBuilder` - 函数/属性/变量 Stub 构建
- `clsStubBuilding.kt` - 通用 Stub 构建工具函数

## 修复记录

### 2025-12-23: 添加 ANNOTATIONS Stub 支持

**问题**: 反编译器不生成注解 Stub，导致 Stub 和 Node 结构不一致

**解决方案**:
1. 在 `clsStubBuilding.kt` 中添加 `createAnnotationsStub()` 函数
2. 在各个声明构建器中，于修饰符列表创建**之前**调用 `createAnnotationsStub()`
3. 注解信息从元数据的 `AnnotationWrapper` 获取

**关键代码**:
```kotlin
// 先创建注解 Stub（注解在修饰符列表之前）
createAnnotationsStub(classStub, classDecl.annotations)

createModifierListStubForDeclaration(
    classStub,
    classDecl.visibility,
    classDecl.modality
)
```

## 相关文件清单

| 文件路径 | 职责 |
|---------|------|
| `psi/src/main/kotlin/.../stubs/elements/CjStubElementTypes.java` | Stub 元素类型定义 |
| `psi/src/main/kotlin/.../stubs/elements/CjFileStubBuilder.kt` | 源码 Stub 构建 |
| `analysis/decompiler-to-psi/.../stub/ClassClsStubBuilder.kt` | 类 Stub 构建 |
| `analysis/decompiler-to-psi/.../stub/CallableClsStubBuilder.kt` | 函数/属性 Stub 构建 |
| `analysis/decompiler-to-psi/.../stub/clsStubBuilding.kt` | 通用 Stub 工具 |
| `metadata/.../wrapper/packageFormat.kt` | 元数据包装器（含 AnnotationWrapper） |

---

*分析日期: 2025-12-23*
*最后更新: 2025-12-23（添加 ANNOTATIONS 支持）*