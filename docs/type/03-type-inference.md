# 类型推导与约束求解

本文档描述仓颉语言编译器中的类型推导机制，包括约束生成、约束求解算法和 Unify 操作。

## 1. 类型推导概述

仓颉编译器使用**基于约束的类型推导**，主要用于：

1. **泛型函数调用**：推导类型参数
2. **Lambda 表达式**：推导参数和返回类型
3. **变量声明**：从初始化表达式推导类型
4. **字面量类型**：将理想类型解析为具体类型

## 2. 约束模型

### 2.1 类型变量边界

每个类型变量维护以下约束信息：

```cpp
struct TyVarBounds {
    PSet<Ptr<Ty>> lbs;           // 下界集合 (Lower Bounds)
    PSet<Ptr<Ty>> ubs;           // 上界集合 (Upper Bounds)
    PSet<Ptr<Ty>> sum;           // 和类型选项 (必须选择其一)
    PSet<Ptr<Ty>> eq;            // 相等约束 (0 或 1 个元素)

    // 归责追踪 (用于错误信息)
    map<Ptr<Ty>, BlameInfo> lb2Blames;
    map<Ptr<Ty>, BlameInfo> ub2Blames;
};
```

### 2.2 约束类型

| 约束类型 | 表示 | 含义 |
|---------|------|------|
| 下界约束 | `T ∈ lbs` | 类型变量必须是 T 的父类型 |
| 上界约束 | `T ∈ ubs` | 类型变量必须是 T 的子类型 |
| 和约束 | `T ∈ sum` | 类型变量必须是 sum 中某一类型 |
| 相等约束 | `T ∈ eq` | 类型变量必须等于 T |

### 2.3 约束表示

```cpp
// 完整约束集合
using Constraint = map<Ptr<GenericsTy>, TyVarBounds>;

// 归责样式
enum BlameStyle {
    ARGUMENT,       // 来自函数参数
    RETURN,         // 来自返回类型
    CONSTRAINT      // 来自泛型约束
};

struct BlameInfo {
    BlameStyle style;
    SourceLocation loc;
    string message;
};
```

## 3. Unify 操作

Unify 是约束生成的核心操作，通过匹配参数类型与实参类型生成约束。

### 3.1 Unify 入口

```cpp
class LocalTypeArgumentSynthesis {
public:
    // 主入口：推导类型参数
    optional<TypeSubst> SynthesizeTypeArguments(
        const vector<Ptr<Ty>>& tyVarsToSolve,  // 待求解的类型变量
        const vector<Ptr<Ty>>& argTys,          // 实参类型
        const vector<Ptr<Ty>>& paramTys,        // 形参类型
        const Ptr<Ty>& funcRetTy,               // 函数返回类型
        const Ptr<Ty>& retTyUB                  // 期望的返回类型上界
    );

private:
    Constraint constraint;  // 当前约束集合

    // Unify 操作
    bool Unify(const Ptr<Ty>& argTy, const Ptr<Ty>& paramTy, Position pos);
    bool UnifyOne(const Ptr<Ty>& argTy, const Ptr<Ty>& paramTy, Position pos);
    bool UnifyTyVar(const Ptr<Ty>& ty, const Ptr<GenericsTy>& tyVar, Position pos);
};
```

### 3.2 Unify 算法

```cpp
bool LocalTypeArgumentSynthesis::Unify(
    const Ptr<Ty>& argTy,
    const Ptr<Ty>& paramTy,
    Position pos
) {
    // 位置决定约束方向
    // COVARIANT: argTy <: paramTy (输出位置)
    // CONTRAVARIANT: paramTy <: argTy (输入位置)

    // 1. 类型相等 - 无需约束
    if (IsTyEqual(argTy, paramTy)) {
        return true;
    }

    // 2. 参数是类型变量
    if (paramTy->IsGeneric()) {
        return UnifyTyVar(argTy, paramTy->AsGenericsTy(), pos);
    }

    // 3. 实参是类型变量
    if (argTy->IsGeneric()) {
        return UnifyTyVar(paramTy, argTy->AsGenericsTy(), FlipPosition(pos));
    }

    // 4. 分派到具体类型的 Unify
    return UnifyOne(argTy, paramTy, pos);
}
```

