# 仓颉语言编译器类型系统文档

本目录包含仓颉语言编译器类型系统的完整实现细节文档。

## 文档结构

| 文件 | 描述 |
|------|------|
| [01-type-hierarchy.md](01-type-hierarchy.md) | 类型层次结构 - TypeKind、Type 类层次、AST 类型定义 |
| [02-subtype-rules.md](02-subtype-rules.md) | 子类型检查规则 - IsSubtype 算法、型变规则、特殊类型处理 |
| [03-type-inference.md](03-type-inference.md) | 类型推导与约束求解 - 约束生成、求解算法、Unify 操作 |
| [04-generic-instantiation.md](04-generic-instantiation.md) | 泛型类型实例化 - 实例化管理、替换策略、缓存机制 |
| [05-join-and-meet.md](05-join-and-meet.md) | Join 与 Meet 操作 - 最小上界、最大下界计算 |
| [06-special-types.md](06-special-types.md) | 特殊类型处理 - Nothing、Any、Option、扩展类型 |
| [07-implementation-details.md](07-implementation-details.md) | **补充细节** - 数值转换、Ideal Types、约束分支、缓存机制、错误传播 |

## 核心源文件参考

编译器类型系统的实现分布在以下关键文件中：

### 头文件 (include/cangjie/)
- `CHIR/Type/Type.h` - CHIR 层类型定义
- `AST/Types.h` - AST 层类型定义
- `Sema/TypeManager.h` - 类型管理器接口
- `Sema/JoinAndMeet.h` - Join/Meet 操作接口
- `Sema/TyVarConstraintGraph.h` - 类型变量约束图
- `Sema/CommonTypeAlias.h` - 类型别名和约束模型定义

### 实现文件 (src/Sema/)
- `TypeManager.cpp` - 类型管理器核心实现 (2300+ 行)
- `JoinAndMeet.cpp` - Join/Meet 操作实现 (400+ 行)
- `LocalTypeArgumentSynthesis.cpp` - 类型参数推导 (1200+ 行)
- `GenericInstantiationManager.cpp` - 泛型实例化管理

## 类型系统概览

### 核心设计原则

1. **多层替换 (Multi-level Substitution)**
   - 使用 `SubstPack` 结构处理嵌套泛型上下文
   - `u2i`: 通用类型变量 → 实例类型变量
   - `inst`: 实例类型变量 → 具体类型参数

2. **约束图分析 (Constraint Graph Analysis)**
   - 使用拓扑排序确定最优求解顺序
   - 优先求解最独立的类型变量

3. **归责追踪 (Blame Attribution)**
   - 跟踪约束来源位置以提供精确错误信息
   - 支持 ARGUMENT、RETURN、CONSTRAINT 三种归责样式

4. **回溯机制 (Backtracking via Commits)**
   - 通过检查点/恢复机制探索多个解决方案
   - `PData::CommitScope` 创建状态检查点

5. **型变规则 (Variance Rules)**
   - 输出位置协变 (covariant)
   - 输入位置逆变 (contravariant)
   - 用户自定义泛型参数不变 (invariant)

6. **扩展子类型 (Extension-based Subtyping)**
   - 通过显式 extend 声明建立名义类型的子类型关系
   - 支持条件扩展和装箱类型扩展

## 快速参考

### 子类型判断流程
```
IsSubtype(Leaf, Root)?
├─ 占位类型变量 → 约束收集
├─ 泛型类型 → 递归替换检查
├─ 类/接口 → 继承层次 + 扩展
├─ 结构体/枚举 → 名义直接子类型
├─ 数组/指针 → 元素类型型变
├─ 原始类型 → 数值提升规则
├─ 元组 → 逐元素型变
└─ 函数 → 参数逆变, 返回值协变
```

### 约束求解流水线
```
1. 初始化: 为每个类型变量创建约束
2. 统一化: 对每个参数/实参对生成子类型约束
3. 拓扑排序: 识别最独立的变量
4. 贪心选择: 为每个变量找到满足所有边界的类型
5. 传播: 应用选定类型，对剩余变量重复
6. 验证: 检查无未解决的类型变量
```

## 关键实现陷阱

在实现类型系统时需要特别注意以下问题：

1. **Ideal Types 的完整处理** - 字面量类型在约束求解中的特殊行为
2. **约束分支爆炸** - Union 类型和重载解析可能导致分支爆炸
3. **循环类型与缓存** - 占位符类型不能缓存，递归类型需要保护
4. **方差规则的一致性** - 函数参数逆变、返回值协变
5. **多元 Ideal Type** - 在 Join/Meet 中的精确处理

详见 [07-implementation-details.md](07-implementation-details.md)。

## 版本信息

- 文档版本: 1.1
- 基于编译器版本: 仓颉编译器 (2025)
- 最后更新: 2025-01
