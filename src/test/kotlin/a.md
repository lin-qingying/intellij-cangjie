
### 变量声明的语法定义
```antlrv4
variableDeclaration :
 variableModifier* NL* (LET | VAR | CONST) NL* patternsMaybeIrrefutable (
  (NL* COLON NL* type)? (NL* ASSIGN NL* expression)
| (NL* COLON NL*
  type)
)
```
### 是否应该将普遍变量声明与模式区分开，定义以下语法
let A(a):Int = A(1)  这里的类型显得是否突兀
```antlrv4 
variableDeclaration :
 variableModifier* NL* (LET | VAR | CONST) NL* (patternsMaybeIrrefutable | (identifier (NL* COLON NL* type)?))  (
  (NL* ASSIGN NL* expression)
|  variableModifier* NL* (LET | VAR | CONST) NL*   identifier (NL* COLON NL*
  type)
);

```
