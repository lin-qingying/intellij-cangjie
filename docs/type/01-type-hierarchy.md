# 类型层次结构

本文档描述仓颉语言编译器中的类型层次结构，包括 CHIR 层和 AST 层的类型定义。

## 1. TypeKind 枚举

编译器使用 `TypeKind` 枚举定义所有类型种类：

```cpp
enum TypeKind : uint8_t {
    // 无效类型
    TYPE_INVALID = 0,

    // 有符号整数类型
    TYPE_INT8,
    TYPE_INT16,
    TYPE_INT32,
    TYPE_INT64,
    TYPE_INT_NATIVE,      // 平台相关的 IntNative

    // 无符号整数类型
    TYPE_UINT8,
    TYPE_UINT16,
    TYPE_UINT32,
    TYPE_UINT64,
    TYPE_UINT_NATIVE,     // 平台相关的 UIntNative

    // 浮点类型
    TYPE_FLOAT16,
    TYPE_FLOAT32,
    TYPE_FLOAT64,

    // 其他原始类型
    TYPE_RUNE,            // Unicode 字符
    TYPE_BOOLEAN,         // 布尔类型
    TYPE_UNIT,            // 单元类型 (类似 void)
    TYPE_NOTHING,         // 底类型 (永不返回)
    TYPE_VOID,            // C 互操作的 void

    // 复合类型
    TYPE_TUPLE,           // 元组类型
    TYPE_STRUCT,          // 结构体类型
    TYPE_ENUM,            // 枚举类型
    TYPE_FUNC,            // 函数类型
    TYPE_CLASS,           // 类类型

    // 数组和指针类型
    TYPE_RAWARRAY,        // 原始数组
    TYPE_VARRAY,          // 可变长数组
    TYPE_CPOINTER,        // C 指针
    TYPE_CSTRING,         // C 字符串

    // 泛型和特殊类型
    TYPE_GENERIC,         // 泛型类型参数
    TYPE_REFTYPE,         // 引用类型
    TYPE_BOXTYPE,         // 装箱类型
    TYPE_THIS,            // this 类型
};
```

## 2. CHIR 层类型类层次

CHIR (Cangjie High-level IR) 层的类型定义在 `include/cangjie/CHIR/Type/Type.h` 中：

```
Type (基类)
├── BuiltinType (内置类型)
│   ├── NumericType (数值类型)
│   │   ├── IntType (整数类型)
│   │   │   ├── Int8Type
│   │   │   ├── Int16Type
│   │   │   ├── Int32Type
│   │   │   ├── Int64Type
│   │   │   ├── IntNativeType
│   │   │   ├── UInt8Type
│   │   │   ├── UInt16Type
│   │   │   ├── UInt32Type
│   │   │   ├── UInt64Type
│   │   │   └── UIntNativeType
│   │   └── FloatType (浮点类型)
│   │       ├── Float16Type
│   │       ├── Float32Type
│   │       └── Float64Type
│   ├── RuneType (字符类型)
│   ├── BooleanType (布尔类型)
│   ├── UnitType (单元类型)
│   └── NothingType (底类型)
│
├── FuncType (函数类型)
│   ├── paramTypes: vector<Type*>  // 参数类型列表
│   ├── returnType: Type*          // 返回类型
│   └── isVariadic: bool           // 是否可变参数
│
├── CustomType (自定义类型基类)
│   ├── ClassType (类类型)
│   │   ├── declaration: ClassDecl*
│   │   ├── typeArguments: vector<Type*>
│   │   └── superTypes: vector<Type*>
│   │
│   ├── StructType (结构体类型)
│   │   ├── declaration: StructDecl*
│   │   └── typeArguments: vector<Type*>
│   │
│   └── EnumType (枚举类型)
│       ├── declaration: EnumDecl*
│       └── typeArguments: vector<Type*>
│
├── TupleType (元组类型)
│   └── elementTypes: vector<Type*>
│
├── RefType (引用类型)
│   └── referredType: Type*
│
├── BoxType (装箱类型)
│   └── boxedType: Type*
│
├── ThisType (this 类型)
│   └── boundType: Type*
│
├── RawArrayType (原始数组类型)
│   └── elementType: Type*
│
├── VArrayType (可变数组类型)
│   ├── elementType: Type*
│   └── size: optional<int64_t>
│
└── GenericType (泛型类型参数)
    ├── name: string
    ├── index: int
    └── upperBounds: vector<Type*>
```

