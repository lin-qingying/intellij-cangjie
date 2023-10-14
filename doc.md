## 语法分析

### 表达式语句构成
语句：表达式，类声明，函数声明，接口声明，结构体声明
表达式：赋值表达式，函数调用表达式，运算表达式，变量表达式，常量表达式，类型转换表达式，类型断言表达式，索引表达式，切片表达式，

仓颉自顶向下
class ,enum ,struct,interface 不能嵌套声明，可以包含 函数定义，赋值定义，属性定义 
func 可以嵌套声明func 可以含 函数定义，赋值定义，表达式


if while match for 流程控制表达式，都是有值返回式，可以赋值给变量
 
语法套件
https://github.com/JetBrains/Grammar-Kit
分析表达式
https://en.wikipedia.org/wiki/Parsing_expression_grammar

