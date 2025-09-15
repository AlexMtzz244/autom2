package proyecto.lenguaje.lexer;

import java.util.List;

public class SemanticValidator {
    public static String validateCycles(List<Token> tokens) {
        StringBuilder errors = new StringBuilder();
        StringBuilder info = new StringBuilder();
        StringBuilder debugInfo = new StringBuilder();
        int cycleCount = 0;
        
        // Debug: mostrar algunos tokens para entender la tokenización
        debugInfo.append("=== DEBUG: PRIMEROS 20 TOKENS ===\n");
        for (int i = 0; i < Math.min(20, tokens.size()); i++) {
            Token t = tokens.get(i);
            debugInfo.append(i).append(": ").append(t.toString()).append("\n");
        }
        debugInfo.append("============================\n\n");
        
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            
            // Detecta palabras clave que podrían representar ciclos
            if (t.getType() == Token.Type.KEYWORD && 
                (t.getValue().equals("while") || t.getValue().equals("for") || 
                 t.getValue().equals("loop") || t.getValue().equals("ciclo"))) {
                
                cycleCount++;
                info.append("Ciclo detectado: '").append(t.getValue())
                    .append("' en línea ").append(t.getLine()).append("\n");
                
                // Validación estructural mejorada
                boolean hasCondition = false;
                boolean hasBlock = false;
                boolean foundOpenParen = false;
                boolean foundCloseParen = false;
                boolean foundOpenBrace = false;
                int parenBalance = 0;
                
                // Examinar los siguientes tokens
                for (int j = i + 1; j < tokens.size() && j < i + 20; j++) {
                    Token next = tokens.get(j);
                    
                    // Buscar paréntesis en diferentes tipos de token
                    if ((next.getType() == Token.Type.OPERATOR && next.getValue().equals("(")) ||
                        (next.getType() == Token.Type.TUPLE_START)) {
                        foundOpenParen = true;
                        parenBalance++;
                    } else if ((next.getType() == Token.Type.OPERATOR && next.getValue().equals(")")) ||
                               (next.getType() == Token.Type.TUPLE_END)) {
                        parenBalance--;
                        if (parenBalance == 0 && foundOpenParen) {
                            foundCloseParen = true;
                        }
                    } else if ((next.getType() == Token.Type.OPERATOR && next.getValue().equals("{")) ||
                               (next.getType() == Token.Type.SYMBOL && next.getValue().equals("{"))) {
                        foundOpenBrace = true;
                        break; // Encontramos el bloque
                    } else if (next.getType() == Token.Type.KEYWORD && 
                              (next.getValue().equals("while") || next.getValue().equals("for") || 
                               next.getValue().equals("loop") || next.getValue().equals("ciclo"))) {
                        // Si encontramos otra palabra clave de ciclo sin haber encontrado estructura, paramos
                        break;
                    }
                }
                
                // Evaluar estructura encontrada
                hasCondition = foundOpenParen && foundCloseParen;
                hasBlock = foundOpenBrace;
                
                // Casos específicos de error
                if (!foundOpenParen && !foundOpenBrace) {
                    errors.append("ERROR SEMÁNTICO: Ciclo '").append(t.getValue())
                          .append("' en línea ").append(t.getLine())
                          .append(" - falta condición y bloque de código\n");
                } else if (!foundOpenParen && foundOpenBrace) {
                    errors.append("ERROR SEMÁNTICO: Ciclo '").append(t.getValue())
                          .append("' en línea ").append(t.getLine())
                          .append(" - falta condición (paréntesis)\n");
                } else if (foundOpenParen && !foundCloseParen) {
                    errors.append("ERROR SEMÁNTICO: Ciclo '").append(t.getValue())
                          .append("' en línea ").append(t.getLine())
                          .append(" - condición sin cerrar (falta ')')\n");
                } else if (hasCondition && !hasBlock) {
                    errors.append("ERROR SEMÁNTICO: Ciclo '").append(t.getValue())
                          .append("' en línea ").append(t.getLine())
                          .append(" - falta bloque de código (llaves '{}')\n");
                } else if (hasCondition && hasBlock) {
                    info.append("  -> Ciclo bien formado semánticamente\n");
                }
            }
        }
        
        StringBuilder result = new StringBuilder();
        result.append(debugInfo.toString());
        result.append("=== ANÁLISIS SEMÁNTICO DE CICLOS ===\n");
        result.append("Total de ciclos detectados: ").append(cycleCount).append("\n\n");
        
        if (cycleCount == 0) {
            result.append("No se detectaron ciclos en el código.\n");
            result.append("Nota: Haskell es un lenguaje funcional que no utiliza ciclos imperativos.\n");
        } else {
            result.append("--- INFORMACIÓN ---\n");
            result.append(info.toString()).append("\n");
            
            if (errors.length() > 0) {
                result.append("--- ERRORES SEMÁNTICOS ---\n");
                result.append(errors.toString());
            } else {
                result.append("Todos los ciclos están bien formados semánticamente.\n");
            }
        }
        
        return result.toString();
    }
}
