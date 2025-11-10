package proyecto.lenguaje.codegen;

import java.util.*;
import java.util.regex.*;

/**
 * Optimizador de código Haskell
 * Realiza las siguientes optimizaciones:
 * 1. Eliminación de comentarios
 * 2. Eliminación de espacios innecesarios
 * 3. Eliminación de Subexpresiones Comunes (CSE)
 */
public class CodeOptimizer {
    
    private Map<String, String> subexpressionMap;
    private int tempVarCounter;
    private List<String> optimizationLog;
    
    public CodeOptimizer() {
        this.subexpressionMap = new LinkedHashMap<>();
        this.tempVarCounter = 0;
        this.optimizationLog = new ArrayList<>();
    }
    
    /**
     * Resultado de la optimización
     */
    public static class OptimizationResult {
        public String optimizedCode;
        public List<String> log;
        public boolean success;
        public String errorMessage;
        public int commentsRemoved;
        public int spacesOptimized;
        public int subexpressionsEliminated;
        
        public OptimizationResult(String code, List<String> log, boolean success) {
            this.optimizedCode = code;
            this.log = log;
            this.success = success;
            this.commentsRemoved = 0;
            this.spacesOptimized = 0;
            this.subexpressionsEliminated = 0;
        }
    }
    
    /**
     * Optimiza el código completo
     */
    public OptimizationResult optimize(String code) {
        try {
            optimizationLog.clear();
            subexpressionMap.clear();
            tempVarCounter = 0;
            
            optimizationLog.add("=== INICIANDO OPTIMIZACIÓN ===\n");
            
            // Paso 1: Eliminar comentarios
            optimizationLog.add("PASO 1: Eliminando comentarios...");
            String withoutComments = removeComments(code);
            int commentsRemoved = countComments(code);
            optimizationLog.add("  ✓ Comentarios eliminados: " + commentsRemoved + "\n");
            
            // Paso 2: Optimizar espacios
            optimizationLog.add("PASO 2: Optimizando espacios en blanco...");
            String withoutExtraSpaces = optimizeSpaces(withoutComments);
            int spacesOptimized = countExtraSpaces(withoutComments, withoutExtraSpaces);
            optimizationLog.add("  ✓ Espacios optimizados: " + spacesOptimized + "\n");
            
            // Paso 3: Eliminación de Subexpresiones Comunes
            optimizationLog.add("PASO 3: Eliminando subexpresiones comunes...");
            String optimized = eliminateCommonSubexpressions(withoutExtraSpaces);
            optimizationLog.add("  ✓ Subexpresiones eliminadas: " + subexpressionMap.size() + "\n");
            
            // Si se encontraron subexpresiones, agregar las definiciones al inicio
            if (!subexpressionMap.isEmpty()) {
                optimizationLog.add("\nSubexpresiones extraídas:");
                StringBuilder definitions = new StringBuilder();
                for (Map.Entry<String, String> entry : subexpressionMap.entrySet()) {
                    definitions.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
                    optimizationLog.add("  " + entry.getKey() + " = " + entry.getValue());
                }
                optimized = definitions.toString() + optimized;
            }
            
            optimizationLog.add("\n=== OPTIMIZACIÓN COMPLETADA ===");
            optimizationLog.add("Tamaño original: " + code.length() + " caracteres");
            optimizationLog.add("Tamaño optimizado: " + optimized.length() + " caracteres");
            optimizationLog.add("Reducción: " + (code.length() - optimized.length()) + " caracteres (" + 
                              String.format("%.1f", (1.0 - (double)optimized.length() / code.length()) * 100) + "%)");
            
            OptimizationResult result = new OptimizationResult(optimized, optimizationLog, true);
            result.commentsRemoved = commentsRemoved;
            result.spacesOptimized = spacesOptimized;
            result.subexpressionsEliminated = subexpressionMap.size();
            
            return result;
            
        } catch (Exception e) {
            optimizationLog.add("\n❌ ERROR: " + e.getMessage());
            OptimizationResult result = new OptimizationResult("", optimizationLog, false);
            result.errorMessage = e.getMessage();
            return result;
        }
    }
    
    /**
     * Elimina todos los comentarios del código
     */
    private String removeComments(String code) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        
        while (pos < code.length()) {
            // Comentarios de línea --
            if (pos < code.length() - 1 && code.charAt(pos) == '-' && code.charAt(pos + 1) == '-') {
                // Saltar hasta el final de la línea
                int endLine = code.indexOf('\n', pos);
                if (endLine == -1) {
                    break; // Comentario hasta el final del archivo
                }
                pos = endLine + 1;
                result.append('\n'); // Mantener el salto de línea
                continue;
            }
            
            // Comentarios multilínea {- -}
            if (pos < code.length() - 1 && code.charAt(pos) == '{' && code.charAt(pos + 1) == '-') {
                pos += 2;
                int depth = 1;
                while (pos < code.length() - 1 && depth > 0) {
                    if (code.charAt(pos) == '{' && code.charAt(pos + 1) == '-') {
                        depth++;
                        pos += 2;
                    } else if (code.charAt(pos) == '-' && code.charAt(pos + 1) == '}') {
                        depth--;
                        pos += 2;
                    } else {
                        if (code.charAt(pos) == '\n') {
                            result.append('\n'); // Mantener saltos de línea
                        }
                        pos++;
                    }
                }
                continue;
            }
            
            result.append(code.charAt(pos));
            pos++;
        }
        
