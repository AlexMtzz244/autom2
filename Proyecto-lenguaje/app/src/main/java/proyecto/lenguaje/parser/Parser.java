package proyecto.lenguaje.parser;

import proyecto.lenguaje.lexer.Token;
import java.util.*;

/**
 * Parser simple por descenso recursivo.
 * No cubre toda la sintaxis Haskell, pero permite analizar los ejemplos comunes:
 * - declaraciones simples: name = expr
 * - if ... then ... else ...
 * - let name = expr in expr
 * - llamadas (aplicación) y operadores binarios (asociación izquierda)
 * - listas y tuplas
 */
public class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public AstNode parseProgram() {
        List<AstNode> items = new ArrayList<>();
        while (!isAtEnd()) {
            // intentar parsear declaración (ident = expr) o expresión
            AstNode item = parseTopLevel();
            if (item != null) items.add(item);
            else break;
        }
        return new ProgramNode(items);
    }

    private AstNode parseTopLevel() {
        if (isAtEnd()) return null;
        // if next is identifier and following token is '=', parse decl
        if (peekTypeIs(Token.Type.IDENTIFIER_VAR) && peekNextValueEquals("=")) {
            Token id = advance();
            consumeValue("="); // skip =
            AstNode expr = parseExpression();
            return new DeclNode(id.getValue(), expr);
        }
        // otherwise parse expression
        return parseExpression();
    }

    private AstNode parseExpression() {
        // handle if / let specially
        if (matchKeyword("if")) {
            AstNode cond = parseExpression();
            if (!matchKeyword("then")) throw error("expected 'then' after if condition");
            AstNode thenB = parseExpression();
            if (!matchKeyword("else")) throw error("expected 'else' after then-branch");
            AstNode elseB = parseExpression();
            return new IfNode(cond, thenB, elseB);
        }
        if (matchKeyword("let")) {
            // let name = expr in expr
            if (!peekTypeIs(Token.Type.IDENTIFIER_VAR)) throw error("expected identifier after let");
            String name = advance().getValue();
            consumeValue("=");
            AstNode bound = parseExpression();
            if (!matchKeyword("in")) throw error("expected 'in' after let binding");
            AstNode body = parseExpression();
            return new LetNode(name, bound, body);
        }
        // binary operators with left-assoc simple precedence
        AstNode left = parseApplication();
        while (peekTypeIs(Token.Type.OPERATOR)) {
            String op = advance().getValue();
            AstNode right = parseApplication();
            left = new BinaryOpNode(op, left, right);
        }
        return left;
    }

    // parse function application: left-assoc: primary { primary }
    private AstNode parseApplication() {
        AstNode expr = parsePrimary();
        while (startsPrimary(peek())) {
            AstNode arg = parsePrimary();
            // if expr already an Apply, append arg; else create new Apply
            if (expr instanceof ApplyNode) {
                ApplyNode a = (ApplyNode) expr;
                List<AstNode> newArgs = new ArrayList<>(a.args);
                newArgs.add(arg);
                expr = new ApplyNode(a.function, newArgs);
            } else {
                expr = new ApplyNode(expr, Collections.singletonList(arg));
            }
        }
        return expr;
    }

    private boolean startsPrimary(Token t) {
        if (t == null) return false;
        Token.Type ty = t.getType();
        return ty == Token.Type.IDENTIFIER_VAR || ty == Token.Type.IDENTIFIER_TYPE ||
               ty == Token.Type.INTEGER || ty == Token.Type.FLOAT ||
               ty == Token.Type.STRING || ty == Token.Type.CHAR ||
               ty == Token.Type.BOOLEAN ||
               ty == Token.Type.TUPLE_START || ty == Token.Type.LIST_START;
    }

    private AstNode parsePrimary() {
        if (isAtEnd()) throw error("unexpected end of input");
        Token t = peek();
        // literals and identifiers
        switch (t.getType()) {
            case INTEGER: case FLOAT: case STRING: case CHAR: case BOOLEAN:
                advance();
                return new LiteralNode(t);
            case IDENTIFIER_VAR:
            case IDENTIFIER_TYPE:
                advance();
                return new IdentifierNode(t.getValue());
            case TUPLE_START:
                return parseTupleOrParenExpr();
            case LIST_START:
                return parseList();
            default:
                throw error("unexpected token in primary: " + t.getValue() + " (" + t.getType() + ")");
        }
    }

    private AstNode parseTupleOrParenExpr() {
        consumeType(Token.Type.TUPLE_START);
        // if next is ')' -> empty tuple? treat as empty list/tuple not used; attempt expr parsing
        List<AstNode> elems = new ArrayList<>();
        if (!peekTypeIs(Token.Type.TUPLE_END)) {
            elems.add(parseExpression());
            while (matchValue(",")) {
                elems.add(parseExpression());
            }
        }
        consumeType(Token.Type.TUPLE_END);
        if (elems.size() == 1) {
            // just (expr)
            return elems.get(0);
        } else {
            return new TupleNode(elems);
        }
    }

    private AstNode parseList() {
        consumeType(Token.Type.LIST_START);
        List<AstNode> elems = new ArrayList<>();
        if (!peekTypeIs(Token.Type.LIST_END)) {
            elems.add(parseExpression());
            while (matchValue(",")) {
                elems.add(parseExpression());
            }
        }
        consumeType(Token.Type.LIST_END);
        return new ListNode(elems);
    }

    // --- token helpers ---
    private Token peek() { return pos < tokens.size() ? tokens.get(pos) : null; }
    private boolean isAtEnd() { return pos >= tokens.size(); }
    private Token advance() { return tokens.get(pos++); }
    private boolean peekTypeIs(Token.Type type) { Token p = peek(); return p != null && p.getType() == type; }
    private boolean peekNextValueEquals(String v) {
        Token p = (pos + 1) < tokens.size() ? tokens.get(pos + 1) : null;
        return p != null && v.equals(p.getValue());
    }
    private boolean matchKeyword(String kw) {
        Token p = peek();
        if (p != null && p.getType() == Token.Type.KEYWORD && p.getValue().equals(kw)) {
            advance(); return true;
        }
        return false;
    }
    private boolean matchValue(String v) {
        Token p = peek();
        if (p != null && v.equals(p.getValue())) { advance(); return true; }
        return false;
    }
    private boolean matchType(Token.Type t) {
        Token p = peek();
        if (p != null && p.getType() == t) { advance(); return true; }
        return false;
    }
    private void consumeType(Token.Type t) {
        if (!matchType(t)) throw error("expected token type " + t);
    }
    private void consumeValue(String v) {
        if (!matchValue(v)) throw error("expected '" + v + "'");
    }
    private ParseException error(String msg) {
        Token p = peek();
        String where = (p == null) ? "EOF" : ("line " + p.getLine() + " pos " + p.getPosition());
        return new ParseException(msg + " at " + where);
    }

    public static class ParseException extends RuntimeException {
        ParseException(String m) { super(m); }
    }
}
