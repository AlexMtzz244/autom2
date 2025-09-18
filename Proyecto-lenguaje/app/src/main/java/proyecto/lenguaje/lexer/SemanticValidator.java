package proyecto.lenguaje.lexer;

import java.util.List;

public class SemanticValidator {
    public static String validateCycles(List<Token> tokens) {
        StringBuilder errors = new StringBuilder();
        StringBuilder info = new StringBuilder();
        int cycleCount = 0;
        
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            
            // Detecta palabras clave que podrían representar ciclos
            if (t.getType() == Token.Type.KEYWORD && 
                (t.getValue().equals("while") || t.getValue().equals("for") || 
                 t.getValue().equals("loop") || t.getValue().equals("ciclo"))) {
                
                cycleCount++;
                info.append("Ciclo detectado: '").append(t.getValue())
                    .append("' en línea ").append(t.getLine())
                    .append(", posición ").append(t.getPosition()).append("\n");
                
                // Validación estructural mejorada
                boolean foundOpenParen = false;
                boolean foundCloseParen = false;
                boolean foundOpenBrace = false;
                boolean foundCloseBrace = false;
                int parenBalance = 0;
                int braceBalance = 0;
                
                // Examinar los siguientes tokens para validar estructura
                for (int j = i + 1; j < tokens.size() && j < i + 20; j++) {
                    Token next = tokens.get(j);
                    
                    // Buscar paréntesis para condición
                    if (next.getType() == Token.Type.TUPLE_START || 
                        (next.getType() == Token.Type.SYMBOL && next.getValue().equals("("))) {
                        foundOpenParen = true;
                        parenBalance++;
                    } else if (next.getType() == Token.Type.TUPLE_END || 
                               (next.getType() == Token.Type.SYMBOL && next.getValue().equals(")"))) {
                        parenBalance--;
                        if (parenBalance == 0 && foundOpenParen) {
                            foundCloseParen = true;
                        }
                    }
                    
                    // Buscar llaves para bloque
                    else if (next.getType() == Token.Type.SYMBOL && next.getValue().equals("{")) {
                        foundOpenBrace = true;
                        braceBalance++;
                    } else if (next.getType() == Token.Type.SYMBOL && next.getValue().equals("}")) {
                        braceBalance--;
                        if (braceBalance == 0 && foundOpenBrace) {
                            foundCloseBrace = true;
                        }
                    }
                    
                    // Detener búsqueda si encontramos otro ciclo
                    if (next.getType() == Token.Type.KEYWORD && 
                        (next.getValue().equals("while") || next.getValue().equals("for") || 
                         next.getValue().equals("loop") || next.getValue().equals("ciclo"))) {
                        break;
                    }
                }
                
                // Validar errores semánticos
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
}