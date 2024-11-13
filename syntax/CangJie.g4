






grammar CangJie;

quoteExpr
: LPAREN NL* quoteParameters NL* RPAREN
;
quoteParameters
: (NL* quoteToken | NL* quoteInterpolate | NL* macroExpression)+
;
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
  ;
quoteInterpolate
: DOLLAR LPAREN NL* expression NL* RPAREN
;
macroExpression
: AT Identifier macroAttrExpr? NL* (macroInputExprWithoutParens |
  macroInputExprWithParens)
;