### 3.3 类型变量 Unify

```cpp
bool LocalTypeArgumentSynthesis::UnifyTyVar(
    const Ptr<Ty>& ty,
    const Ptr<GenericsTy>& tyVar,
    Position pos
) {
    // 检查类型变量是否在待求解集合中
    if (!tyVarsToSolve.contains(tyVar)) {
        // 不在求解集合中，检查上界约束
        for (auto& ub : tyVar->upperBounds) {
            if (!Unify(ty, ub, pos)) {
                return false;
            }
        }
        return true;
    }

    auto& bounds = constraint[tyVar];

    if (pos == COVARIANT) {
        // ty <: tyVar，ty 是下界
        bounds.lbs.insert(ty);
        bounds.lb2Blames[ty] = currentBlame;
    } else {
        // tyVar <: ty，ty 是上界
        bounds.ubs.insert(ty);
        bounds.ub2Blames[ty] = currentBlame;
    }

    return true;
}
```

### 3.4 函数类型 Unify

```cpp
bool LocalTypeArgumentSynthesis::UnifyFunc(
    const Ptr<FuncTy>& argFunc,
    const Ptr<FuncTy>& paramFunc,
    Position pos
) {
    // 参数数量必须匹配
    if (argFunc->paramTys.size() != paramFunc->paramTys.size()) {
        return false;
    }

    // 参数类型：逆变位置
    for (size_t i = 0; i < argFunc->paramTys.size(); i++) {
        if (!Unify(argFunc->paramTys[i], paramFunc->paramTys[i],
                   FlipPosition(pos))) {
            return false;
        }
    }

    // 返回类型：协变位置
    return Unify(argFunc->retTy, paramFunc->retTy, pos);
}
```

### 3.5 复合类型 Unify

```cpp
bool LocalTypeArgumentSynthesis::UnifyOne(
    const Ptr<Ty>& argTy,
    const Ptr<Ty>& paramTy,
    Position pos
) {
    // 必须是同一类型构造器
    if (!OfSameCtor(argTy, paramTy)) {
        return false;
    }

    switch (paramTy->kind) {
        case TYPE_FUNC:
            return UnifyFunc(argTy, paramTy, pos);

        case TYPE_TUPLE:
            // 逐元素 Unify
            for (size_t i = 0; i < paramTy->elementTys.size(); i++) {
                if (!Unify(argTy->elementTys[i], paramTy->elementTys[i], pos)) {
                    return false;
                }
            }
            return true;

        case TYPE_CLASS:
        case TYPE_STRUCT:
        case TYPE_INTERFACE:
            // 类型参数 Unify（不变位置）
            for (size_t i = 0; i < paramTy->typeArgs.size(); i++) {
                if (!Unify(argTy->typeArgs[i], paramTy->typeArgs[i], INVARIANT)) {
                    return false;
                }
            }
            return true;

        case TYPE_ARRAY:
        case TYPE_VARRAY:
            // 元素类型不变
            return Unify(argTy->elementTy, paramTy->elementTy, INVARIANT);

        default:
            // 原始类型已在入口检查相等性
            return true;
    }
}
```

## 4. 约束求解

### 4.1 类型变量约束图

使用 `TyVarConstraintGraph` 管理类型变量之间的依赖关系：

```cpp
class TyVarConstraintGraph {
private:
    map<Ptr<GenericsTy>, int> indegree;              // 入度
    map<Ptr<GenericsTy>, set<Ptr<GenericsTy>>> edges; // 依赖边
    map<Ptr<GenericsTy>, bool> isVisited;            // 已处理标记
    set<Ptr<GenericsTy>> solvedTyVars;               // 已求解变量
    set<Ptr<GenericsTy>> usedTyVars;                 // 使用中的变量

public:
    // 预处理：构建约束图
    void PreProcessConstraintGraph(const Constraint& constraint);

    // 拓扑排序：返回入度为 0 的变量
    set<Ptr<GenericsTy>> TopoOnce();

    // 更新图：应用类型替换后更新
    void ApplyTypeSubst(const TypeSubst& subst);

    // 循环检测
    bool HasLoop();
    set<Ptr<GenericsTy>> FindLoopConstraints();
};
```