## 3. AST 层类型结构

AST 层的类型定义在 `include/cangjie/AST/Types.h` 中，使用 `Ty` 结构体：

### 3.1 Ty 基类

```cpp
struct Ty {
    TypeKind kind;           // 类型种类
    bool isConst;            // 是否为常量类型
    bool isResolved;         // 是否已解析
    SourceLocation loc;      // 源码位置

    // 类型判断方法
    bool IsInteger() const;
    bool IsFloating() const;
    bool IsBoolean() const;
    bool IsPrimitive() const;
    bool IsNumeric() const;
    bool IsGeneric() const;
    bool IsFunc() const;
    bool IsTuple() const;
    bool IsClass() const;
    bool IsStruct() const;
    bool IsEnum() const;
    bool IsInterface() const;
    bool IsArray() const;
    bool IsPointer() const;
    bool IsNothing() const;
    bool IsAny() const;
    bool IsUnit() const;
    bool IsOption() const;
    bool IsUnion() const;
    bool IsIntersection() const;
};
```

### 3.2 具体类型结构

#### 原始类型

```cpp
struct PrimitiveTy : Ty {
    // 使用 TypeKind 区分具体的原始类型
    // TYPE_INT8, TYPE_INT16, ..., TYPE_BOOLEAN, TYPE_RUNE, TYPE_UNIT
};

struct NothingTy : Ty {
    // 底类型，是所有类型的子类型
};

struct AnyTy : Ty {
    // 顶类型，所有类型都是它的子类型
};
```

#### 复合类型

```cpp
struct ArrayTy : Ty {
    Ptr<Ty> elementTy;       // 元素类型
};

struct VArrayTy : Ty {
    Ptr<Ty> elementTy;       // 元素类型
    optional<int64_t> size;  // 可选的固定大小
};

struct PointerTy : Ty {
    Ptr<Ty> pointeeTy;       // 指向的类型
    bool isMutable;          // 是否可变
};

struct TupleTy : Ty {
    vector<Ptr<Ty>> elementTys;  // 元素类型列表
};

struct FuncTy : Ty {
    vector<Ptr<Ty>> paramTys;    // 参数类型列表
    Ptr<Ty> retTy;               // 返回类型
    bool isVariadic;             // 是否可变参数
    bool throwsException;        // 是否抛出异常
};
```

#### 类类型

```cpp
struct ClassLikeTy : Ty {
    Ptr<Decl> decl;                    // 关联的声明
    vector<Ptr<Ty>> typeArgs;          // 类型参数
    vector<Ptr<Ty>> superTys;          // 父类型列表
};

struct InterfaceTy : ClassLikeTy {
    // 接口类型
};

struct ClassTy : ClassLikeTy {
    // 类类型
    bool isOpen;                       // 是否为 open class
};

struct StructTy : ClassLikeTy {
    // 结构体类型
};

struct EnumTy : ClassLikeTy {
    // 枚举类型
    vector<Ptr<EnumMember>> members;   // 枚举成员
};
```

#### 泛型类型

```cpp
struct GenericsTy : Ty {
    string name;                       // 类型参数名
    int index;                         // 在参数列表中的索引
    vector<Ptr<Ty>> upperBounds;       // 上界约束
    Ptr<Decl> declaredIn;              // 声明所在的作用域
    bool isReified;                    // 是否具体化
};
```

#### 特殊类型

