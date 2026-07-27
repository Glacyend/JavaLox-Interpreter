import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", TokenType.And);
        keywords.put("class", TokenType.Class);
        keywords.put("else", TokenType.Else);
        keywords.put("false", TokenType.False);
        keywords.put("for", TokenType.For);
        keywords.put("fun", TokenType.Fun);
        keywords.put("if", TokenType.If);
        keywords.put("nil", TokenType.Nil);
        keywords.put("or", TokenType.Or);
        keywords.put("print", TokenType.Print);
        keywords.put("return", TokenType.Return);
        keywords.put("super", TokenType.Super);
        keywords.put("this", TokenType.This);
        keywords.put("true", TokenType.True);
        keywords.put("var", TokenType.Var);
        keywords.put("while", TokenType.While);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': {
                addToken(TokenType.LeftParen);
                break;
            }
            case ')': {
                addToken(TokenType.RightParen);
                break;
            }
            case '{': {
                addToken(TokenType.LeftBrace);
                break;
            }
            case '}': {
                addToken(TokenType.RightBrace);
                break;
            }
            case ',': {
                addToken(TokenType.Comma);
                break;
            }
            case '.': {
                addToken(TokenType.Dot);
                break;
            }
            case '-': {
                addToken(TokenType.Minus);
                break;
            }
            case '+': {
                addToken(TokenType.Plus);
                break;
            }
            case ';': {
                addToken(TokenType.Semicolon);
                break;
            }
            case '*': {
                addToken(TokenType.Star);
                break;
            }
            case '!': {
                addToken(match('=') ? TokenType.BangEqual : TokenType.Bang);
                break;
            }
            case '=': {
                addToken(match('=') ? TokenType.EqualEqual : TokenType.Equal);
                break;
            }
            case '<': {
                addToken(match('=') ? TokenType.LessEqual : TokenType.Less);
                break;
            }
            case '>': {
                addToken(match('=') ? TokenType.GreaterEqual : TokenType.Greater);
                break;
            }
            case '/': {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else {
                    addToken(TokenType.Slash);
                }
            }
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n': {
                line++;
                break;
            }
            case '"': {
                string();
                break;
            }
            default: {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = TokenType.Identifier;
        }
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        if (peek() == '.' && isDigit(peekNext())) {
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(TokenType.Number, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance();

        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.String, value);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean match(char expected) {
        if (isAtEnd()) {
            return false;
        }
        if (source.charAt(current) != expected) {
            return false;
        }
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
