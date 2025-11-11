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
    private List<String> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public AstNode parseProgram() {
        List<AstNode> items = new ArrayList<>();
        errors.clear(); // Limpiar errores anteriores
        
        while (!isAtEnd()) {
            try {
                // intentar parsear declaración (ident = expr) o expresión
                AstNode item = parseTopLevel();
                if (item != null) {
                    items.add(item);
                } else {
                    // Si no se puede parsear, avanzar para evitar bucle infinito
                    advance();
                }
            } catch (ParseException ex) {
                // Capturar error y continuar
                errors.add(ex.getMessage());
                // Intentar recuperarse avanzando hasta el siguiente token potencial
                recoverFromError();
            }
        }
        
        // Si hay errores, lanzar excepción con todos los errores
        if (!errors.isEmpty()) {
            StringBuilder allErrors = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                allErrors.append("Error ").append(i + 1).append(": ").append(errors.get(i));
                if (i < errors.size() - 1) {
                    allErrors.append("\n");
                }
            }
            throw new ParseException(allErrors.toString());
        }
        
        return new ProgramNode(items);
    }
    
    private void recoverFromError() {
        // Estrategia de recuperación: avanzar hasta encontrar un token que podría iniciar una nueva declaración
        while (!isAtEnd()) {
            Token current = peek();
            if (current.getType() == Token.Type.IDENTIFIER_VAR) {
                // Verificar si el siguiente es '=' para una posible declaración
                Token next = (pos + 1) < tokens.size() ? tokens.get(pos + 1) : null;
                if (next != null && "=".equals(next.getValue())) {
                    break; // Posible inicio de nueva declaración
                }
            }
            advance();
        }
    }

    private AstNode parseTopLevel() {
        if (isAtEnd()) return null;
        
        // if next is identifier and following token is '=', parse decl
        if (peekTypeIs(Token.Type.IDENTIFIER_VAR) && peekNextValueEquals("=")) {
            Token id = advance();
            consumeValue("="); // skip =
            try {
                AstNode expr = parseExpression();
                return new DeclNode(id.getValue(), expr);
            } catch (ParseException ex) {
                // Re-lanzar con contexto de la declaración
                throw new ParseException("En declaración de '" + id.getValue() + "': " + ex.getMessage());
            }
        }
        
        // otherwise parse expression
        try {
            return parseExpression();
        } catch (ParseException ex) {
            // Re-lanzar con contexto
            throw new ParseException("En expresión: " + ex.getMessage());
        }
    }

    private AstNode parseExpression() {
        // handle if / let / cycles specially
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
        
        // Handle cycle structures (while/for/loop)
        if (matchKeyword("while")) {
            return parseCycle(CycleNode.CycleType.WHILE);
        }
        if (matchKeyword("for")) {
            return parseCycle(CycleNode.CycleType.FOR);
        }
        if (matchKeyword("loop") || matchKeyword("ciclo")) {
            return parseCycle(CycleNode.CycleType.LOOP);
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
        
        // cycle keywords count as primaries as they start a cycle expression
        if (ty == Token.Type.KEYWORD) {
            String keyword = t.getValue();
            return keyword.equals("while") || keyword.equals("for") ||
                   keyword.equals("loop") || keyword.equals("ciclo");
        }
        
        // Allow unary - to start a primary (unary negation)
        if (ty == Token.Type.OPERATOR) {
            String v = t.getValue();
            if ("-".equals(v)) return true;
        }

     return ty == Token.Type.IDENTIFIER_VAR || ty == Token.Type.IDENTIFIER_TYPE ||
         ty == Token.Type.INTEGER || ty == Token.Type.FLOAT ||
         ty == Token.Type.STRING || ty == Token.Type.CHAR ||
         ty == Token.Type.BOOLEAN ||
         ty == Token.Type.TUPLE_START || ty == Token.Type.LIST_START;
    }

    private AstNode parsePrimary() {
        if (isAtEnd()) throw error("unexpected end of input");
        Token t = peek();
        
        // NUEVO: Manejo de operadores unarios (negación con -)
        if (t.getType() == Token.Type.OPERATOR && t.getValue().equals("-")) {
            advance(); // consumir el operador -
            AstNode operand = parsePrimary(); // parsear recursivamente el operando
            // Crear un nodo de operación unaria (negación)
            return new UnaryOpNode("-", operand);
        }
        
        // Check for cycle keywords first
        if (t.getType() == Token.Type.KEYWORD) {
            String keyword = t.getValue();
            if (keyword.equals("while") || keyword.equals("for") || keyword.equals("loop") || keyword.equals("ciclo")) {
                // consume the keyword and delegate to parseCycle
                Token kw = advance();
                CycleNode.CycleType type;
                if ("while".equals(kw.getValue())) type = CycleNode.CycleType.WHILE;
                else if ("for".equals(kw.getValue())) type = CycleNode.CycleType.FOR;
                else type = CycleNode.CycleType.LOOP;
                return parseCycle(type, kw);
            }
        }
        
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

    // Unified cycle parser producing CycleNode
    private AstNode parseCycle(CycleNode.CycleType type, Token kw) {
        // expect '('
        if (!matchType(Token.Type.TUPLE_START) && !matchSymbol("(")) {
            throw error("expected '(' after '" + kw.getValue() + "'");
        }

        AstNode init = null;
        AstNode condition = null;
        AstNode update = null;

    if (type == CycleNode.CycleType.FOR) {
            // for ( init ; cond ; update )
            // init (optional)
            if (!peekValueEquals(";") && !peekTypeIs(Token.Type.TUPLE_END)) {
                init = parseExpression();
            }
            // expect ';'
            consumeValue(";");

            // cond (optional)
            if (!peekValueEquals(";") && !peekTypeIs(Token.Type.TUPLE_END)) {
                condition = parseExpression();
            }
            consumeValue(";");

            // update (optional)
            if (!peekTypeIs(Token.Type.TUPLE_END)) {
                update = parseExpression();
            }
        } else {
            // while/loop: single condition expression
            condition = parseExpression();
        }

        // expect ')'
        if (!matchType(Token.Type.TUPLE_END) && !matchSymbol(")")) {
            throw error("expected ')' to close cycle header");
        }

        List<AstNode> body = parseBlock();
        return new CycleNode(type, init, condition, update, body);
    }

    // Overload used when the keyword was already consumed (last token)
    private AstNode parseCycle(CycleNode.CycleType type) {
        Token kw = null;
        if (pos > 0 && pos - 1 < tokens.size()) kw = tokens.get(pos - 1);
        return parseCycle(type, kw);
    }
    
    private List<AstNode> parseBlock() {
        // Consume opening brace
        if (!matchSymbol("{")) {
            throw error("expected '{' to start block");
        }
        
        List<AstNode> statements = new ArrayList<>();
        
        while (!peekSymbolEquals("}") && !isAtEnd()) {
            AstNode stmt = parseTopLevel(); // Parse statement or expression
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        
        // Consume closing brace
        if (!matchSymbol("}")) {
            throw error("expected '}' to close block");
        }
        
        return statements;
    }
    
    private boolean peekValueEquals(String value) {
        Token p = peek();
        return p != null && value.equals(p.getValue());
    }
    
    private boolean peekSymbolEquals(String symbol) {
        Token p = peek();
        return p != null && p.getType() == Token.Type.SYMBOL && symbol.equals(p.getValue());
    }
    
    private boolean matchSymbol(String symbol) {
        Token p = peek();
        if (p != null && p.getType() == Token.Type.SYMBOL && symbol.equals(p.getValue())) {
            advance();
            return true;
        }
        return false;
    }

    public static class ParseException extends RuntimeException {
        ParseException(String m) { super(m); }
    }
}