```cpp
struct UnionTy : Ty {
    vector<Ptr<Ty>> alternatives;      // 联合的类型选项
};

struct IntersectionTy : Ty {
    vector<Ptr<Ty>> components;        // 交集的类型组件
};

struct OptionTy : Ty {
    Ptr<Ty> wrappedTy;                 // 被包装的类型
    // Option<T> 等价于 T | None
};
```

## 4. 类型缓存与去重

TypeManager 使用类型缓存确保相同类型只分配一次：

```cpp
class TypeManager {
private:
    // 类型缓存使用自定义哈希
    unordered_set<TypePointer, TypeHash> typeCache;

    // 原始类型的静态数组 (预分配)
    static array<BuiltinType*, NUM_BUILTIN_TYPES> primitiveTypes;

    // 类型变量池
    vector<Ptr<GenericsTy>> tyVarPool;

public:
    // 获取或创建类型
    Ptr<Ty> GetPrimitiveTy(TypeKind kind);
    Ptr<Ty> GetFuncTy(vector<Ptr<Ty>> params, Ptr<Ty> ret);
    Ptr<Ty> GetTupleTy(vector<Ptr<Ty>> elements);
    Ptr<Ty> GetArrayTy(Ptr<Ty> element);
    Ptr<Ty> GetClassTy(ClassDecl* decl, vector<Ptr<Ty>> typeArgs);
    // ...
};
```

### 类型哈希策略

```cpp
struct TypeHash {
    size_t operator()(const TypePointer& ty) const {
        size_t hash = std::hash<int>()(ty->kind);

        switch (ty->kind) {
            case TYPE_FUNC:
                // 哈希参数类型和返回类型
                for (auto& param : ty->paramTys) {
                    hash = combine(hash, (*this)(param));
                }
                hash = combine(hash, (*this)(ty->retTy));
                break;

            case TYPE_TUPLE:
                // 哈希所有元素类型
                for (auto& elem : ty->elementTys) {
                    hash = combine(hash, (*this)(elem));
                }
                break;

            case TYPE_CLASS:
            case TYPE_STRUCT:
            case TYPE_ENUM:
                // 哈希声明指针和类型参数
                hash = combine(hash, std::hash<void*>()(ty->decl));
                for (auto& arg : ty->typeArgs) {
                    hash = combine(hash, (*this)(arg));
                }
                break;

            // ... 其他类型
        }
        return hash;
    }
};
```

## 5. 类型变量管理

### 5.1 类型变量分配

```cpp
// 分配新的类型变量
Ptr<GenericsTy> TypeManager::AllocTyVar(const string& name) {
    auto tyVar = make_shared<GenericsTy>();
    tyVar->name = name;
    tyVar->index = tyVarPool.size();
    tyVar->scopeDepth = currentScopeDepth;
    tyVarPool.push_back(tyVar);
    return tyVar;
}

// 类型变量作用域管理
class TyVarScope {
public:
    TyVarScope(TypeManager& tm) : tm(tm) {
        tm.EnterTyVarScope();
    }
    ~TyVarScope() {
        tm.ExitTyVarScope();
    }
};
```

### 5.2 类型变量作用域深度

```cpp
// 跟踪每个类型变量的嵌套深度
map<Ptr<GenericsTy>, int> tyVarScopeDepth;

void TypeManager::EnterTyVarScope() {
    currentScopeDepth++;
}

void TypeManager::ExitTyVarScope() {
    // 释放当前作用域的类型变量
    while (!tyVarPool.empty() &&
           tyVarScopeDepth[tyVarPool.back()] == currentScopeDepth) {
        tyVarPool.pop_back();
    }
    currentScopeDepth--;
}
```

## 6. 理想类型 (Ideal Types)

仓颉编译器使用理想类型处理字面量的类型推导：

```cpp
// 理想整数类型 - 字面量 42 的初始类型
struct IdealIntTy : Ty {
    int64_t value;  // 字面量值
};

// 理想浮点类型 - 字面量 3.14 的初始类型
struct IdealFloatTy : Ty {
    double value;   // 字面量值
};
```