### 4.2 构建约束图

```cpp
void TyVarConstraintGraph::PreProcessConstraintGraph(const Constraint& constraint) {
    // 1. 初始化所有类型变量
    for (auto& [tyVar, bounds] : constraint) {
        indegree[tyVar] = 0;
        edges[tyVar] = {};
    }

    // 2. 建立依赖边
    for (auto& [tyVar, bounds] : constraint) {
        // 收集边界中出现的其他类型变量
        set<Ptr<GenericsTy>> deps;

        for (auto& lb : bounds.lbs) {
            CollectTyVars(lb, deps);
        }
        for (auto& ub : bounds.ubs) {
            CollectTyVars(ub, deps);
        }
        for (auto& s : bounds.sum) {
            CollectTyVars(s, deps);
        }

        // tyVar 依赖于 deps 中的变量
        for (auto& dep : deps) {
            if (constraint.count(dep) && dep != tyVar) {
                edges[dep].insert(tyVar);
                indegree[tyVar]++;
            }
        }
    }
}
```

### 4.3 拓扑排序求解

```cpp
set<Ptr<GenericsTy>> TyVarConstraintGraph::TopoOnce() {
    set<Ptr<GenericsTy>> result;

    for (auto& [tyVar, degree] : indegree) {
        if (degree == 0 && !isVisited[tyVar]) {
            result.insert(tyVar);
            isVisited[tyVar] = true;
        }
    }

    // 更新后续变量的入度
    for (auto& tyVar : result) {
        for (auto& successor : edges[tyVar]) {
            indegree[successor]--;
        }
        solvedTyVars.insert(tyVar);
    }

    return result;
}
```

### 4.4 约束求解算法

```cpp
optional<TypeSubst> LocalTypeArgumentSynthesis::SolveConstraints() {
    TypeSubst result;
    TyVarConstraintGraph graph;
    graph.PreProcessConstraintGraph(constraint);

    while (true) {
        // 1. 获取可求解的变量（入度为 0）
        auto solvable = graph.TopoOnce();
        if (solvable.empty()) {
            break;
        }

        // 2. 逐个求解
        for (auto& tyVar : solvable) {
            auto solution = SolveSingleTyVar(tyVar);
            if (!solution) {
                return nullopt;  // 求解失败
            }

            result[tyVar] = *solution;

            // 3. 将解代入其他约束
            SubstituteInConstraint(*solution, tyVar);
        }

        // 4. 更新约束图
        graph.ApplyTypeSubst(result);
    }

    // 5. 检查是否所有变量都已求解
    if (!AllTyVarsSolved()) {
        // 可能存在循环依赖
        if (graph.HasLoop()) {
            return HandleLoopConstraints(graph);
        }
        return nullopt;
    }

    return result;
}
```

### 4.5 单个类型变量求解

```cpp
optional<Ptr<Ty>> LocalTypeArgumentSynthesis::SolveSingleTyVar(
    const Ptr<GenericsTy>& tyVar
) {
    auto& bounds = constraint[tyVar];

    // 1. 有相等约束 - 直接使用
    if (!bounds.eq.empty()) {
        return *bounds.eq.begin();
    }

    // 2. 有和约束 - 选择最佳选项
    if (!bounds.sum.empty()) {
        return FindBestFromSum(tyVar, bounds);
    }

    // 3. 计算边界的交集
    Ptr<Ty> lowerBound = ComputeJoin(bounds.lbs);  // 下界的最小上界
    Ptr<Ty> upperBound = ComputeMeet(bounds.ubs);  // 上界的最大下界

    // 4. 验证边界兼容性
    if (!IsSubtype(lowerBound, upperBound, false)) {
        // 下界不是上界的子类型 - 冲突
        ReportConflictingBounds(tyVar, bounds);
        return nullopt;
    }

    // 5. 选择解
    // 优先使用下界（更具体的类型）
    if (lowerBound && !lowerBound->IsNothing()) {
        return lowerBound;
    }

    // 否则使用上界
    if (upperBound && !upperBound->IsAny()) {
        return upperBound;
    }

    // 无约束 - 使用默认类型或报错
    return HandleUnconstrainedTyVar(tyVar);
}
```

