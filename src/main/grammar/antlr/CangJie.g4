grammar CangJie;



@header {
package  antlr.parser;
}



// **词法部分**

////////////////////////////////////词法/////////////////////////////////////////////////
/*********************************注释*****************************************/
DelimitedComment
: '/*' ( DelimitedComment | . )*? '*/'
;
LineComment
: '//' ~[\n\r]*  // 单行注释将被忽略
;
/*********************************空白和换行*****************************************/
WS
:  [\u0020\u0009\u000C]+ -> channel(HIDDEN)
;
NL
: '\u000A' | '\u000D' '\u000A'
;





/*********************************符号*****************************************/
DOT                : '.';
COMMA              : ',';
LPAREN             : '(';
RPAREN             : ')';
LSQUARE            : '[';
RSQUARE            : ']';
LCURL              : '{';
RCURL              : '}';
EXP                : '**';
MUL                : '*';
MOD                : '%';
DIV                : '/';
ADD                : '+';
SUB                : '-';
PIPELINE           : '|>';
COMPOSITION        : '~>';
INC                : '++';
DEC                : '--';
AND                : '&&';
OR                 : '||';
NOT                : '!';
BITAND             : '&';
BITOR              : '|';
BITXOR             : '^';
LSHIFT             : '<<';
RSHIFT             : '>>';
COLON              : ':';
SEMI               : ';';
ASSIGN             : '=';
ADD_ASSIGN         : '+=';
SUB_ASSIGN         : '-=';
MUL_ASSIGN         : '*=';
EXP_ASSIGN         : '**=';
DIV_ASSIGN         : '/=';
MOD_ASSIGN         : '%=';
AND_ASSIGN         : '&&=';
OR_ASSIGN          : '||=';
BITAND_ASSIGN      : '&=';
BITOR_ASSIGN       : '|=';
BITXOR_ASSIGN      : '^=';
LSHIFT_ASSIGN      : '<<=';
RSHIFT_ASSIGN      : '>>=';
ARROW              : '->';
BACKARROW          : '<-';
DOUBLE_ARROW       : '=>';
ELLIPSIS           : '...';
CLOSEDRANGEOP      : '..=';
RANGEOP            : '..';
HASH               : '#';
AT                 : '@';
QUEST              : '?';
UPPERBOUND         : '<:';
LT                 : '<';
GT                 : '>';
LE                 : '<=';
GE                 : '>=';
NOTEQUAL           : '!=';
EQUAL              : '==';
WILDCARD           : '_';
BACKSLASH          : '\\';
QUOTESYMBOL        : '`';
DOLLAR             : '$';
QUOTE_OPEN         : '"';
TRIPLE_QUOTE_OPEN  : '"""' NL;
//QUOTE_CLOSE        : '"';
//TRIPLE_QUOTE_CLOSE : '"""'?;
LineStrExprStart   : '${';
//MultiLineStrExprStart: '${';

/*********************************关键字*****************************************/
INT8               : 'Int8';
INT16              : 'Int16';
INT32              : 'Int32';
INT64              : 'Int64';
INTNATIVE          : 'IntNative';
UINT8              : 'UInt8';
UINT16             : 'UInt16';
UINT32             : 'UInt32';
UINT64             : 'UInt64';
UINTNATIVE         : 'UIntNative';
FLOAT16            : 'Float16';
FLOAT32            : 'Float32';
FLOAT64            : 'Float64';
RUNE               : 'Rune';
BOOL            : 'Bool';
UNIT               : 'Unit';
NOTHING            : 'Nothing';
STRUCT             : 'struct';
ENUM               : 'enum';
THISTYPE           : 'This';
PACKAGE            : 'package';
IMPORT             : 'import';
CLASS              : 'class';
INTERFACE          : 'interface';
FUNC               : 'func';
MAIN               : 'main';
LET                : 'let';
VAR                : 'var';
CONST              : 'const';
TYPE_ALIAS         : 'type';
INIT               : 'init';
THIS               : 'this';
SUPER              : 'super';
// 控制流和条件关键字
IF                : 'if';
ELSE              : 'else';
CASE              : 'case';
TRY               : 'try';
CATCH             : 'catch';
FINALLY           : 'finally';
FOR               : 'for';
DO                : 'do';
WHILE             : 'while';
THROW             : 'throw';
RETURN            : 'return';
CONTINUE          : 'continue';
BREAK             : 'break';
IS                : 'is';
AS                : 'as';
IN                : 'in';
MATCH             : 'match';
FROM              : 'from';
WHERE             : 'where';

// 类型和成员修饰符
EXTEND            : 'extend';
SPAWN             : 'spawn';
SYNCHRONIZED      : 'synchronized';
MACRO             : 'macro';
QUOTE             : 'quote';
TRUE              : 'true';
FALSE             : 'false';
STATIC            : 'static';
PUBLIC            : 'public';
PRIVATE           : 'private';
PROTECTED         : 'protected';
OVERRIDE          : 'override';
REDEF             : 'redef';
ABSTRACT          : 'abstract';
OPEN              : 'open';
OPERATOR          : 'operator';
FOREIGN           : 'foreign';
INOUT             : 'inout';
PROP              : 'prop';
MUT               : 'mut';
UNSAFE            : 'unsafe';
GET               : 'get';
SET               : 'set';


/*********************************字面量*****************************************/
// 整数类型字面量后缀
IntegerLiteralSuffix
  : 'i8' | 'i16' | 'i32' | 'i64' | 'u8' | 'u16' | 'u32' | 'u64'
;

// 整数字面量
IntegerLiteral
  : BinaryLiteral IntegerLiteralSuffix?
  | OctalLiteral IntegerLiteralSuffix?
  | DecimalLiteral ('_'* IntegerLiteralSuffix)?
  | HexadecimalLiteral IntegerLiteralSuffix?
;

// 二进制字面量
BinaryLiteral
  : '0' [bB] BinDigit (BinDigit | '_')*
;

// 二进制数字字符
BinDigit
  : [01]
;

// 八进制字面量
OctalLiteral
  : '0' [oO] OctalDigit (OctalDigit | '_')*
;

// 八进制数字字符
OctalDigit
  : [0-7]
;

// 十进制字面量
DecimalLiteral
  : (DecimalDigitWithOutZero (DecimalDigit | '_')*) | DecimalDigit
;

fragment DecimalFragment
  : DecimalDigit (DecimalDigit | '_')*
;

fragment DecimalDigit
  : [0-9]
;

fragment DecimalDigitWithOutZero
  : [1-9]
;

// 十六进制字面量
HexadecimalLiteral
  : '0' [xX] HexadecimalDigits
;

// 十六进制数字字符序列
HexadecimalDigits
  : HexadecimalDigit (HexadecimalDigit | '_')*
;

