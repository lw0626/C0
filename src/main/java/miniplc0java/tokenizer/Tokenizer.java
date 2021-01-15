package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }


    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return null;
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUIntOrDouble();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        } else if (peek == '\''){
            return lexChar();
        } else if (peek == '"'){
            return lexString();
        } else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexUIntOrDouble() throws TokenizeError {
        String token = "";
        boolean isDouble = false;
        Pos startPos = it.currentPos();
        char next = it.peekChar();
        while(Character.isDigit(next)){
            next = it.nextChar();
            token += next;
            next = it.peekChar();
        }
        if(next == '.'){
            next = it.peekChar();
            if(!Character.isDigit(next)){
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
            isDouble = true;
            while(Character.isDigit(next)){
                next = it.nextChar();
                token += next;
                next = it.peekChar();
            }
            if(next == 'e' || next == 'E'){
                next = it.nextChar();
                token += next;
                next = it.peekChar();
                if(next == '+' || next == '-'){
                    next = it.nextChar();
                    token += next;
                    next = it.peekChar();
                }
                if(Character.isDigit(next)){
                    next = it.nextChar();
                    token += next;
                    next = it.peekChar();
                    while (Character.isDigit(next)){
                        next = it.nextChar();
                        token += next;
                        next = it.peekChar();
                    }
                }
                else {
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            }
        }
        Pos endPos = it.currentPos();
        if(isDouble){
            double value = Double.parseDouble(token);
            return new Token(TokenType.DOUBLE_LITERAL, value, startPos, endPos);
        }
        else {
            int value = Integer.parseUnsignedInt(token);
            return new Token(TokenType.UINT_LITERAL, value, startPos, endPos);
        }
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        String token = "";
        Pos startPos = it.currentPos();
        char next = it.peekChar();
        while(Character.isAlphabetic(next) || next == '_' || Character.isDigit(next)){
            next = it.nextChar();
            token += next;
            next = it.peekChar();
        }
        Pos endPos = it.currentPos();
        //是不是关键字，前十个里面
        TokenType tokenType[] = TokenType.values();
        for(int i = 0; i < 10; i++) {
            if (tokenType[i].toString().toLowerCase().equals(token)) {
                return new Token(tokenType[i], token, startPos, endPos);
            }
        }
        //不是关键字，是标识符
        return new Token(TokenType.IDENT,token,startPos,endPos);
    }

    private boolean isEscapeSequence(String str){
        if(str.equals("\\\\") || str.equals("\\\'") || str.equals("\\\"") || str.equals("\\\n") || str.equals("\\\r") || str.equals("\\\t")){
            return true;
        }
        return false;
    }

    private Token lexString() throws TokenizeError {
        String token = "";
        Pos startPos = it.currentPos();
        char next = it.nextChar();
        while (true){
            next = it.peekChar();
            if(next == '"'){
                next = it.nextChar();
                break;
            }
            if(next == '\\'){//偷看到的下一个是反斜杠，考虑转义字符
                token += next;
                next = it.nextChar();
                next = it.peekChar();
                switch (next){
                    case '\\':
                    case '\'':
                    case '\"':
                    case '\n':
                    case '\r':
                    case '\t':
                        token += next;
                        next = it.nextChar();
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            }
            else {
                token += next;
                next = it.nextChar();
            }
        }
        Pos endPos = it.currentPos();

        return new Token(TokenType.IDENT,token,startPos,endPos);
    }

    private Token lexChar() throws TokenizeError {
        String token = "";
        Pos startPos = it.currentPos();
        char next = it.nextChar();
        next = it.peekChar();
        if(next == '\\'){
            token += next;//token是反斜杠
            next = it.nextChar();
            next = it.peekChar();
            token += next;//token是反斜杠加一个字符，判断是不是转义字符
            if(isEscapeSequence(token)){//是转义字符
                next = it.nextChar();
                next = it.peekChar();//next现在是后面的一个‘
            }else {
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
        }else {//不会是转义字符，应该是普通字符
            if(next == '\\' || next == '\''){
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
            token += next;
            next = it.nextChar();
            next = it.peekChar();//next现在是后面的一个‘
        }
        if(next == '\''){
            next = it.nextChar();
            Pos endPos = it.currentPos();
            return new Token(TokenType.CHAR_LITERAL,token,startPos,endPos);
        }else {
            throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
            case '-':
                char op5 = it.peekChar();
                if(op5 == '>'){
                    op5 = it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());
            case '/':
                char op6 = it.peekChar();
                //出现注释的情况
                if(op6 == '/'){
                    op6 = it.nextChar();
                    while (it.nextChar() != '\n');
                }
                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN, '(', it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, ')', it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, ';', it.previousPos(), it.currentPos());
            case '=':
                char op1 = it.peekChar();
                if(op1 == '='){
                    op1 = it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());
            case '!':
                char op2 = it.peekChar();
                if(op2 == '='){
                    op2 = it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            case '>':
                char op3 = it.peekChar();
                if(op3 == '='){
                    op3 = it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.GT, '>', it.previousPos(), it.currentPos());
            case '<':
                char op4 = it.peekChar();
                if(op4 == '='){
                    op4 = it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.LT, '<', it.previousPos(), it.currentPos());
            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
