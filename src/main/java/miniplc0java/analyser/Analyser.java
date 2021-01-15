package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        return instructions;
    }

    /**
     * 查看下一个 Token
     * 调用 peek 后第一次调用 next 会返回与之前相同的 token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 输出并前进一个token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 偷看下一个 token，如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 相当于先调用 check 再调用 next
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 相当于会抛异常版的 nextIf
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     * 
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     * 
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
        }
    }

    /**
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

        /*表达式
    expr ->
          expr binary_operator expr
        | '-' expr
        | IDENT '=' expr
        | expr 'as' IDENT
        | IDENT '(' call_param_list? ')'
        | UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL      //字面量
        | IDENT
        |  '(' expr ')'

    call_param_list -> expr (',' expr)*
    binary_operator -> '+' | '-' | '*' | '/' | '==' | '!=' | '<' | '>' | '<=' | '>='*/
    private void analyseExpr() throws CompileError {
        /*'-' expr*/
        if(check(TokenType.MINUS)){
            next();
            analyseExpr();
        }
        /*UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL      //字面量*/
        else if(check(TokenType.UINT_LITERAL)){
            next();
        }
        else if(check(TokenType.DOUBLE_LITERAL)){
            next();
        }
        else if(check(TokenType.STRING_LITERAL)){
            next();
        }
        else if(check(TokenType.CHAR_LITERAL)){
            next();
        }
        /*IDENT*/
        else if(check(TokenType.IDENT)){
            next();
            /*IDENT '=' expr*/
            if(check(TokenType.ASSIGN)){
                next();
                analyseExpr();
            }
            /*IDENT '(' call_param_list? ')'*/
            else if(check(TokenType.L_PAREN)){
                next();
                analyseParamList();
            }
            expect(TokenType.R_PAREN);
        }
        else if(check(TokenType.L_PAREN)){
            next();
            analyseExpr();
            expect(TokenType.R_PAREN);
        }
        while(analyseBinaryOperator()||check(TokenType.AS_KW)){
            if(analyseBinaryOperator()){
                next();
                analyseExpr();
            }
            else if(check(TokenType.AS_KW)){
                expect(TokenType.IDENT);
            }
        }
    }

    private void analyseParamList() throws CompileError {
        analyseExpr();
        while (check(TokenType.COMMA)){
            next();
            analyseExpr();
        }
    }

    //binary_operator -> '+' | '-' | '*' | '/' | '==' | '!=' | '<' | '>' | '<=' | '>='
    private boolean analyseBinaryOperator() throws CompileError {
        if(check(TokenType.PLUS)||check(TokenType.MINUS)||
                check(TokenType.MUL)||check(TokenType.DIV)||
                check(TokenType.ASSIGN)||check(TokenType.EQ)||
                check(TokenType.NEQ)||check(TokenType.LT)||check(TokenType.GT)
                ||check(TokenType.LE)||check(TokenType.GE)) {
            return true;
        }
        return false;
    }
        /*// # 语句
    stmt ->
          expr ';'
        | decl_stmt
        | 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
        | 'while' expr block_stmt
        | 'break' ';'
        | 'continue' ';'
        | 'return' expr? ';'
        | block_stmt
        | ';'

    decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';' | 'const' IDENT ':' ty '=' expr ';'

    block_stmt -> '{' stmt* '}'*/

    private void analyseStmt() throws CompileError {
        if(check(TokenType.IF_KW)){
            next();
            analyseExpr();
            analyseBlockStmt();
            while(check(TokenType.ELSE_KW)){
                next();
                if(check(TokenType.IF_KW)){
                    next();
                    analyseExpr();
                    analyseBlockStmt();
                }
                else {
                    analyseBlockStmt();
                    break;
                }
            }
        }
        else if(check(TokenType.WHILE_KW)){
            analyseExpr();
            analyseBlockStmt();
        }
        else if(check(TokenType.BREAK_KW)){
            next();
            expect(TokenType.SEMICOLON);
        }
    }

    private void analyseBlockStmt() throws CompileError {
        expect(TokenType.L_BRACE);
        while (!check(TokenType.R_BRACE)){
            analyseStmt();
        }
        expect(TokenType.R_BRACE);
    }

}
