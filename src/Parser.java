import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.Class)) {
                return classDeclaration();
            } else if (match(TokenType.Fun)) {
                return function("function");
            } else if (match(TokenType.Var)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(TokenType.Identifier, "Expect class name.");
        consume(TokenType.LeftBrace, "Expect `{` before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.RightBrace) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(TokenType.RightBrace, "Expect `}` after class body.");

        return new Stmt.Class(name, methods);
    }

    private Stmt statement() {
        if (match(TokenType.For)) {
            return forStatement();
        } else if (match(TokenType.If)) {
            return ifStatement();
        } else if (match(TokenType.Print)) {
            return printStatement();
        } else if (match(TokenType.Return)) {
            return returnStatement();
        } else if (match(TokenType.While)) {
            return whileStatement();
        } else if (match(TokenType.LeftBrace)) {
            return new Stmt.Block(block());
        }

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(TokenType.LeftParen, "Expect `(` after `for`.");

        Stmt initializer;
        if (match(TokenType.Semicolon)) {
            initializer = null;
        } else if (match(TokenType.Var)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.Semicolon)) {
            condition = expression();
        }
        consume(TokenType.Semicolon, "Expect `;` after loop condition.");

        Expr increment = null;
        if (!check(TokenType.RightParen)) {
            increment = expression();
        }
        consume(TokenType.RightParen, "Expect `)` after for clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
                )
            );
        }

        if (condition == null) {
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(TokenType.LeftParen, "Expect `(` after `if`.");
        Expr condition = expression();
        consume(TokenType.RightParen, "Expect `)` after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.Else)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.Semicolon, "Expect `;` after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.Semicolon)) {
            value = expression();
        }

        consume(TokenType.Semicolon, "Expect `;` after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.Identifier, "Expect variable name.");

        Expr initializer = null;
        if (match(TokenType.Equal)) {
            initializer = expression();
        }

        consume(TokenType.Semicolon, "Expect `;` after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(TokenType.LeftParen, "Expect `(` after `while`.");
        Expr condition = expression();
        consume(TokenType.RightParen, "Expect `)` after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(TokenType.Identifier, "Expect " + kind + " name.");
        consume(TokenType.LeftParen, "Expect `(` after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RightParen)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(consume(TokenType.Identifier, "Expect parameter name."));
            } while (match(TokenType.Comma));
        }
        consume(TokenType.RightParen, "Expect `)` after parameters.");

        consume(TokenType.LeftBrace, "Expect `{` before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.Semicolon, "Expect `;` after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RightBrace) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RightBrace, "Expect `}` after block.");
        return statements;
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.Equal)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(TokenType.Or)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.And)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.BangEqual, TokenType.EqualEqual)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(TokenType.Greater, TokenType.GreaterEqual, TokenType.Less, TokenType.LessEqual)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.Minus, TokenType.Plus)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(TokenType.Slash, TokenType.Star)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.Plus, TokenType.Minus)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RightParen)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(TokenType.Comma));
        }

        Token paren = consume(TokenType.RightParen, "Expect `)` after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LeftParen)) {
                expr = finishCall(expr);
            } else if (match(TokenType.Dot)) {
                Token name = consume(TokenType.Identifier, "Expect property name after `.`.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr primary() {
        if (match(TokenType.False)) {
            return new Expr.Literal(false);
        } else if (match(TokenType.True)) {
            return new Expr.Literal(true);
        } else if (match(TokenType.Nil)) {
            return new Expr.Literal(null);
        } else if (match(TokenType.Number, TokenType.String)) {
            return new Expr.Literal(previous().literal);
        } else if (match(TokenType.This)) {
            return new Expr.This(previous());
        } else if (match(TokenType.Identifier)) {
            return new Expr.Variable(previous());
        } else if (match(TokenType.LeftParen)) {
            Expr expr = expression();
            consume(TokenType.RightParen, "Expect `)` after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.Semicolon) {
                return;
            }

            switch (peek().type) {
                case TokenType.Class:
                case TokenType.Fun:
                case TokenType.Var:
                case TokenType.For:
                case TokenType.If:
                case TokenType.While:
                case TokenType.Print:
                case TokenType.Return:
                    return;
                default:
                    break;
            }

            advance();
        }
    }
}
