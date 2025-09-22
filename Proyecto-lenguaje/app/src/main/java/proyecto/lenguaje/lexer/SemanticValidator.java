package proyecto.lenguaje.lexer;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class SemanticValidator {
    private static final Map<String, String> variableTypes = new HashMap<>();
    private static final Set<String> numericOperators = new HashSet<>(Arrays.asList("+", "-", "*", "/", "<", ">", "<=", ">="));
    private static final Set<String> booleanOperators = new HashSet<>(Arrays.asList("&&", "||", "==", "/="));
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

                boolean foundOpenParen = false;
                boolean foundCloseParen = false;
                boolean foundOpenBrace = false;
                boolean foundCloseBrace = false;
                int parenBalance = 0;
                int braceBalance = 0;

                // índice de inicio para el escaneo (después de la keyword)
                int j = i + 1;

                // Buscar el primer paréntesis de condición o la primera llave de bloque
                List<Token> conditionTokens = new ArrayList<>();
                boolean inCondition = false;
                
                for (; j < tokens.size(); j++) {
                    Token next = tokens.get(j);
                    if (next.getType() == Token.Type.KEYWORD && isCycleKeyword(next.getValue()) && j > i + 1) {
                        break;
                    }

                    if (!foundOpenParen && (next.getType() == Token.Type.TUPLE_START ||
                            (next.getType() == Token.Type.SYMBOL && "(".equals(next.getValue())))) {
                        foundOpenParen = true;
                        parenBalance = 1;
                        inCondition = true;
                        j++; // continuar desde token siguiente al '('
                        continue;
                    }

                    if (inCondition) {
                        if (next.getType() == Token.Type.TUPLE_END ||
                            (next.getType() == Token.Type.SYMBOL && ")".equals(next.getValue()))) {
                            inCondition = false;
                            foundCloseParen = true;
                            validateCondition(conditionTokens, errors, t.getLine());
                        } else {
                            conditionTokens.add(next);
                        }
                    }

                    if (!foundOpenBrace && next.getType() == Token.Type.SYMBOL && "{".equals(next.getValue())) {
                        foundOpenBrace = true;
                        braceBalance = 1;
                        break;
                    }
                }

                // Validar el cuerpo del ciclo
                List<Token> bodyTokens = new ArrayList<>();
                boolean inBody = false;

                // Escanear desde j hasta cerrar las llaves o hasta otra keyword de ciclo
                for (int k = j + 1; k < tokens.size(); k++) {
                    Token next = tokens.get(k);
                    if (next.getType() == Token.Type.KEYWORD && isCycleKeyword(next.getValue()) && k > i + 1) {
                        break;
                    }

                    if (next.getType() == Token.Type.SYMBOL && "{".equals(next.getValue())) {
                        braceBalance++;
                        if (!inBody) {
                            inBody = true;
                            continue;
                        }
                    } else if (next.getType() == Token.Type.SYMBOL && "}".equals(next.getValue())) {
                        braceBalance--;
                        if (braceBalance == 0) {
                            foundCloseBrace = true;
                            validateBody(bodyTokens, errors);
                            break;
                        }
                    }

                    if (inBody && braceBalance > 0) {
                        bodyTokens.add(next);
                    }
                }

                // Comprobaciones específicas para 'for': dentro de los paréntesis debería haber 2 ';'
                if ("for".equals(t.getValue()) && foundOpenParen) {
                    // contar ';' entre el primer '(' y su cierre
                    int semicolons = 0;
                    int depth = 0;
                    boolean inside = false;
                    for (int k = i + 1; k < tokens.size(); k++) {
                        Token next = tokens.get(k);
                        if (!inside && (next.getType() == Token.Type.TUPLE_START || (next.getType() == Token.Type.SYMBOL && "(".equals(next.getValue())))) {
                            inside = true;
                            depth = 1;
                            continue;
                        }
                        if (inside) {
                            if (next.getType() == Token.Type.TUPLE_START || (next.getType() == Token.Type.SYMBOL && "(".equals(next.getValue()))) depth++;
                            else if (next.getType() == Token.Type.TUPLE_END || (next.getType() == Token.Type.SYMBOL && ")".equals(next.getValue()))) {
                                depth--;
                                if (depth == 0) break; // fin de paréntesis
                            }
                            if (next.getType() == Token.Type.SYMBOL && ";".equals(next.getValue())) semicolons++;
                        }
                    }
                    if (semicolons < 2) {
                        errors.append("ERROR: for en línea ").append(t.getLine())
                              .append(" parece no contener las 2 separaciones ';' dentro de sus paréntesis. Encontradas: ")
                              .append(semicolons).append(".\n");
                    }
                }

                // Mensajes de error finales según lo descubierto
                if (!foundOpenParen) {
                    errors.append("ERROR: Ciclo '").append(t.getValue())
                          .append("' en línea ").append(t.getLine())
                          .append(" no tiene paréntesis de apertura para la condición.\n");
                } else if (!foundCloseParen) {
                    errors.append("ERROR: Ciclo '").append(t.getValue())
                          .append("' en línea ").append(t.getLine())
                          .append(" no tiene paréntesis de cierre para la condición.\n");
                }

                if (!foundOpenBrace) {
                    errors.append("ERROR: Ciclo '").append(t.getValue())
                          .append("' en línea ").append(t.getLine())
                          .append(" no tiene llave de apertura para el bloque de código.\n");
                } else if (!foundCloseBrace) {
                    errors.append("ERROR: Ciclo '").append(t.getValue())
                          .append("' en línea ").append(t.getLine())
                          .append(" no tiene llave de cierre para el bloque de código.\n");
                }
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
            }
        }

        return result.toString();
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
                variableTypes.put(tokens.get(i).getValue(), type);
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
                    if (token.getValue().matches("\\d+(\\.\\d+)?")) {
                        return "numeric";
                    } else if (token.getValue().matches("\".*\"")) {
                        return "string";
                    } else if (token.getValue().equals("True") || token.getValue().equals("False")) {
                        return "boolean";
                    }
                }
                return type != null ? type : "unknown";
            default:
                return "unknown";
        }
    }

    private static void validateCondition(List<Token> conditionTokens, StringBuilder errors, int line) {
        if (conditionTokens.isEmpty()) {
            errors.append("ERROR en línea ").append(line)
                  .append(": Condición del ciclo vacía\n");
            return;
        }

        // Validar que las variables existan y sean del tipo correcto
        for (int i = 0; i < conditionTokens.size(); i++) {
            Token token = conditionTokens.get(i);
            
            if (token.getType() == Token.Type.IDENTIFIER_VAR || token.getType() == Token.Type.IDENTIFIER_TYPE) {
                String varType = variableTypes.get(token.getValue());
                if (varType == null) {
                    errors.append("ERROR en línea ").append(line)
                          .append(": Variable '").append(token.getValue())
                          .append("' no está definida\n");
                }
            }
            
            // Validar operadores
            if (token.getType() == Token.Type.SYMBOL) {
                String operator = token.getValue();
                if (i > 0 && i < conditionTokens.size() - 1) {
                    Token left = conditionTokens.get(i - 1);
                    Token right = conditionTokens.get(i + 1);
                    validateOperation(left, operator, right, errors, line);
                }
            }
        }
    }

    private static void validateBody(List<Token> bodyTokens, StringBuilder errors) {
        for (int i = 0; i < bodyTokens.size() - 2; i++) {
            Token current = bodyTokens.get(i);
            
            // Validar asignaciones
            if ((current.getType() == Token.Type.IDENTIFIER_VAR || current.getType() == Token.Type.IDENTIFIER_TYPE) &&
                i + 1 < bodyTokens.size() && bodyTokens.get(i + 1).getValue().equals("=")) {
                
                Token value = bodyTokens.get(i + 2);
                String expectedType = variableTypes.get(current.getValue());
                String assignedType = inferType(value);
                
                if (expectedType == null) {
                    // Primera asignación a la variable
                    variableTypes.put(current.getValue(), assignedType);
                } else if (!isCompatibleType(expectedType, assignedType)) {
                    errors.append("ERROR en línea ").append(current.getLine())
                          .append(": Tipo incompatible en asignación. Variable '")
                          .append(current.getValue())
                          .append("' es de tipo ").append(expectedType)
                          .append(" pero se le intenta asignar un valor de tipo ")
                          .append(assignedType).append("\n");
                }

                // Si el valor asignado es una operación, validar la operación
                if (i + 4 < bodyTokens.size() && bodyTokens.get(i + 3).getType() == Token.Type.SYMBOL) {
                    validateOperation(value, bodyTokens.get(i + 3).getValue(), bodyTokens.get(i + 4), errors, current.getLine());
                }
            }
        }
    }

    private static boolean isCompatibleType(String type1, String type2) {
        if (type1 == null || type2 == null || type1.equals("unknown") || type2.equals("unknown")) 
            return false;
            
        // Tipos idénticos son compatibles
        if (type1.equals(type2)) 
            return true;
            
        // Numéricos son compatibles entre sí
        if (type1.equals("numeric") && type2.equals("numeric")) 
            return true;
            
        // Casos especiales
        if (type1.equals("char") && type2.equals("string") || 
            type1.equals("string") && type2.equals("char"))
            return true;
            
        return false;
    }

    private static void validateOperation(Token left, String operator, Token right, StringBuilder errors, int line) {
        String leftType = inferType(left);
        String rightType = inferType(right);

        // Validar que ambos operandos tengan tipos conocidos
        if (leftType.equals("unknown")) {
            errors.append("ERROR en línea ").append(line)
                  .append(": Variable no definida o tipo desconocido '")
                  .append(left.getValue()).append("'\n");
            return;
        }
        if (rightType.equals("unknown")) {
            errors.append("ERROR en línea ").append(line)
                  .append(": Variable no definida o tipo desconocido '")
                  .append(right.getValue()).append("'\n");
            return;
        }

        // Validar operaciones aritméticas
        if (numericOperators.contains(operator)) {
            if (!leftType.equals("numeric") || !rightType.equals("numeric")) {
                errors.append("ERROR en línea ").append(line)
                      .append(": No se puede usar el operador '").append(operator)
                      .append("' entre tipos '").append(leftType).append("' y '")
                      .append(rightType).append("'. Se requieren operandos numéricos.\n");
            }
        } 
        // Validar operadores booleanos
        else if (booleanOperators.contains(operator)) {
            if (operator.equals("&&") || operator.equals("||")) {
                if (!leftType.equals("boolean") || !rightType.equals("boolean")) {
                    errors.append("ERROR en línea ").append(line)
                          .append(": Operador '").append(operator)
                          .append("' requiere operandos booleanos\n");
                }
            } else {
                // Para operadores de comparación (==, /=)
                if (!leftType.equals(rightType)) {
                    errors.append("ERROR en línea ").append(line)
                          .append(": No se pueden comparar tipos diferentes: '")
                          .append(leftType).append("' con '").append(rightType)
                          .append("'\n");
                }
            }
        }
        // Validar concatenación de strings
        else if (operator.equals("+") && (leftType.equals("string") || rightType.equals("string"))) {
            if (!leftType.equals(rightType)) {
                errors.append("ERROR en línea ").append(line)
                      .append(": No se puede concatenar tipo '").append(leftType)
                      .append("' con tipo '").append(rightType)
                      .append("'. Ambos deben ser strings.\n");
            }
        }
    }
}