// 十六进制数字字符
HexadecimalDigit
  : [0-9a-fA-F]
;

// 浮点数类型字面量后缀
FloatLiteralSuffix
  : 'f16' | 'f32' | 'f64'
;

// 浮点数字面量
FloatLiteral
  : (DecimalLiteral DecimalExponent | DecimalFraction DecimalExponent? | (DecimalLiteral DecimalFraction) DecimalExponent?) FloatLiteralSuffix?
  | (Hexadecimalprefix (HexadecimalDigits | HexadecimalFraction | (HexadecimalDigits HexadecimalFraction)) HexadecimalExponent)
;

// 十进制小数部分
fragment DecimalFraction
  : '.' DecimalFragment
;

// 十进制指数部分
fragment DecimalExponent
  : FloatE Sign? DecimalFragment
;

// 符号（正负）
fragment Sign
  : [-]
;

// 十六进制前缀
fragment Hexadecimalprefix
  : '0' [xX]
;

// 十六进制小数部分
HexadecimalFraction
  : '.' HexadecimalDigits
;

// 十六进制指数部分
HexadecimalExponent
  : FloatP Sign? DecimalFragment
;

// 浮点数指数符号（e或E）
FloatE
  : [eE]
;

// 十六进制浮点数指数符号（p或P）
FloatP
  : [pP]
;

// 字符字面量
CharacterLiteral
  : '\'' (SingleChar | EscapeSeq) '\''
;