## 5. 约束传播

当求解一个类型变量后，需要将结果传播到其他约束：

```cpp
void LocalTypeArgumentSynthesis::SubstituteInConstraint(
    const Ptr<Ty>& solution,
    const Ptr<GenericsTy>& tyVar
) {
    for (auto& [otherTyVar, bounds] : constraint) {
        if (otherTyVar == tyVar) continue;

        // 替换下界中的类型变量
        PSet<Ptr<Ty>> newLbs;
        for (auto& lb : bounds.lbs) {
            newLbs.insert(SubstTyVar(lb, tyVar, solution));
        }
        bounds.lbs = newLbs;

        // 替换上界
        PSet<Ptr<Ty>> newUbs;
        for (auto& ub : bounds.ubs) {
            newUbs.insert(SubstTyVar(ub, tyVar, solution));
        }
        bounds.ubs = newUbs;

        // 替换和约束
        PSet<Ptr<Ty>> newSum;
        for (auto& s : bounds.sum) {
            newSum.insert(SubstTyVar(s, tyVar, solution));
        }
        bounds.sum = newSum;
    }
}
```

## 6. 循环约束处理

当类型变量之间存在循环依赖时：

```cpp
optional<TypeSubst> LocalTypeArgumentSynthesis::HandleLoopConstraints(
    TyVarConstraintGraph& graph
) {
    // 1. 找到所有循环中的变量
    auto loopVars = graph.FindLoopConstraints();

    // 2. 尝试同时求解循环变量
    // 使用迭代方法
    TypeSubst tentative;
    for (auto& tyVar : loopVars) {
        // 初始猜测：使用上界
        auto& bounds = constraint[tyVar];
        if (!bounds.ubs.empty()) {
            tentative[tyVar] = *bounds.ubs.begin();
        } else {
            tentative[tyVar] = MakeAnyTy();
        }
    }

    // 3. 迭代精化
    for (int i = 0; i < MAX_ITERATIONS; i++) {
        TypeSubst refined;
        bool changed = false;

        for (auto& tyVar : loopVars) {
            auto newSolution = RefineWithSubst(tyVar, tentative);
            refined[tyVar] = newSolution;
            if (!IsTyEqual(newSolution, tentative[tyVar])) {
                changed = true;
            }
        }

        if (!changed) {
            return refined;  // 达到不动点
        }
        tentative = refined;
    }

    // 未能收敛
    return nullopt;
}
```

## 7. 解的选择策略

当存在多个可能解时，使用以下策略选择最佳解：

```cpp
Ptr<Ty> LocalTypeArgumentSynthesis::FindBestFromSum(
    const Ptr<GenericsTy>& tyVar,
    const TyVarBounds& bounds
) {
    vector<Ptr<Ty>> candidates;

    for (auto& option : bounds.sum) {
        // 检查选项是否满足所有边界
        bool valid = true;

        // 检查下界
        for (auto& lb : bounds.lbs) {
            if (!IsSubtype(lb, option, false)) {
                valid = false;
                break;
            }
        }

        // 检查上界
        if (valid) {
            for (auto& ub : bounds.ubs) {
                if (!IsSubtype(option, ub, false)) {
                    valid = false;
                    break;
                }
            }
        }

        if (valid) {
            candidates.push_back(option);
        }
    }

    if (candidates.empty()) {
        return nullptr;
    }

    // 按优先级排序
    sort(candidates.begin(), candidates.end(), [](auto& a, auto& b) {
        // 1. 非理想类型优先
        if (a->IsIdealType() != b->IsIdealType()) {
            return !a->IsIdealType();
        }

        // 2. 更具体的类型优先
        if (IsSubtype(a, b, false) && !IsTyEqual(a, b)) {
            return true;
        }

        // 3. 按类型复杂度排序
        return GetTypeComplexity(a) < GetTypeComplexity(b);
    });

    return candidates[0];
}
```