### 理想类型的解析

理想类型在类型推导过程中会被解析为具体类型：

```
IdealInt  →  Int64 (默认) 或根据上下文确定
IdealFloat → Float64 (默认) 或根据上下文确定
```

解析规则：
1. 如果有明确的类型注解，使用注解类型
2. 如果有上下文期望类型，使用期望类型
3. 否则使用默认类型 (Int64 / Float64)

## 7. 类型构造器 (Type Constructors)

类型构造器用于判断两个类型是否具有相同的"形状"：

```cpp
bool TypeManager::OfSameCtor(const Ptr<Ty>& ty1, const Ptr<Ty>& ty2) {
    if (ty1->kind != ty2->kind) return false;

    switch (ty1->kind) {
        case TYPE_CLASS:
        case TYPE_STRUCT:
        case TYPE_ENUM:
        case TYPE_INTERFACE:
            // 同一声明的类型具有相同构造器
            return ty1->decl == ty2->decl;

        case TYPE_FUNC:
            // 参数数量相同的函数具有相同构造器
            return ty1->paramTys.size() == ty2->paramTys.size();

        case TYPE_TUPLE:
            // 元素数量相同的元组具有相同构造器
            return ty1->elementTys.size() == ty2->elementTys.size();

        case TYPE_ARRAY:
        case TYPE_VARRAY:
        case TYPE_POINTER:
            // 容器类型总是相同构造器
            return true;

        default:
            // 原始类型直接比较 kind
            return true;
    }
}
```

## 8. 类型相等性

### 结构相等

```cpp
bool TypeManager::IsTyEqual(const Ptr<Ty>& ty1, const Ptr<Ty>& ty2) {
    if (ty1.get() == ty2.get()) return true;  // 指针相等
    if (ty1->kind != ty2->kind) return false;

    switch (ty1->kind) {
        case TYPE_FUNC:
            if (ty1->paramTys.size() != ty2->paramTys.size()) return false;
            for (size_t i = 0; i < ty1->paramTys.size(); i++) {
                if (!IsTyEqual(ty1->paramTys[i], ty2->paramTys[i])) return false;
            }
            return IsTyEqual(ty1->retTy, ty2->retTy);

        case TYPE_TUPLE:
            if (ty1->elementTys.size() != ty2->elementTys.size()) return false;
            for (size_t i = 0; i < ty1->elementTys.size(); i++) {
                if (!IsTyEqual(ty1->elementTys[i], ty2->elementTys[i])) return false;
            }
            return true;

        case TYPE_CLASS:
        case TYPE_STRUCT:
        case TYPE_ENUM:
            return IsClassTyEqual(ty1, ty2);

        case TYPE_GENERIC:
            // 泛型类型按身份比较
            return ty1->index == ty2->index &&
                   ty1->declaredIn == ty2->declaredIn;

        default:
            return true;  // 原始类型只需 kind 相同
    }
}
```

### 类类型相等

```cpp
bool TypeManager::IsClassTyEqual(const Ptr<Ty>& ty1, const Ptr<Ty>& ty2) {
    // 必须是同一声明
    if (ty1->decl != ty2->decl) return false;

    // 类型参数必须相等
    if (ty1->typeArgs.size() != ty2->typeArgs.size()) return false;
    for (size_t i = 0; i < ty1->typeArgs.size(); i++) {
        if (!IsTyEqual(ty1->typeArgs[i], ty2->typeArgs[i])) return false;
    }

    return true;
}
```

## 9. 总结

仓颉编译器的类型系统具有以下特点：

1. **双层类型表示**：AST 层用于语法分析和语义检查，CHIR 层用于代码生成
2. **类型缓存**：通过哈希去重确保类型的唯一性
3. **类型变量作用域**：支持嵌套的泛型上下文
4. **理想类型**：延迟字面量的类型确定
5. **结构化类型相等**：基于类型结构而非引用判断相等