        return result.toString();
    }
    
    /**
     * Optimiza espacios en blanco innecesarios
     */
    private String optimizeSpaces(String code) {
        // Eliminar espacios múltiples
        String result = code.replaceAll("[ \\t]+", " ");
        
        // Eliminar espacios al inicio y final de líneas
        result = result.replaceAll("(?m)^\\s+|\\s+$", "");
        
        // Eliminar líneas vacías múltiples
        result = result.replaceAll("\\n{3,}", "\n\n");
        
        // Eliminar espacios alrededor de operadores (opcional, más agresivo)
        // result = result.replaceAll("\\s*([=+\\-*/()\\[\\]{},:;])\\s*", "$1");
        
        return result.trim();
    }
    
    /**
     * Elimina subexpresiones comunes (CSE)
     * Identifica expresiones que aparecen múltiples veces y las extrae en variables
     */
    private String eliminateCommonSubexpressions(String code) {
        String[] lines = code.split("\n");
        Map<String, Integer> expressionCount = new HashMap<>();
        
        // Fase 1: Identificar subexpresiones comunes
        for (String line : lines) {
            List<String> expressions = extractExpressions(line);
            for (String expr : expressions) {
                if (isOptimizableExpression(expr)) {
                    expressionCount.put(expr, expressionCount.getOrDefault(expr, 0) + 1);
                }
            }
        }
        
        // Fase 2: Crear variables temporales para expresiones que aparecen más de una vez
        for (Map.Entry<String, Integer> entry : expressionCount.entrySet()) {
            if (entry.getValue() > 1) {
                String tempVar = "cse_" + tempVarCounter++;
                subexpressionMap.put(tempVar, entry.getKey());
            }
        }
        
        // Fase 3: Reemplazar expresiones comunes con variables temporales
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String optimizedLine = line;
            for (Map.Entry<String, String> entry : subexpressionMap.entrySet()) {
                // Reemplazar la expresión con la variable temporal
                optimizedLine = optimizedLine.replace(entry.getValue(), entry.getKey());
            }
            result.append(optimizedLine).append("\n");
        }
        
        return result.toString().trim();
    }
    
    /**
     * Extrae expresiones de una línea de código
     */
    private List<String> extractExpressions(String line) {
        List<String> expressions = new ArrayList<>();
        
        // Buscar expresiones aritméticas/lógicas (simplificado)
        Pattern pattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_']*\\s*[+\\-*/]\\s*[a-zA-Z_][a-zA-Z0-9_']*)");
        Matcher matcher = pattern.matcher(line);
        
        while (matcher.find()) {
            expressions.add(matcher.group(1).trim());
        }
        
        // Buscar llamadas a funciones con argumentos
        pattern = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_']*\\s+[a-zA-Z_][a-zA-Z0-9_']*\\s+[a-zA-Z_][a-zA-Z0-9_']*)");
        matcher = pattern.matcher(line);
        
        while (matcher.find()) {
            expressions.add(matcher.group(1).trim());
        }
        
        return expressions;
    }
    
    /**
     * Determina si una expresión es optimizable
     */
    private boolean isOptimizableExpression(String expr) {
        // No optimizar expresiones muy simples
        if (expr.length() < 5) return false;
        
        // No optimizar si contiene solo una variable
        if (!expr.matches(".*[+\\-*/\\s].*")) return false;
        
        // No optimizar literales simples
        if (expr.matches("\\d+")) return false;
        
        return true;
    }
    
    /**
     * Cuenta comentarios en el código
     */
    private int countComments(String code) {
        int count = 0;
        
        // Contar comentarios de línea
        Pattern lineComments = Pattern.compile("--.*");
        Matcher matcher = lineComments.matcher(code);
        while (matcher.find()) {
            count++;
        }
        
        // Contar comentarios multilínea
        Pattern blockComments = Pattern.compile("\\{-.*?-\\}", Pattern.DOTALL);
        matcher = blockComments.matcher(code);
        while (matcher.find()) {
            count++;
        }
        
        return count;
    }
    
    /**
     * Cuenta espacios extra eliminados
     */
    private int countExtraSpaces(String original, String optimized) {
        return original.length() - optimized.length();
    }
}