## 8. 错误诊断

### 8.1 错误类型

```cpp
enum SolvingErrStyle {
    NO_CONSTRAINT,           // 类型变量无约束
    CONFLICTING_CONSTRAINTS, // 下界与上界冲突
    ARG_MISMATCH,           // 实参与形参不匹配
    RET_MISMATCH            // 返回类型不匹配
};
```

### 8.2 错误信息生成

```cpp
void LocalTypeArgumentSynthesis::ReportConflictingBounds(
    const Ptr<GenericsTy>& tyVar,
    const TyVarBounds& bounds
) {
    stringstream ss;
    ss << "Type parameter '" << tyVar->name << "' has conflicting constraints:\n";

    // 列出下界
    ss << "  Lower bounds (must be supertype of):\n";
    for (auto& lb : bounds.lbs) {
        auto& blame = bounds.lb2Blames[lb];
        ss << "    - " << TypeToString(lb);
        ss << " (from " << blame.loc << ")\n";
    }

    // 列出上界
    ss << "  Upper bounds (must be subtype of):\n";
    for (auto& ub : bounds.ubs) {
        auto& blame = bounds.ub2Blames[ub];
        ss << "    - " << TypeToString(ub);
        ss << " (from " << blame.loc << ")\n";
    }

    ReportError(ss.str());
}
```

## 9. 完整求解流程

```
类型参数推导流程
│
├─ 1. 收集待求解的类型变量
│     └─ 从函数签名中提取泛型参数
│
├─ 2. Unify 阶段
│     ├─ 对每个 (实参, 形参) 对执行 Unify
│     ├─ 生成类型变量的边界约束
│     └─ 如果有返回类型期望，Unify 返回类型
│
├─ 3. 构建约束图
│     ├─ 识别类型变量之间的依赖
│     └─ 计算拓扑排序顺序
│
├─ 4. 迭代求解
│     ├─ 选择入度为 0 的变量
│     ├─ 求解变量
│     ├─ 传播解到其他约束
│     └─ 更新约束图
│
├─ 5. 处理循环依赖
│     ├─ 检测循环
│     └─ 迭代精化求解
│
└─ 6. 验证和返回
      ├─ 检查所有变量已求解
      ├─ 验证解满足原始约束
      └─ 返回类型替换映射
```

## 10. 示例

### 示例 1：简单类型推导

```cangjie
func identity<T>(x: T): T { x }

let result = identity(42)  // 推导 T = Int64
```

推导过程：
1. 待求解：`T`
2. Unify：`42: IdealInt` 与 `x: T`
   - 生成约束：`T ∈ lbs = {IdealInt}`
3. 求解：`T = Int64`（理想类型解析为默认类型）

### 示例 2：多约束推导

```cangjie
func combine<T>(a: T, b: T): T { ... }

let r = combine("hello", "world")  // 推导 T = String
```

推导过程：
1. 待求解：`T`
2. Unify `"hello": String` 与 `a: T`
   - 约束：`T ∈ lbs = {String}`
3. Unify `"world": String` 与 `b: T`
   - 约束：`T ∈ lbs = {String, String}` (去重后 `{String}`)
4. 求解：`T = String`

### 示例 3：复杂类型推导

```cangjie
func map<T, U>(arr: Array<T>, f: (T) -> U): Array<U> { ... }

let nums = [1, 2, 3]
let strs = map(nums, |x| x.toString())  // 推导 T = Int64, U = String
```

推导过程：
1. 待求解：`T`, `U`
2. Unify `nums: Array<Int64>` 与 `arr: Array<T>`
   - 约束：`T ∈ lbs = {Int64}`
3. Unify `lambda: (Int64) -> String` 与 `f: (T) -> U`
   - 参数逆变：Unify `T` 与 `Int64`（已满足）
   - 返回协变：Unify `String` 与 `U`
   - 约束：`U ∈ lbs = {String}`
4. 求解：`T = Int64`, `U = String`
