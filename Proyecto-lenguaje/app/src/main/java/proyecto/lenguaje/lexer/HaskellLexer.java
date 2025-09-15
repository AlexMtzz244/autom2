package proyecto.lenguaje.lexer;

import java.util.*;
import java.util.regex.*;

public class HaskellLexer {
    private static final Map<Token.Type, Pattern> patterns = new LinkedHashMap<>();
    static {
        // Keywords de Haskell (incluye tipos básicos y ciclos hipotéticos)
        patterns.put(Token.Type.KEYWORD, Pattern.compile("\\b(let|in|if|then|else|case|of|data|type|where|module|import|deriving|class|instance|newtype|do|default|foreign|forall|hiding|qualified|as|family|role|pattern|static|stock|anyclass|via|Int|Integer|Float|Double|Bool|Char|String|while|for|loop|ciclo)\\b"));
        
        // Booleanos
        patterns.put(Token.Type.BOOLEAN, Pattern.compile("\\b(True|False)\\b"));
        
        // Identificadores de variables y funciones (comienzan con minúscula)
        patterns.put(Token.Type.IDENTIFIER_VAR, Pattern.compile("\\b[a-z][a-zA-Z0-9_']*\\b"));
        
        // Identificadores de tipos y constructores (comienzan con mayúscula)
        patterns.put(Token.Type.IDENTIFIER_TYPE, Pattern.compile("\\b[A-Z][a-zA-Z0-9_']*\\b"));
        
        // Números enteros (incluye negativos, hexadecimal, octal y binario)
        patterns.put(Token.Type.INTEGER, Pattern.compile("-?\\b\\d+\\b|\\b0[xX][0-9a-fA-F]+\\b|\\b0[oO][0-7]+\\b|\\b0[bB][01]+\\b"));
        
        // Números decimales (incluye notación científica)
        patterns.put(Token.Type.FLOAT, Pattern.compile("-?\\b\\d+\\.\\d+(?:[eE][-+]?\\d+)?\\b"));
        
        // Strings (con escape sequences)
        patterns.put(Token.Type.STRING, Pattern.compile("\"([^\"\\\\]|\\\\.)*\""));
        
        // Caracteres (con escape sequences)
        patterns.put(Token.Type.CHAR, Pattern.compile("'([^'\\\\]|\\\\.)'"));
        // Operadores válidos en Haskell
        patterns.put(Token.Type.OPERATOR, Pattern.compile("(\\+\\+|\\.|::|->|<-|<=|>=|==|/=|&&|\\|\\||\\$|[-+*/=<>:|&!])+"));
        
        // Símbolos para listas y tuplas
        patterns.put(Token.Type.LIST_START, Pattern.compile("\\["));
        patterns.put(Token.Type.LIST_END, Pattern.compile("\\]"));
        patterns.put(Token.Type.TUPLE_START, Pattern.compile("\\("));
        patterns.put(Token.Type.TUPLE_END, Pattern.compile("\\)"));
        
        // Otros símbolos de separación
        patterns.put(Token.Type.SYMBOL, Pattern.compile("[,;{}]"));
    }

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int line = 1;
        while (pos < input.length()) {
            boolean matched = false;
            char currentChar = input.charAt(pos);
            
            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') {
                    line++;
                }
                pos++;
                continue;
            }
            
            for (Map.Entry<Token.Type, Pattern> entry : patterns.entrySet()) {
                Matcher m = entry.getValue().matcher(input.substring(pos));
                if (m.lookingAt()) {
                    tokens.add(new Token(entry.getKey(), m.group(), pos, line));
                    pos += m.end();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                tokens.add(new Token(Token.Type.ERROR, String.valueOf(input.charAt(pos)), pos, line));
                pos++;
            }
        }
        return tokens;
    }
}
