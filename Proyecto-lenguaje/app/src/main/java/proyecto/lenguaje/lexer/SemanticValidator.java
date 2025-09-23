package proyecto.lenguaje.lexer;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class SemanticValidator {
    private static final Map<String, String> variableTypes = new HashMap<>();
    
    public static String validateCycles(List<Token> tokens) {
        StringBuilder errors = new StringBuilder();
        StringBuilder info = new StringBuilder();
        int cycleCount = 0;

        // Primero, recopilamos todas las variables y sus tipos
        initializeVariableTypes(tokens);

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);

            // Detecta palabras clave que podrían representar ciclos
            if (t.getType() == Token.Type.KEYWORD && isCycleKeyword(t.getValue())) {
                cycleCount++;
                info.append("Ciclo detectado: '").append(t.getValue())
                    .append("' en línea ").append(t.getLine())
                    .append(", posición ").append(t.getPosition()).append("\n");

                // Validar estructura completa del ciclo
                validateCycleStructure(tokens, i, t, errors, info);
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("=== VALIDACIÓN SEMÁNTICA DE CICLOS ===\n");
        result.append("Total de ciclos detectados: ").append(cycleCount).append("\n\n");

        if (cycleCount == 0) {
            result.append("No se detectaron ciclos en el código.\n");
            result.append("Nota: El código utiliza solo estructuras funcionales válidas.\n");
        } else {
            result.append("--- INFORMACIÓN DE CICLOS ---\n");
            result.append(info.toString()).append("\n");

            if (errors.length() > 0) {
                result.append("--- ERRORES SEMÁNTICOS ENCONTRADOS ---\n");
                result.append(errors.toString());
            } else {
                result.append("✅ Todos los ciclos están bien formados semánticamente.\n");
                result.append("Estructura de condiciones y bloques correcta.\n");
                result.append("Variables y tipos validados correctamente.\n");
            }
        }

        return result.toString();
    }

    private static void validateCycleStructure(List<Token> tokens, int cycleIndex, Token cycleToken, 
                                             StringBuilder errors, StringBuilder info) {
        String cycleType = cycleToken.getValue();
        int line = cycleToken.getLine();

        // Buscar paréntesis de apertura
        int parenStart = findNextToken(tokens, cycleIndex + 1, Token.Type.TUPLE_START, "(");
        if (parenStart == -1) {
            errors.append("ERROR: Ciclo '").append(cycleType)
                  .append("' en línea ").append(line)
                  .append(" no tiene paréntesis de apertura '(' para la condición.\n");
            return;
        }

        // Buscar paréntesis de cierre
        int parenEnd = findMatchingCloseParen(tokens, parenStart);
        if (parenEnd == -1) {
            errors.append("ERROR: Ciclo '").append(cycleType)
                  .append("' en línea ").append(line)
                  .append(" no tiene paréntesis de cierre ')' para la condición.\n");
            return;
        }

        // Buscar llaves del bloque
        int braceStart = findNextToken(tokens, parenEnd + 1, Token.Type.SYMBOL, "{");
        if (braceStart == -1) {
            errors.append("ERROR: Ciclo '").append(cycleType)
                  .append("' en línea ").append(line)
                  .append(" no tiene llave de apertura '{' para el bloque de código.\n");
            return;
        }

        int braceEnd = findMatchingCloseBrace(tokens, braceStart);
        if (braceEnd == -1) {
            errors.append("ERROR: Ciclo '").append(cycleType)
                  .append("' en línea ").append(line)
                  .append(" no tiene llave de cierre '}' para el bloque de código.\n");
            return;
        }

        // Extraer tokens de la condición/inicialización
        List<Token> headerTokens = extractTokens(tokens, parenStart + 1, parenEnd);
        List<Token> bodyTokens = extractTokens(tokens, braceStart + 1, braceEnd);

        // Validar según el tipo de ciclo
        if ("for".equals(cycleType)) {
            validateForCycle(headerTokens, bodyTokens, line, errors, info);
        } else if ("while".equals(cycleType) || "loop".equals(cycleType) || "ciclo".equals(cycleType)) {
            validateWhileCycle(headerTokens, bodyTokens, line, errors, info);
        }
    }

    private static void validateForCycle(List<Token> headerTokens, List<Token> bodyTokens, 
                                       int line, StringBuilder errors, StringBuilder info) {
        info.append("  Validando estructura FOR en línea ").append(line).append("\n");

        // Dividir la cabecera del for por punto y coma
        List<List<Token>> forParts = splitByDelimiter(headerTokens, ";");
        
        if (forParts.size() != 3) {
            errors.append("ERROR: FOR en línea ").append(line)
                  .append(" debe tener exactamente 3 partes separadas por ';' (inicialización; condición; incremento). ")
                  .append("Encontradas: ").append(forParts.size()).append(" partes.\n");
            return;
        }

        List<Token> initTokens = forParts.get(0);
        List<Token> condTokens = forParts.get(1);
        List<Token> incrTokens = forParts.get(2);

        // Validar inicialización
        if (initTokens.isEmpty()) {
            errors.append("ERROR: FOR en línea ").append(line)
                  .append(" no tiene inicialización. Ejemplo: 'i = 0'\n");
        } else {
            validateAssignment(initTokens, line, errors, "inicialización del FOR");
        }

        // Validar condición
        if (condTokens.isEmpty()) {
            errors.append("ERROR: FOR en línea ").append(line)
                  .append(" no tiene condición. Ejemplo: 'i < 10'\n");
        } else {
            validateConditionExpression(condTokens, line, errors, "condición del FOR");
        }

        // Validar incremento
        if (incrTokens.isEmpty()) {
            errors.append("ERROR: FOR en línea ").append(line)
                  .append(" no tiene incremento. Ejemplo: 'i = i + 1'\n");
        } else {
            validateAssignment(incrTokens, line, errors, "incremento del FOR");
        }

        // Validar cuerpo del ciclo
        validateCycleBody(bodyTokens, line, errors, info);
    }

    private static void validateWhileCycle(List<Token> headerTokens, List<Token> bodyTokens, 
                                         int line, StringBuilder errors, StringBuilder info) {
        info.append("  Validando estructura WHILE/LOOP en línea ").append(line).append("\n");

        if (headerTokens.isEmpty()) {
            errors.append("ERROR: WHILE/LOOP en línea ").append(line)
                  .append(" no tiene condición. Ejemplo: 'x > 0'\n");
            return;
        }

        // Validar condición
        validateConditionExpression(headerTokens, line, errors, "condición del WHILE/LOOP");

        // Validar cuerpo del ciclo
        validateCycleBody(bodyTokens, line, errors, info);
    }

    private static void validateAssignment(List<Token> tokens, int line, StringBuilder errors, String context) {
        if (tokens.size() < 3) {
            errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                  .append(" incompleta. Se esperaba: variable = valor\n");
            return;
        }

        Token variable = tokens.get(0);
        Token equals = tokens.get(1);
        
        // Validar que sea un identificador
        if (variable.getType() != Token.Type.IDENTIFIER_VAR && variable.getType() != Token.Type.IDENTIFIER_TYPE) {
            errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                  .append(" debe comenzar con una variable válida, no '").append(variable.getValue()).append("'\n");
            return;
        }

        // Validar operador de asignación
        if (!"=".equals(equals.getValue())) {
            errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                  .append(" debe usar '=' para asignación, no '").append(equals.getValue()).append("'\n");
            return;
        }

        // Validar la expresión del lado derecho
        List<Token> rightSide = tokens.subList(2, tokens.size());
        validateExpression(rightSide, line, errors, context + " - lado derecho");

        // Verificar compatibilidad de tipos con validación estricta
        String varName = variable.getValue();
        String previousType = variableTypes.get(varName);
        
        if (rightSide.size() >= 1) {
            String assignedType = inferExpressionType(rightSide);
            
            // Validación adicional: verificar cada token individual para detectar mezclas de tipos
            for (Token token : rightSide) {
                String tokenType = inferType(token);
                if (!tokenType.equals("unknown") && !tokenType.equals(assignedType)) {
                    // Detectar mezcla de tipos en la expresión
                    if ((assignedType.equals("numeric") && tokenType.equals("string")) ||
                        (assignedType.equals("string") && tokenType.equals("numeric"))) {
                        errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                              .append(" - MEZCLA DE TIPOS INCOMPATIBLES. Expresión contiene tanto valores numéricos como texto. ")
                              .append("Token '").append(token.getValue()).append("' es de tipo ").append(tokenType)
                              .append(" pero la expresión se evaluó como ").append(assignedType).append("\n");
                    }
                }
            }
            
            if (previousType != null) {
                // La variable ya fue declarada anteriormente
                if (!isCompatibleType(previousType, assignedType)) {
                    errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                          .append(" - INCOMPATIBILIDAD DE TIPOS. Variable '").append(varName)
                          .append("' fue declarada como tipo '").append(previousType)
                          .append("' pero se intenta asignar un valor de tipo '").append(assignedType).append("'.\n")
                          .append("  Ejemplo de valor esperado para tipo '").append(previousType).append("': ")
                          .append(getExampleValue(previousType)).append("\n")
                          .append("  Valor actual detectado como tipo '").append(assignedType).append("': ");
                    
                    // Mostrar los tokens problemáticos
                    for (int i = 0; i < rightSide.size(); i++) {
                        errors.append(rightSide.get(i).getValue());
                        if (i < rightSide.size() - 1) errors.append(" ");
                    }
                    errors.append("\n");
                } else {
                    // Tipo compatible, pero reportar información útil si hay conversión
                    if (!previousType.equals(assignedType) && !assignedType.equals("unknown")) {
                        errors.append("INFO: ").append(context).append(" en línea ").append(line)
                              .append(" - Variable '").append(varName).append("' cambió de tipo '")
                              .append(previousType).append("' a '").append(assignedType)
                              .append("' (conversión automática permitida).\n");
                    }
                }
            } else {
                // Primera asignación de la variable
                variableTypes.put(varName, assignedType);
            }
            
            // Validación adicional para asignaciones numéricas
            if ("numeric".equals(assignedType) && rightSide.size() > 2) {
                validateNumericExpression(rightSide, line, errors, context);
            }
        }
    }

    private static void validateConditionExpression(List<Token> tokens, int line, StringBuilder errors, String context) {
        if (tokens.isEmpty()) {
            errors.append("ERROR: ").append(context).append(" en línea ").append(line).append(" está vacía\n");
            return;
        }

        // Validar que todas las variables estén definidas
        for (Token token : tokens) {
            if ((token.getType() == Token.Type.IDENTIFIER_VAR || token.getType() == Token.Type.IDENTIFIER_TYPE) 
                && !variableTypes.containsKey(token.getValue())) {
                errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                      .append(" usa variable no definida: '").append(token.getValue()).append("'\n");
            }
        }

        // Validar operadores de comparación y compatibilidad de tipos
        boolean hasComparisonOp = false;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == Token.Type.SYMBOL || token.getType() == Token.Type.OPERATOR) {
                String op = token.getValue();
                if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=") || 
                    op.equals("==") || op.equals("/=")) {
                    hasComparisonOp = true;
                    
                    // Validar tipos de los operandos
                    if (i > 0 && i < tokens.size() - 1) {
                        Token leftToken = tokens.get(i - 1);
                        Token rightToken = tokens.get(i + 1);
                        
                        String leftType = inferType(leftToken);
                        String rightType = inferType(rightToken);
                        
                        // Validar que los tipos sean compatibles para comparación
                        if (!leftType.equals("unknown") && !rightType.equals("unknown")) {
                            if (!areComparableTypes(leftType, rightType)) {
                                errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                                      .append(" - INCOMPATIBILIDAD DE TIPOS en comparación. ")
                                      .append("No se puede comparar '").append(leftToken.getValue())
                                      .append("' (tipo: ").append(leftType).append(") ")
                                      .append("con '").append(rightToken.getValue())
                                      .append("' (tipo: ").append(rightType).append(") ")
                                      .append("usando el operador '").append(op).append("'.\n")
                                      .append("  Los tipos deben ser compatibles para realizar comparaciones.\n");
                            }
                            
                            // Validación especial para operadores de orden (<, >, <=, >=)
                            if ((op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) &&
                                (!leftType.equals("numeric") || !rightType.equals("numeric"))) {
                                errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                                      .append(" - Los operadores de orden (").append(op)
                                      .append(") solo pueden usarse con valores numéricos. ")
                                      .append("Encontrado: '").append(leftToken.getValue()).append("' (")
                                      .append(leftType).append(") y '").append(rightToken.getValue())
                                      .append("' (").append(rightType).append(")\n");
                            }
                        }
                    }
                } else if (op.equals("&&") || op.equals("||")) {
                    // Validar operadores lógicos
                    if (i > 0 && i < tokens.size() - 1) {
                        Token leftToken = tokens.get(i - 1);
                        Token rightToken = tokens.get(i + 1);
                        
                        String leftType = inferType(leftToken);
                        String rightType = inferType(rightToken);
                        
                        if (!leftType.equals("boolean") && !leftType.equals("unknown")) {
                            errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                                  .append(" - El operador '").append(op).append("' requiere operandos booleanos. ")
                                  .append("Operando izquierdo '").append(leftToken.getValue())
                                  .append("' es de tipo ").append(leftType).append("\n");
                        }
                        if (!rightType.equals("boolean") && !rightType.equals("unknown")) {
                            errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                                  .append(" - El operador '").append(op).append("' requiere operandos booleanos. ")
                                  .append("Operando derecho '").append(rightToken.getValue())
                                  .append("' es de tipo ").append(rightType).append("\n");
                        }
                    }
                }
            }
        }

        if (!hasComparisonOp) {
            errors.append("WARNING: ").append(context).append(" en línea ").append(line)
                  .append(" no parece tener operadores de comparación. ")
                  .append("¿Está seguro de que es una condición válida?\n");
        }
    }

    private static void validateCycleBody(List<Token> bodyTokens, int line, StringBuilder errors, StringBuilder info) {
        if (bodyTokens.isEmpty()) {
            errors.append("WARNING: Cuerpo del ciclo en línea ").append(line).append(" está vacío\n");
            return;
        }

        info.append("  Validando cuerpo del ciclo con ").append(bodyTokens.size()).append(" tokens\n");

        // Buscar y validar asignaciones en el cuerpo
        for (int i = 0; i < bodyTokens.size() - 2; i++) {
            if ((bodyTokens.get(i).getType() == Token.Type.IDENTIFIER_VAR || 
                 bodyTokens.get(i).getType() == Token.Type.IDENTIFIER_TYPE) &&
                "=".equals(bodyTokens.get(i + 1).getValue())) {
                
                List<Token> assignment = new ArrayList<>();
                int j = i;
                // Recoger toda la asignación hasta el final de la línea o punto y coma
                while (j < bodyTokens.size() && !";".equals(bodyTokens.get(j).getValue()) && 
                       !(j > i && (bodyTokens.get(j).getType() == Token.Type.IDENTIFIER_VAR || 
                                   bodyTokens.get(j).getType() == Token.Type.IDENTIFIER_TYPE) &&
                         j + 1 < bodyTokens.size() && "=".equals(bodyTokens.get(j + 1).getValue()))) {
                    assignment.add(bodyTokens.get(j));
                    j++;
                }
                
                validateAssignment(assignment, line, errors, "asignación en cuerpo del ciclo");
                i = j - 1; // Saltar los tokens ya procesados
            }
        }
    }

    private static String inferExpressionType(List<Token> tokens) {
        if (tokens.isEmpty()) return "unknown";
        
        // Para expresiones simples, usar el tipo del primer token significativo
        for (Token token : tokens) {
            if (token.getType() != Token.Type.SYMBOL && token.getType() != Token.Type.OPERATOR) {
                return inferType(token);
            }
        }
        return "unknown";
    }

    private static void validateExpression(List<Token> tokens, int line, StringBuilder errors, String context) {
        for (Token token : tokens) {
            if ((token.getType() == Token.Type.IDENTIFIER_VAR || token.getType() == Token.Type.IDENTIFIER_TYPE) 
                && !variableTypes.containsKey(token.getValue())) {
                errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                      .append(" usa variable no definida: '").append(token.getValue()).append("'\n");
            }
        }
    }

    // Métodos auxiliares para navegación de tokens
    private static int findNextToken(List<Token> tokens, int start, Token.Type type, String value) {
        for (int i = start; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == type && (value == null || value.equals(tokens.get(i).getValue()))) {
                return i;
            }
        }
        return -1;
    }

    private static int findMatchingCloseParen(List<Token> tokens, int openIndex) {
        int balance = 1;
        for (int i = openIndex + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == Token.Type.TUPLE_START || "(".equals(token.getValue())) {
                balance++;
            } else if (token.getType() == Token.Type.TUPLE_END || ")".equals(token.getValue())) {
                balance--;
                if (balance == 0) return i;
            }
        }
        return -1;
    }

    private static int findMatchingCloseBrace(List<Token> tokens, int openIndex) {
        int balance = 1;
        for (int i = openIndex + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("{".equals(token.getValue())) {
                balance++;
            } else if ("}".equals(token.getValue())) {
                balance--;
                if (balance == 0) return i;
            }
        }
        return -1;
    }

    private static List<Token> extractTokens(List<Token> tokens, int start, int end) {
        if (start >= end || start < 0 || end > tokens.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tokens.subList(start, end));
    }

    private static List<List<Token>> splitByDelimiter(List<Token> tokens, String delimiter) {
        List<List<Token>> parts = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        
        for (Token token : tokens) {
            if (delimiter.equals(token.getValue())) {
                parts.add(new ArrayList<>(current));
                current.clear();
            } else {
                current.add(token);
            }
        }
        parts.add(current); // Agregar la última parte
        
        return parts;
    }

    private static boolean isCycleKeyword(String kw) {
        return "while".equals(kw) || "for".equals(kw) || "loop".equals(kw) || "ciclo".equals(kw);
    }

    private static void initializeVariableTypes(List<Token> tokens) {
        variableTypes.clear();
        for (int i = 0; i < tokens.size() - 2; i++) {
            if (tokens.get(i).getType() == Token.Type.IDENTIFIER_VAR &&
                i + 1 < tokens.size() && tokens.get(i + 1).getValue().equals("=")) {
                
                Token valueToken = tokens.get(i + 2);
                String type = inferType(valueToken);
                String varName = tokens.get(i).getValue();
                
                // Registrar la primera declaración de cada variable
                if (!variableTypes.containsKey(varName)) {
                    variableTypes.put(varName, type);
                }
            }
        }
    }

    private static String inferType(Token token) {
        if (token == null) return "unknown";
        
        switch (token.getType()) {
            case INTEGER:
            case FLOAT:
                return "numeric";
            case STRING:
                return "string";
            case BOOLEAN:
                return "boolean";
            case CHAR:
                return "char";
            case IDENTIFIER_VAR:
            case IDENTIFIER_TYPE:
                String type = variableTypes.get(token.getValue());
                if (type == null) {
                    // Si la variable no está definida, intentamos inferir su tipo por el valor
                    String value = token.getValue();
                    if (value.matches("\\d+(\\.\\d+)?")) {
                        return "numeric";
                    } else if (value.startsWith("\"") && value.endsWith("\"")) {
                        return "string";
                    } else if (value.equals("True") || value.equals("False")) {
                        return "boolean";
                    } else if (value.startsWith("'") && value.endsWith("'") && value.length() == 3) {
                        return "char";
                    }
                }
                return type != null ? type : "unknown";
            case SYMBOL:
                // Reconocer valores literales que pueden venir como símbolos
                String value = token.getValue();
                if (value.matches("\\d+")) {
                    return "numeric";
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    return "string";
                }
                return "unknown";
            default:
                // Para tokens que no tienen tipo específico, intentar inferir por valor
                String tokenValue = token.getValue();
                if (tokenValue != null) {
                    // Detectar números enteros y decimales
                    if (tokenValue.matches("\\d+(\\.\\d+)?")) {
                        return "numeric";
                    } 
                    // Detectar strings - ser más flexible con las comillas
                    else if ((tokenValue.startsWith("\"") && tokenValue.endsWith("\"")) ||
                             (tokenValue.startsWith("'") && tokenValue.endsWith("'") && tokenValue.length() > 3)) {
                        return "string";
                    } 
                    // Detectar caracteres individuales
                    else if (tokenValue.startsWith("'") && tokenValue.endsWith("'") && tokenValue.length() == 3) {
                        return "char";
                    } 
                    // Detectar booleanos
                    else if (tokenValue.equals("True") || tokenValue.equals("False")) {
                        return "boolean";
                    }
                    // Detectar strings sin comillas (caso especial para el lexer)
                    else if (tokenValue.matches("[a-zA-Z]+") && !tokenValue.matches("\\d.*")) {
                        // Si es una palabra sin números, podría ser un string literal sin comillas
                        // Esto captura casos como 'hi' en lugar de '"hi"'
                        return "string";
                    }
                }
                return "unknown";
        }
    }

    private static boolean isCompatibleType(String type1, String type2) {
        if (type1 == null || type2 == null || type1.equals("unknown") || type2.equals("unknown")) 
            return true; // Permitir unknown para evitar errores en cascada
            
        // Tipos idénticos son compatibles
        if (type1.equals(type2)) 
            return true;
            
        // Ser más estricto: solo permitir conversiones muy específicas
        // NO permitir conversiones automáticas entre string y numeric
        if ((type1.equals("string") && type2.equals("numeric")) || 
            (type1.equals("numeric") && type2.equals("string"))) {
            return false; // Esta es la clave: NO permitir string <-> numeric
        }
            
        // Solo permitir conversiones seguras entre char y string
        if ((type1.equals("char") && type2.equals("string")) || 
            (type1.equals("string") && type2.equals("char"))) {
            return true;
        }
            
        // Todas las demás combinaciones son incompatibles
        return false;
    }

    private static boolean areComparableTypes(String type1, String type2) {
        if (type1 == null || type2 == null || type1.equals("unknown") || type2.equals("unknown")) 
            return true; // Permitir unknown para evitar errores en cascada
            
        // Tipos idénticos siempre son comparables
        if (type1.equals(type2)) 
            return true;
            
        // Para comparaciones, ser muy estricto:
        // NO permitir comparar string con numeric
        if ((type1.equals("string") && type2.equals("numeric")) || 
            (type1.equals("numeric") && type2.equals("string"))) {
            return false;
        }
        
        // NO permitir comparar boolean con otros tipos
        if (type1.equals("boolean") || type2.equals("boolean")) {
            return type1.equals("boolean") && type2.equals("boolean");
        }
        
        // Solo permitir comparaciones muy específicas
        // char y string pueden compararse
        if ((type1.equals("char") && type2.equals("string")) || 
            (type1.equals("string") && type2.equals("char"))) {
            return true;
        }
        
        // Todos los demás casos son incompatibles para comparación
        return false;
    }

    private static String getExampleValue(String type) {
        switch (type) {
            case "numeric":
                return "123 o 45.67";
            case "string":
                return "\"texto\"";
            case "boolean":
                return "True o False";
            case "char":
                return "'c'";
            default:
                return "valor del tipo " + type;
        }
    }

    private static void validateNumericExpression(List<Token> tokens, int line, StringBuilder errors, String context) {
        // Validar que una expresión numérica sea coherente
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            String tokenType = inferType(token);
            
            // Si encontramos un operador, verificar que los operandos sean numéricos
            if (token.getType() == Token.Type.SYMBOL || token.getType() == Token.Type.OPERATOR) {
                String op = token.getValue();
                if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
                    // Verificar operandos izquierdo y derecho
                    if (i > 0) {
                        String leftType = inferType(tokens.get(i - 1));
                        if (!leftType.equals("numeric") && !leftType.equals("unknown")) {
                            errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                                  .append(" - operando izquierdo del operador '").append(op)
                                  .append("' no es numérico (tipo: ").append(leftType).append(")\n");
                        }
                    }
                    if (i < tokens.size() - 1) {
                        String rightType = inferType(tokens.get(i + 1));
                        if (!rightType.equals("numeric") && !rightType.equals("unknown")) {
                            errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                                  .append(" - operando derecho del operador '").append(op)
                                  .append("' no es numérico (tipo: ").append(rightType).append(")\n");
                        }
                    }
                }
            } else if (!tokenType.equals("numeric") && !tokenType.equals("unknown")) {
                // Token no numérico en expresión numérica
                errors.append("ERROR: ").append(context).append(" en línea ").append(line)
                      .append(" - token no numérico '").append(token.getValue())
                      .append("' (tipo: ").append(tokenType).append(") en expresión numérica\n");
            }
        }
    }

}