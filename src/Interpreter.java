class Interpreter implements Expr.Visitor<Object> {
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case TokenType.Greater: {
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            }
            case TokenType.GreaterEqual: {
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            }
            case TokenType.Less: {
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            }
            case TokenType.LessEqual: {
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            }
            case TokenType.BangEqual: {
                return !isEqual(left, right);
            }
            case TokenType.EqualEqual: {
                return isEqual(left, right);
            }
            case TokenType.Minus: {
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            }
            case TokenType.Plus: {
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
            case TokenType.Slash: {
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            }
            case TokenType.Star: {
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            }
            default:
                break;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case TokenType.Bang: {
                return !isTruthy(right);
            }
            case TokenType.Minus: {
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            }
            default:
                break;
        }

        // Unreachable.
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        } else if (object instanceof Boolean) {
            return (boolean)object;
        } else {
            return true;
        }
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}
