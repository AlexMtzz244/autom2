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

                // Buscar el primer paréntesis de condición o la primera llave de bloque.
                // No usamos un límite rígido; escaneamos hasta encontrar lo necesario o hasta otra keyword de ciclo.
                for (; j < tokens.size(); j++) {
                    Token next = tokens.get(j);
                    if (next.getType() == Token.Type.KEYWORD && isCycleKeyword(next.getValue()) && j > i + 1) {
                        // otro ciclo cercano -> detener búsqueda
                        break;
                    }

                    if (!foundOpenParen && (next.getType() == Token.Type.TUPLE_START ||
                            (next.getType() == Token.Type.SYMBOL && "(".equals(next.getValue())))) {
                        foundOpenParen = true;
                        parenBalance = 1;
                        j++; // continuar desde token siguiente al '('
                        break;
                    }

                    if (!foundOpenBrace && next.getType() == Token.Type.SYMBOL && "{".equals(next.getValue())) {
                        // Encontramos directamente el bloque sin paréntesis de condición
                        foundOpenBrace = true;
                        braceBalance = 1;
                        j++; // continuar desde token siguiente a '{'
                        break;
                    }
                }

                // Escanear desde j hasta cerrar paréntesis/llaves o hasta otra keyword de ciclo
                for (int k = j; k < tokens.size(); k++) {
                    Token next = tokens.get(k);
                    if (next.getType() == Token.Type.KEYWORD && isCycleKeyword(next.getValue()) && k > i + 1) {
                        break;
                    }

                    // Paréntesis
                    if (next.getType() == Token.Type.TUPLE_START ||
                        (next.getType() == Token.Type.SYMBOL && "(".equals(next.getValue()))) {
                        foundOpenParen = true;
                        parenBalance++;
                    } else if (next.getType() == Token.Type.TUPLE_END ||
                               (next.getType() == Token.Type.SYMBOL && ")".equals(next.getValue()))) {
                        if (parenBalance > 0) parenBalance--;
                        if (foundOpenParen && parenBalance == 0) foundCloseParen = true;
                    }

                    // Llaves
                    if (next.getType() == Token.Type.SYMBOL && "{".equals(next.getValue())) {
                        foundOpenBrace = true;
                        braceBalance++;
                    } else if (next.getType() == Token.Type.SYMBOL && "}".equals(next.getValue())) {
                        if (braceBalance > 0) braceBalance--;
                        if (foundOpenBrace && braceBalance == 0) foundCloseBrace = true;
                    }

                    // Si ya cerramos el bloque y (si había paréntesis) también los paréntesis, podemos parar
                    if (foundOpenBrace && foundCloseBrace && (!foundOpenParen || foundCloseParen)) {
                        break;
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
}