// 单一字符（排除单引号、反斜杠和换行符）
SingleChar
  : ~['\\\r\n]
;

// 转义序列
EscapeSeq
  : UniCharacterLiteral
  | EscapedIdentifier
;

// 定义 Unicode 字符字面量
UniCharacterLiteral
  : '\\' 'u' '{' HexadecimalDigit (HexadecimalDigit (HexadecimalDigit (HexadecimalDigit (HexadecimalDigit HexadecimalDigit?)?)?)?)? '}'
;

// 定义转义标识符
EscapedIdentifier
  : '\\' ('t' | 'b' | 'r' | 'n' | '\'' | '"' | '\\' | 'f' | 'v' | '0' | '$')
;

// 定义字符字节字面量
CharacterByteLiteral
  : 'b' '\'' (SingleCharByte | ByteEscapeSeq) '\''
;

// 定义字节转义序列
ByteEscapeSeq
  : HexCharByte
  | ByteEscapedIdentifier
;

// 单个字节字符（排除特定ASCII码）
SingleCharByte
  : [\u0000-\u0009\u000B\u000C\u000E-\u0021\u0023-\u0026\u0028-\u005B\u005D-\u007F]
;

fragment ByteEscapedIdentifier
  : '\\' ('t' | 'b' | 'r' | 'n' | '\'' | '"' | '\\' | 'f' | 'v' | '0')
;

fragment HexCharByte
  : '\\' 'u' '{' HexadecimalDigit (HexadecimalDigit)? '}'
;

// 定义字节数组字符串字面量
ByteStringArrayLiteral
  : 'b' '"' (SingleCharByte | ByteEscapeSeq)* '"'
;

// J风格字符串字面量
JStringLiteral
  : 'J' '"' (SingleChar | EscapeSeq)* '"'
;

// 单行字符串文本片段
LineStrText
  : ~["\\\r\n] | EscapeSeq
;

// 三引号结束标记
TRIPLE_QUOTE_CLOSE
  : MultiLineStringQuote? '"""'
;

// 多行字符串引号定义
MultiLineStringQuote
  : '"' '+'
;

// 多行字符串文本片段
MultiLineStrText
  : ~('\\') | EscapeSeq
;

// 多行原始字符串字面量
MultiLineRawStringLiteral
  : MultiLineRawStringContent
;

fragment MultiLineRawStringContent
  : HASH MultiLineRawStringContent HASH
  | HASH '"' .*? '"' HASH
;
/*********************************标识符*****************************************/
identifier
: Identifier
| PUBLIC
| PRIVATE
| PROTECTED
| OVERRIDE
| ABSTRACT
| OPEN
| REDEF
| GET
| SET
;

Identifier
: '_'* Letter (Letter | '_' | DecimalDigit)*
| '`' '_'* Letter (Letter | '_' | DecimalDigit)* '`'
;
Letter
: [a-zA-Z]
;

DollarIdentifier
: '$' Identifier
;
////////////////////////////////////语法/////////////////////////////////////////////////
/*********************************编译单元*****************************************/
translationUnit
: NL* preamble end* topLevelObject* (end+ mainDefinition)? NL* (topLevelObject (end+
  topLevelObject?)*)? EOF
;

end
  : NL | SEMI
  ;



/*********************************包定义和包导入*****************************************/
preamble
  : packageHeader? importList*
  ;

packageHeader
  : PACKAGE  packageNameIdentifier
  ;

packageNameIdentifier
  : identifier (NL* DOT NL* identifier)*
  ;

importList
: (FROM NL* identifier)? NL* IMPORT NL* importAllOrSpecified
(NL* COMMA NL* importAllOrSpecified)* end+
  ;

importAllOrSpecified
  : importAll
  | importSpecified (NL* importAlias)?
  ;

importSpecified
  :  (identifier NL* DOT NL*)+ identifier
  ;

importAll
  :  (identifier NL* DOT NL*)+ MUL
  ;

importAlias
  : AS (NL* | WS+ ) identifier
  ;


/*********************************顶层声明*****************************************/
// 顶级对象定义
topLevelObject
    : classDefinition
    | interfaceDefinition
    | functionDefinition
    | variableDeclaration
    | enumDefinition
    | structDefinition
    | typeAlias
    | extendDefinition
    | foreignDeclaration
    | macroDefinition
    | macroExpression
    ;

// 类定义
classDefinition
  : (classModifierList NL*)? CLASS NL* identifier
  (NL* typeParameters NL*)?
  (NL* UPPERBOUND NL* superClassOrInterfaces)?
  (NL* genericConstraints)?
  NL* classBody
  ;


superClassOrInterfaces
    : superClass (NL* BITAND NL* superInterfaces)?
    | superInterfaces
    ;

classModifierList
    : classModifier+
    ;

classModifier
    : PUBLIC
    | ABSTRACT
    | OPEN
    ;

typeParameters
    : LT NL* identifier (NL* COMMA NL* identifier)* NL* GT
    ;

superClass
    : classType
    ;

classType
    : (identifier NL* DOT NL*)* identifier (NL* typeParameters)?
    ;

typeArguments
    : LT NL* type (NL* COMMA NL* type)* NL* GT
    ;

superInterfaces
    : interfaceType (NL* COMMA NL* interfaceType )*
    ;

interfaceType
    : classType
    ;

genericConstraints
    : WHERE NL* (identifier | THISTYPE) NL* UPPERBOUND NL* upperBounds
        (NL* COMMA NL* (identifier | THISTYPE) NL* UPPERBOUND NL* upperBounds)*
    ;

upperBounds
    : type (NL* BITAND NL* type)*
    ;

classBody
    : LCURL end*
        classMemberDeclaration* NL*
        classPrimaryInit? NL*
        classMemberDeclaration* end* RCURL
    ;

classMemberDeclaration
    : (classInit
    | staticInit
    | variableDeclaration
    | functionDefinition
    | operatorFunctionDefinition
    | macroExpression
    | propertyDefinition
    ) end*
    ;

classInit
    : (classNonStaticMemberModifier | CONST NL*)? INIT NL* functionParameters NL* block
    ;

staticInit
    : STATIC INIT LPAREN RPAREN
        LCURL expressionOrDeclarations? RCURL
    ;

classPrimaryInit
    : (classNonStaticMemberModifier | CONST NL*)? className NL* LPAREN NL*
        classPrimaryInitParamLists NL* RPAREN NL*
        LCURL NL*
        (SUPER callSuffix)? end
        expressionOrDeclarations?
        NL* RCURL
    ;

className
    : identifier
    ;



// 类初始化参数列表规则
classPrimaryInitParamLists
    : unnamedParameterList (NL* COMMA NL* namedParameterList)? (NL* COMMA NL* classNamedInitParamList)?
    | unnamedParameterList (NL* COMMA NL* classUnnamedInitParamList)? (NL* COMMA NL* classNamedInitParamList)?
    | classUnnamedInitParamList (NL* COMMA NL* classNamedInitParamList)?
    | namedParameterList (NL* COMMA NL* classNamedInitParamList)?
    | classNamedInitParamList
    ;

classUnnamedInitParamList
    : classUnnamedInitParam (NL* COMMA NL* classUnnamedInitParam)*
    ;

classNamedInitParamList
    : classNamedInitParam (NL* COMMA NL* classNamedInitParam)*
    ;

classUnnamedInitParam
    : (classNonStaticMemberModifier NL*)? (LET | VAR) NL* identifier NL* COLON NL* type
    ;

classNamedInitParam
    : (classNonStaticMemberModifier NL*)? (LET | VAR) NL* identifier NL* NOT NL* COLON NL* type (NL* ASSIGN NL* expression)?
    ;

classNonStaticMemberModifier
    : PUBLIC
    | PRIVATE
    | PROTECTED
    ;


// 接口定义规则
interfaceDefinition
    : (interfaceModifierList NL*)? INTERFACE NL* identifier
        (NL* typeParameters NL*)?
        (NL* UPPERBOUND NL* superInterfaces)?
        (NL* genericConstraints)?
        NL* interfaceBody
    ;

interfaceBody
    : LCURL end* interfaceMemberDeclaration* end* RCURL
    ;

interfaceMemberDeclaration
    : (functionDefinition
    | operatorFunctionDefinition
    | macroExpression
    | propertyDefinition
    ) end*
    ;

interfaceModifierList
    : interfaceModifier+
    ;

interfaceModifier
    : PUBLIC
    | OPEN
    ;


// 函数定义规则
functionDefinition
    : (functionModifierList NL*)? FUNC (NL* | WS+) identifier
        (NL* typeParameters NL*)?
        NL* functionParameters
        (NL* COLON NL* type)?
        (NL* genericConstraints)?
        (NL* block)?
    ;

 // 运算符函数定义规则
 operatorFunctionDefinition
     : (functionModifierList NL*)? OPERATOR NL* FUNC
         NL* overloadedOperators
         (NL* typeParameters NL*)?
         NL* functionParameters
         (NL* COLON NL* type)?
         (NL* genericConstraints)?
         (NL* block)?
     ;

 // 函数参数列表规则
 functionParameters
     : (LPAREN (NL* unnamedParameterList ( NL* COMMA NL* namedParameterList)? )? NL* RPAREN NL*)
     | (LPAREN NL* (namedParameterList NL*)? RPAREN NL*)
     ;

 // 非默认参数列表规则
 nondefaultParameterList
     : unnamedParameter (NL* COMMA NL* unnamedParameter)* (NL* COMMA NL* namedParameter)*
     | namedParameter (NL* COMMA NL* namedParameter)*
     ;

 // 未命名参数列表规则
 unnamedParameterList
     : unnamedParameter (NL* COMMA NL* unnamedParameter)*
     ;

 // 未命名参数规则
 unnamedParameter
     : (identifier | WILDCARD) NL* COLON NL* type
     ;

 // 命名参数列表规则
 namedParameterList
     : (namedParameter | defaultParameter) (NL* COMMA NL* (namedParameter | defaultParameter))*
     ;

 // 命名参数规则
 namedParameter
     : identifier NL* NOT NL* COLON NL* type
     ;

 // 默认参数规则
 defaultParameter
     : identifier NL* NOT NL* COLON NL* type NL* ASSIGN NL* expression
     ;

 // 函数修饰符列表规则
 functionModifierList
     : (functionModifier NL*)+
     ;

 // 函数修饰符规则
 functionModifier
     : PUBLIC
     | PRIVATE
     | PROTECTED
     | STATIC
     | OPEN
     | OVERRIDE
     | OPERATOR
     | REDEF
     | MUT
     | UNSAFE
     | CONST
     ;

//变量声明
variableDeclaration
: variableModifier* (NL* | WS+) (LET | VAR | CONST) (NL* | WS+)  patternsMaybeIrrefutable
  ( ((NL* | WS+)  COLON (NL* | WS+)  type)? ((NL* | WS+)  ASSIGN (NL* | WS+)  expression)
  | ((NL* | WS+)  COLON (NL* | WS+)  type)
  )
;

variableModifier
: PUBLIC
| PRIVATE
| PROTECTED
| STATIC
;


// 枚举定义规则
enumDefinition
    : (enumModifier NL*)? ENUM NL* identifier (NL* typeParameters NL*)?
        (NL* UPPERBOUND NL* superInterfaces)?
        (NL* genericConstraints)? NL* LCURL end* enumBody end* RCURL
    ;

enumBody
    : (BITOR NL*)? caseBody (NL* BITOR NL* caseBody)*
        (NL*
            ( functionDefinition
            | operatorFunctionDefinition
            | propertyDefinition
            | macroExpression
        ))*
    ;
caseBody
    : identifier ( NL* LPAREN NL* type (NL* COMMA NL* type)* NL* RPAREN)?
    ;
enumModifier
    : PUBLIC
    ;


// 结构体定义规则
structDefinition
    : (structModifier NL*)? STRUCT NL* identifier (NL* typeParameters NL*)?
        (NL* UPPERBOUND NL* superInterfaces)?
        (NL* genericConstraints)? NL* structBody
    ;

structBody
    : LCURL end*
        structMemberDeclaration* NL*
        structPrimaryInit? NL*
        structMemberDeclaration*
    end* RCURL
    ;

structMemberDeclaration
    : (structInit
        | staticInit
        | variableDeclaration
        | functionDefinition
        | operatorFunctionDefinition
        | macroExpression
        | propertyDefinition
    ) end*
    ;

structInit
    : (structNonStaticMemberModifier | CONST NL*)? INIT NL* functionParameters NL* block
    ;



structPrimaryInit
    : (structNonStaticMemberModifier | CONST NL*)? structName NL* LPAREN NL*
        structPrimaryInitParamLists? NL* RPAREN NL*
        LCURL NL* expressionOrDeclarations? NL* RCURL
    ;

structName
    : identifier
    ;

structPrimaryInitParamLists
    : unnamedParameterList (NL* COMMA NL* namedParameterList)? (NL* COMMA NL* structNamedInitParamList)?
    | unnamedParameterList (NL* COMMA NL* structUnnamedInitParamList)? (NL* COMMA NL* structNamedInitParamList)?
    | structUnnamedInitParamList (NL* COMMA NL* structNamedInitParamList)?
    | namedParameterList (NL* COMMA NL* structNamedInitParamList)?
    | structNamedInitParamList
    ;

structUnnamedInitParamList
    : structUnnamedInitParam (NL* COMMA NL* structUnnamedInitParam)*
    ;

structNamedInitParamList
    : structNamedInitParam (NL* COMMA NL* structNamedInitParam)*
    ;

structUnnamedInitParam
    : (structNonStaticMemberModifier NL*)? (LET | VAR) NL* identifier NL* COLON NL* type
    ;

structNamedInitParam
    : (structNonStaticMemberModifier NL*)? (LET | VAR) NL* identifier NL* NOT NL* COLON NL* type
        (NL* ASSIGN NL* expression)?
    ;

structModifier
    : PUBLIC
    ;

structNonStaticMemberModifier
    : PUBLIC
    | PRIVATE
    ;


// 类型别名定义规则
typeAlias
    : (typeModifier NL*)? TYPE_ALIAS (NL* | WS+) identifier ((NL* | WS+) typeParameters)? (NL* | WS+) ASSIGN (NL* | WS+) type end*
    ;

// 类型修饰符规则
typeModifier
    : PUBLIC
    ;



// 扩展定义规则
extendDefinition
    : EXTEND NL* extendType
        (NL* UPPERBOUND NL* superInterfaces)? (NL* genericConstraints)?
        NL* extendBody
    ;

// 扩展类型规则，可以是命名空间下的标识符类型或内置基本类型
extendType
    : (identifier NL* DOT NL*)* identifier (NL* typeParameters)?
    | INT8
    | INT16
    | INT32
    | INT64
    | INTNATIVE
    | UINT8
    | UINT16
    | UINT32
    | UINT64
    | UINTNATIVE
    | FLOAT16
    | FLOAT32
    | FLOAT64
    | RUNE
    | BOOL
    | NOTHING
    | UNIT
    ;

// 扩展体规则，包含扩展的成员声明
extendBody
    : LCURL end* extendMemberDeclaration* end* RCURL
    ;

// 扩展成员声明规则，可以是函数、运算符函数、宏表达式或属性定义
extendMemberDeclaration
    : (functionDefinition
      | operatorFunctionDefinition
      | macroExpression
      | propertyDefinition
    ) end*
    ;


// 外部声明规则
foreignDeclaration
    : FOREIGN NL* (foreignBody | foreignMemberDeclaration)
    ;

// 外部体规则，包含外部成员声明
foreignBody
    : LCURL end* foreignMemberDeclaration* end* RCURL
    ;

// 外部成员声明规则，可以是类定义、接口定义、函数定义、宏表达式或变量声明
foreignMemberDeclaration
    : (classDefinition
      | interfaceDefinition
      | functionDefinition
      | macroExpression
      | variableDeclaration
    ) end*
    ;


// 注解列表规则
annotationList
    : annotation+
    ;

// 注解规则，包括注解名和可选参数列表
annotation
    : AT (identifier NL* DOT)* identifier (LSQUARE NL* annotationArgumentList NL* RSQUARE)?
    ;

// 注解参数列表规则
annotationArgumentList
    : annotationArgument (NL* COMMA NL* annotationArgument)* NL* COMMA?
    ;

// 注解参数规则，可以是键值对或简单表达式
annotationArgument
    : identifier NL* COLON NL* expression
    | expression
    ;


// 宏定义规则，包含宏标识符、参数定义以及可选返回类型与实现体
macroDefinition
    : PUBLIC NL* MACRO NL* identifier NL*
        (macroWithoutAttrParam | macroWithAttrParam) NL*
        (COLON NL* identifier NL*)?
        (ASSIGN NL* expression | block)
    ;

// 宏定义无属性参数部分
macroWithoutAttrParam
    : LPAREN NL* macroInputDecl NL* RPAREN
    ;

// 宏定义带有属性参数部分
macroWithAttrParam
    : LPAREN NL* macroAttrDecl NL* COMMA NL* macroInputDecl NL* RPAREN
    ;

// 宏输入参数声明规则
macroInputDecl
    : identifier NL* COLON NL* identifier
    ;

// 宏属性声明规则
macroAttrDecl
    : identifier NL* COLON NL* identifier
    ;


// 属性定义规则，包括访问修饰符、属性关键字、标识符、类型以及可选的属性体
propertyDefinition
    : propertyModifier* NL* PROP NL* identifier NL* COLON NL* type NL* propertyBody?
    ;

// 属性体规则，包含 get 和 set 方法实现
propertyBody
    : LCURL end* propertyMemberDeclaration+ end* RCURL
    ;

// 属性成员声明规则，分别对应 get 和 set 方法
propertyMemberDeclaration
    : GET NL* LPAREN RPAREN NL* block end*
    | SET NL* LPAREN identifier RPAREN NL* block end*
    ;

// 属性访问修饰符规则
propertyModifier
    : PUBLIC
    | PRIVATE
    | PROTECTED
    | STATIC
    | OPEN
    | OVERRIDE
    | REDEF
    | MUT
    ;


// main 函数定义规则
mainDefinition
    : MAIN
        NL* functionParameters
        (NL* COLON NL* type)?
        NL* block
    ;


/*********************************类型*****************************************/
// 类型规则
type
    : arrowType
    | tupleType
    | prefixType
    | atomicType
    ;

// 箭头类型（函数类型）规则，例如 (A, B) -> C
arrowType
    : arrowParameters (NL* | WS+)  ARROW (NL* | WS+)  type
    ;

// 箭头参数列表规则，可以包含多个类型参数
arrowParameters
    : LPAREN (NL* | WS+)  (type ((NL* | WS+)  COMMA (NL* | WS+)  type)* (NL* | WS+) )? RPAREN
    ;

// 元组类型规则，例如 (A, B)
tupleType
    : LPAREN NL* type (NL* COMMA NL* type)+ NL* RPAREN
    ;

// 前缀类型规则，目前只定义了可空类型操作符？
prefixType
    : prefixTypeOperator type
    ;

// 前缀类型操作符规则，这里仅列举了可空类型操作符？
prefixTypeOperator
    : QUEST
    ;

// 原子类型规则，包括字符语言类型、用户自定义类型和括号包裹的类型
atomicType
    : charLangTypes
    | userType
    | parenthesizedType
    ;

// 字符语言类型规则，涵盖了数值类型、字符、布尔值、Nothing、Unit 和 ThisType 关键字
charLangTypes
    : numericTypes
    | RUNE
    | BOOL
    | NOTHING
    | UNIT
    | THISTYPE
    ;

// 数值类型规则，列举了多种整数和浮点数类型
numericTypes
    : INT8
    | INT16
    | INT32
    | INT64
    | INTNATIVE
    | UINT8
    | UINT16
    | UINT32
    | UINT64
    | UINTNATIVE
    | FLOAT16
    | FLOAT32
    | FLOAT64
    ;

// 用户自定义类型规则，由零个或多个命名空间限定符后跟标识符及可选的类型参数列表组成
userType
    : (identifier NL* DOT NL*)* identifier ( NL* typeArguments)?
    ;

// 括号包裹的类型规则，用于提升表达式的优先级或者表示单元素的元组类型
parenthesizedType
    : LPAREN NL* type NL* RPAREN
    ;



/*********************************表达式*****************************************/

 // 表达式规则，其基本形式为赋值表达式
 expression
     : assignmentExpression
     ;

 // 赋值表达式的定义，包括多种左值和右值组合方式的赋值操作
 assignmentExpression
     : leftValueExpressionWithoutWildCard NL* assignmentOperator NL* flowExpression
     | leftValueExpression NL* ASSIGN NL* flowExpression
     | tupleLeftValueExpression NL* ASSIGN NL* flowExpression
     | flowExpression
     ;

 // 元组左值表达式规则，可以包含多个左值表达式或嵌套元组左值表达式
 tupleLeftValueExpression
     : LPAREN NL*
         (leftValueExpression | tupleLeftValueExpression)
         (NL* COMMA NL* (leftValueExpression | tupleLeftValueExpression))+
         NL* COMMA? NL*
     RPAREN
     ;

 // 左值表达式规则（不包含通配符）
 leftValueExpression
     : leftValueExpressionWithoutWildCard
     | WILDCARD
     ;

 // 不含通配符的左值表达式子规则
 leftValueExpressionWithoutWildCard
     : identifier
     | leftAuxExpression QUEST? NL* assignableSuffix
     ;

 // 左辅助表达式子规则，用于构建更复杂的左值表达式结构
 leftAuxExpression
     : identifier (NL* typeArguments)?
     | type
     | thisSuperExpression
     | leftAuxExpression QUEST? NL* DOT NL* identifier (NL* typeArguments)?
     | leftAuxExpression QUEST? callSuffix
     | leftAuxExpression QUEST? indexAccess
     ;

 // 可赋值后缀，包括字段访问和索引访问
 assignableSuffix
     : fieldAccess
     | indexAccess
     ;

 // 字段访问表达式，通过点运算符访问对象的属性或字段
 fieldAccess
     : NL* DOT NL* identifier
     ;




// 流控制表达式规则，基于逻辑运算符和空合并操作进行递归组合
flowExpression
    : coalescingExpression (NL* flowOperator NL* coalescingExpression)*
    ;

// 空合并表达式规则，使用双问号 `??` 进行空值合并操作
coalescingExpression
    : logicDisjunctionExpression (NL* QUEST QUEST NL* logicDisjunctionExpression)*
    ;

// 逻辑或表达式规则，通过逻辑 OR 运算符 `OR` 进行递归组合
logicDisjunctionExpression
    : logicConjunctionExpression (NL* OR NL* logicConjunctionExpression)*
    ;

// 逻辑与表达式规则，通过逻辑 AND 运算符 `AND` 进行递归组合
logicConjunctionExpression
    : rangeExpression (NL* AND NL* rangeExpression)*
    ;

// 范围表达式规则，处理连续范围和不连续范围的表达式
rangeExpression
    : bitwiseDisjunctionExpression NL* (CLOSEDRANGEOP | RANGEOP) NL* bitwiseDisjunctionExpression (NL* COLON NL* bitwiseDisjunctionExpression)?
    | bitwiseDisjunctionExpression
    ;
// 按位或表达式规则：由一个或多个按位异或表达式通过按位或（|）运算符连接而成
bitwiseDisjunctionExpression
: bitwiseXorExpression (NL* BITOR NL* bitwiseXorExpression)*
;

// 按位异或表达式规则，通过按位异或运算符 `BITXOR` 进行递归组合
bitwiseXorExpression
    : bitwiseConjunctionExpression (NL* BITXOR NL* bitwiseConjunctionExpression)*
    ;

// 按位与表达式规则，通过按位与运算符 `BITAND` 进行递归组合
bitwiseConjunctionExpression
    : equalityComparisonExpression (NL* BITAND NL* equalityComparisonExpression)*
    ;

// 等价比较表达式规则，包括等于、不等于等比较操作符以及类型检查操作符
equalityComparisonExpression
    : comparisonOrTypeExpression (NL* equalityOperator NL* comparisonOrTypeExpression)?
    ;

// 比较或类型检查表达式规则，包含常规比较运算符以及类型转换和类型检查操作
comparisonOrTypeExpression
    : shiftingExpression (NL* comparisonOperator NL* shiftingExpression)?
    | shiftingExpression (NL* IS NL* type)?
    | shiftingExpression (NL* AS NL* type)?
    ;

// 移位表达式规则，基于移位运算符对加法表达式进行递归组合
shiftingExpression
    : additiveExpression (NL* shiftingOperator NL* additiveExpression)*
    ;

// 加法表达式规则，基于加法和减法运算符对乘法表达式进行递归组合
additiveExpression
    : multiplicativeExpression (NL* additiveOperator NL* multiplicativeExpression)*
    ;

// 乘法表达式规则，基于乘法、除法等运算符对指数表达式进行递归组合
multiplicativeExpression
    : exponentExpression (NL* multiplicativeOperator NL* exponentExpression)*
    ;

// 指数表达式规则，通过指数运算符对前缀一元表达式进行递归组合
exponentExpression
    : prefixUnaryExpression (NL* exponentOperator NL* prefixUnaryExpression)*
    ;

// 前缀一元表达式规则，由零个或多个前缀一元运算符与增减表达式组成
prefixUnaryExpression
    : prefixUnaryOperator* incAndDecExpression
    ;

// 增减表达式规则，包括后缀的自增（INC）或自减（DEC）操作
incAndDecExpression
    : postfixExpression (INC | DEC )?
    ;

// 后缀表达式规则，包含原子表达式以及各种调用、属性访问、索引访问等形式
postfixExpression
    : atomicExpression
    | type NL* DOT NL* identifier
    | postfixExpression NL* DOT NL* identifier (NL* typeArguments)?
    | postfixExpression callSuffix
    | postfixExpression indexAccess
    | postfixExpression NL* DOT NL* identifier callSuffix? trailingLambdaExpression
    | identifier callSuffix? trailingLambdaExpression
    | postfixExpression (QUEST questSeperatedItems)+
    ;

// 以问号分隔的项目列表规则，用于处理可选参数或其他问号分隔的语法结构
questSeperatedItems
    : questSeperatedItem+
    ;

// 以问号分隔的单个项目规则，通常跟随着一个方法调用、索引访问或尾随lambda表达式
questSeperatedItem
    : itemAfterQuest (callSuffix | callSuffix? trailingLambdaExpression | indexAccess)?
    ;

// 问号后面跟随的项目子规则，可以是属性访问、方法调用、索引访问或尾随lambda表达式
itemAfterQuest
    : DOT identifier (NL* typeArguments)?
    | callSuffix
    | indexAccess
    | trailingLambdaExpression
    ;

// 方法调用后缀规则，定义了方法调用参数的括号包裹形式
callSuffix
    : LPAREN NL* (valueArgument (NL* COMMA NL* valueArgument)* NL*)? RPAREN
    ;

// 参数值规则，包括命名参数和普通参数两种形式
valueArgument
    : identifier NL* COLON NL* expression
    | expression
    | refTransferExpression
    ;

// 引用传递表达式规则，表示 INOUT 关键字修饰的引用传递参数
refTransferExpression
    : INOUT (expression DOT)? identifier
    ;

// 索引访问规则，使用方括号获取数组或集合元素
indexAccess
    : LSQUARE NL* (expression | rangeElement) NL* RSQUARE
    ;








// 范围元素规则，定义了表达式范围内的元素
rangeElement
    : RANGEOP
    | (CLOSEDRANGEOP | RANGEOP) NL* expression
    | expression NL* RANGEOP
    ;

// 原子表达式规则，包含了基本的、不可再分的表达式形式
atomicExpression
    : literalConstant
    | collectionLiteral
    | tupleLiteral
    | identifier (NL* typeArguments)?
    | unitLiteral
    | ifExpression
    | matchExpression
    | loopExpression
    | tryExpression
    | jumpExpression
    | numericTypeConvExpr
    | thisSuperExpression
    | spawnExpression
    | synchronizedExpression
    | parenthesizedExpression
    | lambdaExpression
    | quoteExpression
    | macroExpression
    | unsafeExpression
    ;

// 字面常量规则，包括整型、浮点型、字符型、布尔型、字符串等类型的基本值
literalConstant
    : IntegerLiteral
    | FloatLiteral
    | CharacterLiteral
    | CharacterByteLiteral
    | booleanLiteral
    | stringLiteral
    | ByteStringArrayLiteral
    | unitLiteral
    ;

// 布尔字面量规则，表示 true 或 false
booleanLiteral
    : TRUE
    | FALSE
    ;

// 字符串字面量规则，包含单行字符串、多行字符串和原始多行字符串
stringLiteral
    : lineStringLiteral
    | multiLineStringLiteral
    | MultiLineRawStringLiteral
    ;

// 单行字符串内容规则
lineStringContent
    : LineStrText
    ;

// 单行字符串字面量规则，支持简单的插值表达式
lineStringLiteral
    : QUOTE_OPEN (lineStringExpression | lineStringContent)* QUOTE_OPEN
    ;

// 单行字符串中的插值表达式规则
lineStringExpression
    : LineStrExprStart SEMI* (expressionOrDeclaration (SEMI+ expressionOrDeclaration?)*) SEMI* RCURL
    ;

// 多行字符串内容规则
multiLineStringContent
    : MultiLineStrText
    ;

// 多行字符串字面量规则，同样支持插值表达式
multiLineStringLiteral
    : TRIPLE_QUOTE_OPEN (multiLineStringExpression | multiLineStringContent)* TRIPLE_QUOTE_CLOSE
    ;

// 多行字符串中的插值表达式规则
multiLineStringExpression
    : LineStrExprStart end* (expressionOrDeclaration (end+ expressionOrDeclaration?)*)
      end* RCURL
    ;

// 集合字面量规则，当前只实现了数组字面量
collectionLiteral
    : arrayLiteral
    ;

// 数组字面量规则，由方括号包围，包含零个或多个元素
arrayLiteral
    : LSQUARE (NL* elements)? NL* RSQUARE
    ;

// 数组元素列表规则，元素之间用逗号分隔
elements
    : element (NL* COMMA NL* element)*
    ;

// 元素规则，可以是普通表达式元素或展开操作（...）后的表达式
element
    : expressionElement
    | spreadElement
    ;

// 普通表达式元素规则，即一个独立的表达式
expressionElement
    : expression
    ;

// 展开操作元素规则，使用 `*` 运算符对集合进行展开
spreadElement
    : MUL expression
    ;

// 元组字面量规则，由圆括号包围，包含一个或多个逗号分隔的表达式
tupleLiteral
    : LPAREN NL* expression (NL* COMMA NL* expression)+ NL* RPAREN
    ;

// 单元字面量规则，由一对空圆括号表示
unitLiteral
    : LPAREN NL* RPAREN
    ;



// 条件表达式规则，包含条件判断和两个可选的执行分支（if 和 else）
ifExpression
    : IF NL* LPAREN NL* (LET NL* deconstructPattern NL* BACKARROW NL*)? expression NL* RPAREN
      NL* block
    (NL* ELSE (NL* ifExpression | NL* block))?
    ;

// 拆解模式规则，用于匹配和提取值
deconstructPattern
    : constantPattern
    | wildcardPattern
    | varBindingPattern
    | tuplePattern
    | enumPattern
    ;

// 匹配表达式规则，通过匹配不同情况来执行相应的代码块
matchExpression
    : MATCH NL* LPAREN NL* expression NL* RPAREN NL* LCURL NL* matchCase+ NL* RCURL
    | MATCH NL* LCURL NL* (CASE NL* (expression | WILDCARD) NL* DOUBLE_ARROW NL*
        expressionOrDeclaration (end+ expressionOrDeclaration?)*)+ NL* RCURL
    ;

// 匹配情况规则，定义了每个匹配分支的具体条件与结果
matchCase
    : CASE NL* pattern NL* patternGuard? NL* DOUBLE_ARROW NL* expressionOrDeclaration (end+
        expressionOrDeclaration?)*
    ;

// 模式守卫规则，在匹配模式后添加额外的逻辑条件
patternGuard
    : WHERE NL* expression
    ;

// 模式规则，包括多种类型的模式如常量、通配符、变量绑定、元组、类型以及枚举等
pattern
    : constantPattern
    | wildcardPattern
    | varBindingPattern
    | tuplePattern
    | typePattern
    | enumPattern
    ;

// 常量模式规则，可以是字面常量的组合
constantPattern
    : literalConstant NL* (NL* BITOR NL* literalConstant)*
    ;

// 通配符模式规则，用作任意值的占位符
wildcardPattern
    : WILDCARD
    ;

// 变量绑定模式规则，用于捕获并命名匹配到的值
varBindingPattern
    : identifier
    ;

// 元组模式规则，由一组模式构成，用于匹配结构化数据
tuplePattern
    : LPAREN NL* pattern (NL* COMMA NL* pattern)+ NL* RPAREN
    ;

// 类型模式规则，用于根据类型进行匹配，并可进一步指定变量名和类型
typePattern
    : (WILDCARD | identifier) NL* COLON NL* type
    ;

// 枚举模式规则，用于匹配枚举类型及其参数列表
enumPattern
    : NL* ((userType NL* DOT NL*)? identifier enumPatternParameters?) (NL* BITOR NL* ((userType
        NL* DOT NL*)? identifier enumPatternParameters?))*
    ;

// 枚举模式参数列表规则，用于匹配带有参数的枚举实例
enumPatternParameters
    : LPAREN NL* pattern (NL* COMMA NL* pattern)* NL* RPAREN
    ;

// 循环表达式规则，包括 for-in 循环、while 循环和 do-while 循环
loopExpression
    : forInExpression
    | whileExpression
    | doWhileExpression
    ;

// for-in 循环表达式规则，遍历集合或范围中的元素
forInExpression
    : FOR (NL* | WS+) LPAREN (NL* | WS+) patternsMaybeIrrefutable (NL* | WS+) IN (NL* | WS+) expression NL* patternGuard? NL*
        RPAREN NL* block
    ;

// 可能不可拒绝的模式集合规则，适用于 for-in 表达式中同时匹配多个值的情况
patternsMaybeIrrefutable
    : wildcardPattern
    | varBindingPattern
    | tuplePattern
    | enumPattern
    ;
// 循环表达式规则：
// - while 循环，包含一个条件和循环体
whileExpression
    : WHILE NL* LPAREN NL* (LET NL* deconstructPattern NL* BACKARROW NL*)? expression NL* RPAREN
      NL* block
    ;

// - do-while 循环，先执行循环体再检查条件
doWhileExpression
    : DO NL* block NL* WHILE NL* LPAREN NL* expression NL* RPAREN
    ;

// 尝试/捕获/最终化表达式规则：
// - 标准 try-finally 结构
tryExpression
    : TRY (NL* | WS+) block NL* FINALLY NL* block
    | TRY NL* block (NL* CATCH (NL* | WS+) LPAREN NL* catchPattern NL* RPAREN NL* block)+ (NL* FINALLY NL* block)?
    | TRY NL* LPAREN NL* resourceSpecifications NL* RPAREN NL* block
        (NL* CATCH NL* LPAREN NL* catchPattern NL* RPAREN NL* block)* (NL* FINALLY NL* block)?
    ;

// 捕获模式规则，可以是通配符或异常类型模式
catchPattern
    : wildcardPattern
    | exceptionTypePattern
    ;

// 异常类型模式规则，用于指定要捕获的异常类型
exceptionTypePattern
    : (WILDCARD | identifier) NL* COLON NL* type (NL* BITOR NL* type)*
    ;

// 资源声明列表规则，在 try-with-resources 语句中使用
resourceSpecifications
    : resourceSpecification (NL* COMMA NL* resourceSpecification)*
    ;

// 单个资源声明规则，包括变量名、可选的类型和初始化表达式
resourceSpecification
    : identifier (NL* COLON NL* classType)? NL* ASSIGN NL* expression
    ;

// 跳转表达式规则，如 throw、return、continue 和 break
jumpExpression
    : THROW NL* expression
    | RETURN (NL* expression)?
    | CONTINUE
    | BREAK
    ;

// 数值类型转换表达式规则，将表达式转换为指定的数值类型
numericTypeConvExpr
    : numericTypes LPAREN NL* expression NL* RPAREN
    ;

// this 或 super 表达式规则
thisSuperExpression
    : THIS
    | SUPER
    ;

// lambda 表达式规则，定义匿名函数
lambdaExpression
    : LCURL NL* lambdaParameters? NL* DOUBLE_ARROW NL* expressionOrDeclarations? RCURL
    ;

// 后置 lambda 表达式规则，允许省略箭头操作符（->）
trailingLambdaExpression
    : LCURL NL* (lambdaParameters? NL* DOUBLE_ARROW NL*)? expressionOrDeclarations? RCURL
    ;

// lambda 参数列表规则，由零个或多个参数组成
lambdaParameters
    : lambdaParameter (NL* COMMA NL* lambdaParameter)*
    ;

// 单个 lambda 参数规则，包括参数名、可选的类型注解
lambdaParameter
    : (identifier | WILDCARD) (NL* COLON NL* type)?
    ;

// 创建并异步执行 lambda 表达式的 spawn 表达式规则
spawnExpression
    : SPAWN (LPAREN NL* expression NL* RPAREN)? NL* trailingLambdaExpression
    ;

// synchronized 关键字用于同步代码块表达式规则
synchronizedExpression
    : SYNCHRONIZED LPAREN NL* expression NL* RPAREN NL* block
    ;

// 圆括号包裹的表达式规则，用于改变优先级或创建单元素组
parenthesizedExpression
    : LPAREN NL* expression NL* RPAREN
    ;

// 代码块结构规则，由大括号包围的一系列表达式或声明
block
    : LCURL expressionOrDeclarations RCURL
    ;

// 不安全代码块表达式规则，通常用于处理原生指针或其他需要特殊权限的操作
unsafeExpression
    : UNSAFE NL* block
    ;

// 表达式或声明列表规则，可以在上下文中交替出现
expressionOrDeclarations
    : end* (expressionOrDeclaration (end+ expressionOrDeclaration?)*)?
    ;
// 表达式或声明规则：允许在当前上下文中定义变量、函数或直接使用表达式
expressionOrDeclaration
    : expression
    | varOrfuncDeclaration
    ;

// 变量或函数定义规则：可以是函数定义或变量声明
varOrfuncDeclaration
    : functionDefinition
    | variableDeclaration
    ;

// 引用（quote）表达式规则，用于表示不进行求值的代码片段
quoteExpression
    : QUOTE quoteExpr
    ;

// 引用表达式的具体内容规则，包含一系列引用参数
quoteExpr
    : LPAREN NL* quoteParameters NL* RPAREN
    ;

// 引用参数列表规则，由一个或多个引用标记、插值表达式或宏表达式组成
quoteParameters
    : (NL* quoteToken | NL* quoteInterpolate | NL* macroExpression)+
    ;


// 引用标记规则，包含所有可能的语法符号和关键字作为不可计算引用
quoteToken
: DOT | COMMA | LPAREN | RPAREN | LSQUARE | RSQUARE | LCURL | RCURL | EXP | MUL
| MOD | DIV | ADD | SUB
| PIPELINE | COMPOSITION
| INC | DEC | AND | OR | NOT | BITAND | BITOR | BITXOR | LSHIFT | RSHIFT |
COLON | SEMI
| ASSIGN | ADD_ASSIGN | SUB_ASSIGN | MUL_ASSIGN | EXP_ASSIGN | DIV_ASSIGN |
 MOD_ASSIGN
| AND_ASSIGN | OR_ASSIGN | BITAND_ASSIGN | BITOR_ASSIGN | BITXOR_ASSIGN |
 LSHIFT_ASSIGN | RSHIFT_ASSIGN
| ARROW | BACKARROW | DOUBLE_ARROW | ELLIPSIS | CLOSEDRANGEOP | RANGEOP | HASH
 | AT | QUEST | UPPERBOUND | LT | GT | LE | GE

| NOTEQUAL | EQUAL | WILDCARD | BACKSLASH | QUOTESYMBOL | DOLLAR
| INT8 | INT16 | INT32 | INT64 | INTNATIVE | UINT8 | UINT16 | UINT32 | UINT64 |
 UINTNATIVE | FLOAT16
| FLOAT32 | FLOAT64 | RUNE | BOOL | UNIT | NOTHING | STRUCT | ENUM | THIS
| PACKAGE | IMPORT | CLASS | INTERFACE | FUNC | LET | VAR | CONST | type
| INIT | THIS | SUPER | IF | ELSE | CASE | TRY | CATCH | FINALLY
| FOR | DO | WHILE | THROW | RETURN | CONTINUE | BREAK | AS | IN
| MATCH | FROM | WHERE | EXTEND | SPAWN | SYNCHRONIZED | MACRO | QUOTE | TRUE |
 FALSE
| STATIC | PUBLIC | PRIVATE | PROTECTED
| OVERRIDE | ABSTRACT | OPEN | OPERATOR | FOREIGN
| Identifier | DollarIdentifier
| literalConstant
;


// 插值引用表达式规则，允许在引用中嵌入可计算表达式
quoteInterpolate
: DOLLAR LPAREN NL* expression NL* RPAREN
;

// 宏表达式规则，包括宏名称、可选的属性表达式以及宏输入表达式
macroExpression
: AT Identifier macroAttrExpr? NL* (macroInputExprWithoutParens | macroInputExprWithParens)
;

// 宏属性表达式规则，由方括号包围的一系列引用标记组成
macroAttrExpr
: LSQUARE NL* quoteToken* NL* RSQUARE
;

// 不带括号的宏输入表达式规则，可以是多种定义或表达式形式
macroInputExprWithoutParens
: functionDefinition
| operatorFunctionDefinition
| staticInit
| structDefinition
| structPrimaryInit
| structInit
| enumDefinition
| caseBody
| classDefinition
| classPrimaryInit
| classInit
| interfaceDefinition
| variableDeclaration
| propertyDefinition
| extendDefinition
| macroExpression
;

// 带括号的宏输入表达式规则，内部是一系列引用标记或宏表达式
macroInputExprWithParens
: LPAREN NL* macroTokens NL* RPAREN
;

// 宏输入中的令牌列表规则，可以是引用标记或嵌套的宏表达式
macroTokens
: (quoteToken | macroExpression)*
;

// 赋值运算符规则，包括基本赋值及其他复合赋值运算符
assignmentOperator
: ASSIGN
| ADD_ASSIGN
| SUB_ASSIGN
| EXP_ASSIGN
| MUL_ASSIGN
| DIV_ASSIGN
| MOD_ASSIGN
| AND_ASSIGN
| OR_ASSIGN
| BITAND_ASSIGN
| BITOR_ASSIGN
| BITXOR_ASSIGN
| LSHIFT_ASSIGN
| RSHIFT_ASSIGN
;

// 等于性运算符规则，包括不等于（!=）和等于（==）
equalityOperator
: NOTEQUAL
| EQUAL
;

// 比较运算符规则，包括小于（<）、大于（>）、小于等于（<=）和大于等于（>=）
comparisonOperator
: LT
| GT
| LE
| GE
;

// 移位运算符规则，包括左移（<<）和右移（>>）
shiftingOperator
: LSHIFT
| RSHIFT
;

// 流程控制运算符规则，这里指管道（pipeline）和组合（composition）操作
flowOperator
: PIPELINE
| COMPOSITION
;

// 加法运算符规则，包括加（+）和减（-）
additiveOperator
: ADD
| SUB
;

// 幂运算符规则，仅包含指数（**）操作
exponentOperator
: EXP
;

// 乘法运算符规则，定义了支持的乘法、除法和取模操作符
multiplicativeOperator
: MUL   // 乘法操作符
| DIV   // 除法操作符
| MOD   // 取模操作符
;

// 前缀一元运算符规则，包括负号（-）和逻辑非（!）操作符
prefixUnaryOperator
: SUB   // 负号前缀减法操作符
| NOT   // 逻辑非操作符
;

// 覆盖加载的运算符规则，这些运算符在特定上下文中可能有不同的含义或功能
overloadedOperators
: LSQUARE RSQUARE    // 下标访问操作符 []
| NOT                 // 逻辑非操作符 !
| ADD                 // 加法操作符 +
| SUB                 // 减法操作符 -
| EXP                 // 幂运算符 **
| MUL                 // 乘法操作符 *
| DIV                 // 除法操作符 /
| MOD                 // 取模操作符 %
| LSHIFT              // 左移位操作符 <<
| RSHIFT              // 右移位操作符 >>
| LT                  // 小于比较操作符 <
| GT                  // 大于比较操作符 >
| LE                  // 小于等于比较操作符 <=
| GE                  // 大于等于比较操作符 >=
| EQUAL               // 等于比较操作符 ==
| NOTEQUAL            // 不等于比较操作符 !=
| BITAND              // 按位与操作符 &
| BITXOR              // 按位异或操作符 ^
| BITOR               // 按位或操作符 |
;

