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
            
            // Manejar espacios en blanco y saltos de línea
            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') {
                    line++;
                }
                pos++;
                continue;
            }
            
            // Manejar comentarios de línea
            if (pos < input.length() - 1 && input.charAt(pos) == '-' && input.charAt(pos + 1) == '-') {
                // Buscar el final de la línea
                int endComment = input.indexOf('\n', pos);
                if (endComment == -1) {
                    endComment = input.length();
                }
                pos = endComment;
                continue;
            }
            
            // Manejar comentarios multilínea
            if (pos < input.length() - 1 && input.charAt(pos) == '{' && input.charAt(pos + 1) == '-') {
                pos += 2;
                int depth = 1;
                while (pos < input.length() - 1 && depth > 0) {
                    if (input.charAt(pos) == '{' && input.charAt(pos + 1) == '-') {
                        depth++;
                        pos += 2;
                    } else if (input.charAt(pos) == '-' && input.charAt(pos + 1) == '}') {
                        depth--;
                        pos += 2;
                    } else {
                        if (input.charAt(pos) == '\n') {
                            line++;
                        }
                        pos++;
                    }
                }
                continue;
            }
            
            // NUEVA LÓGICA: Detectar secuencias que parecen identificadores pero con caracteres inválidos
            if (Character.isLetter(currentChar) || currentChar == '_') {
                String sequence = extractIdentifierSequence(input, pos);
                
                // Verificar si la secuencia contiene caracteres inválidos
                if (containsInvalidChars(sequence)) {
                    tokens.add(new Token(Token.Type.ERROR, sequence, pos, line));
                    pos += sequence.length();
                    continue;
                }
                
                // Si la secuencia es válida, continuar con el matching normal
                // (el bucle de patrones la procesará)
            }
            
            // Intentar hacer match con los patrones regulares
            for (Map.Entry<Token.Type, Pattern> entry : patterns.entrySet()) {
                Matcher m = entry.getValue().matcher(input.substring(pos));
                if (m.lookingAt()) {
                    String tokenValue = m.group();
                    tokens.add(new Token(entry.getKey(), tokenValue, pos, line));
                    pos += m.end();
                    matched = true;
                    break;
                }
            }
            
            // Si no se encontró match, tratar como secuencia de caracteres inválidos
            if (!matched) {
                String invalidSequence = extractInvalidSequence(input, pos, line);
                tokens.add(new Token(Token.Type.ERROR, invalidSequence, pos, line));
                pos += invalidSequence.length();
            }
        }
        return tokens;
    }
    
    // Método para extraer una secuencia que parece un identificador
    private String extractIdentifierSequence(String input, int startPos) {
        StringBuilder sequence = new StringBuilder();
        int pos = startPos;
        
        while (pos < input.length()) {
            char c = input.charAt(pos);
            
            // Parar en espacios en blanco
            if (Character.isWhitespace(c)) {
                break;
            }
            
            // Parar en operadores y separadores válidos específicos
            if (isDefinitiveSeparator(c)) {
                break;
            }
            
            sequence.append(c);
            pos++;
        }
        
        return sequence.toString();
    }
    
    // Método para verificar separadores definitivos (que claramente terminan un identificador)
    private boolean isDefinitiveSeparator(char c) {
        return c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}' ||
               c == ',' || c == ';' || c == '=' || c == ':' || c == '|' || c == '\\' ||
               c == '"' || c == '\''|| c == '\n' || c == '\r' || c == '\t';
    }
    
    // Método mejorado para verificar si un token contiene caracteres inválidos
    private boolean containsInvalidChars(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            // Para el primer carácter, debe ser letra o underscore
            if (i == 0 && !Character.isLetter(c) && c != '_') {
                return true;
            }
            // Para el resto, verificar caracteres inválidos específicos
            if (c == '@' || c == '#' || c == '$' || c == '%' || c == '&' || 
                c == '*' || c == '+' || c == '-' || c == '/' || c == '?' || 
                c == '!' || c == '^' || c == '~' || c == '`' || c == '|' ||
                c == '<' || c == '>' || c == '=' || c == '{' || c == '}' ||
                c == '[' || c == ']' || c == '(' || c == ')' || c == '\\' ||
                c == '"' || c == ';' || c == ':' || c == ',' || c == '.') {
                return true;
            }
        }
        return false;
    }
    
    // Método para extraer secuencia completa de caracteres inválidos
    private String extractInvalidSequence(String input, int startPos, int line) {
        StringBuilder invalidSeq = new StringBuilder();
        int pos = startPos;
        
        // Continuar mientras encontremos caracteres que forman una secuencia problemática
        while (pos < input.length()) {
            char c = input.charAt(pos);
            
            // Parar en espacios en blanco o saltos de línea
            if (Character.isWhitespace(c)) {
                break;
            }
            
            // Parar en separadores válidos definitivos
            if (isDefinitiveSeparator(c)) {
                break;
            }
            
            invalidSeq.append(c);
            pos++;
        }
        
        return invalidSeq.length() > 0 ? invalidSeq.toString() : String.valueOf(input.charAt(startPos));
    }
}
