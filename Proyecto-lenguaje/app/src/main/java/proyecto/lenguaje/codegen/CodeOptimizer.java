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
            
            // Si se encontraron subexpresiones, agregar las definiciones al inicio con formato válido
            if (!subexpressionMap.isEmpty()) {
                optimizationLog.add("\nSubexpresiones extraídas:");
                StringBuilder definitions = new StringBuilder();
                definitions.append("-- Variables de subexpresiones comunes (generadas por optimización)\n");
                for (Map.Entry<String, String> entry : subexpressionMap.entrySet()) {
                    definitions.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
                    optimizationLog.add("  " + entry.getKey() + " = " + entry.getValue());
                }
                definitions.append("\n-- Código optimizado\n");
                optimized = definitions.toString() + optimized;
            }
            
            // Asegurar que el código termina con salto de línea
            if (!optimized.endsWith("\n")) {
                optimized += "\n";
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
     * Mantiene la sintaxis válida de Haskell
     */
    private String optimizeSpaces(String code) {
        StringBuilder result = new StringBuilder();
        String[] lines = code.split("\n");
        
        for (String line : lines) {
            // Ignorar líneas vacías
            if (line.trim().isEmpty()) {
                continue;
            }
            
            // Normalizar espacios múltiples a uno solo, pero mantener al menos uno
            String optimizedLine = line.replaceAll("[ \\t]+", " ");
            
            // Eliminar espacios al inicio y final de la línea
            optimizedLine = optimizedLine.trim();
            
            // Asegurar espacio correcto alrededor del operador '='
            // Formato: variable = expresión
            optimizedLine = optimizedLine.replaceAll("\\s*=\\s*", " = ");
            
            // IMPORTANTE: Detectar números negativos y no agregar espacio después del signo menos
            // Patrón: = -número o = -variable
            optimizedLine = optimizedLine.replaceAll("=\\s+-\\s*([0-9])", "= -$1");
            
            // Asegurar espacio alrededor de operadores aritméticos binarios
            // Pero solo si no están dentro de strings y no son signos negativos
            if (!optimizedLine.contains("\"") && !optimizedLine.contains("'")) {
                // Para operadores que NO son el signo menos después de =
                optimizedLine = optimizedLine.replaceAll("\\s*([+*/%^])\\s*", " $1 ");
                
                // Para el operador menos, solo si NO es un signo negativo unario
                // (es decir, si hay algo antes del menos que no sea = o un operador)
                optimizedLine = optimizedLine.replaceAll("([a-zA-Z0-9_'])\\s*-\\s*", "$1 - ");
            }
            
            // Limpiar espacios múltiples que puedan haberse generado
            optimizedLine = optimizedLine.replaceAll("  +", " ");
            
            result.append(optimizedLine).append("\n");
        }
        
        // Eliminar líneas vacías al final
        String resultStr = result.toString().trim();
        
        // Asegurar que termina con un salto de línea
        if (!resultStr.isEmpty() && !resultStr.endsWith("\n")) {
            resultStr += "\n";
        }
        
        return resultStr;
    }
    
    /**
     * Elimina subexpresiones comunes (CSE)
     * Identifica expresiones que aparecen múltiples veces y las extrae en variables
     * Genera código sintácticamente válido
     */
    private String eliminateCommonSubexpressions(String code) {
        String[] lines = code.split("\n");
        Map<String, Integer> expressionCount = new HashMap<>();
        Map<String, String> lineContexts = new HashMap<>();
        
        // Fase 1: Identificar subexpresiones comunes
        for (String line : lines) {
            // Ignorar líneas vacías
            if (line.trim().isEmpty()) {
                continue;
            }
            
            List<String> expressions = extractExpressions(line);
            for (String expr : expressions) {
                if (isOptimizableExpression(expr)) {
                    expressionCount.put(expr, expressionCount.getOrDefault(expr, 0) + 1);
                    lineContexts.put(expr, line);
                }
            }
        }
        
        // Fase 2: Crear variables temporales solo para expresiones que aparecen más de una vez
        Map<String, String> replacements = new HashMap<>();
        for (Map.Entry<String, Integer> entry : expressionCount.entrySet()) {
            if (entry.getValue() > 1) {
                String tempVar = "cse" + tempVarCounter;
                tempVarCounter++;
                subexpressionMap.put(tempVar, entry.getKey());
                replacements.put(entry.getKey(), tempVar);
            }
        }
        
        // Fase 3: Reemplazar expresiones comunes con variables temporales
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            // Ignorar líneas vacías
            if (line.trim().isEmpty()) {
                continue;
            }
            
            String optimizedLine = line;
            
            // Reemplazar cada expresión común con su variable temporal
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                String expression = entry.getKey();
                String tempVar = entry.getValue();
                
                // Solo reemplazar si la línea no es la definición de una variable CSE
                if (!optimizedLine.startsWith(tempVar + " =")) {
                    // Reemplazar la expresión completa
                    optimizedLine = optimizedLine.replace(expression, tempVar);
                }
            }
            
            result.append(optimizedLine).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Extrae expresiones aritméticas de una línea de código
     * Solo extrae expresiones válidas en Haskell
     */
    private List<String> extractExpressions(String line) {
        List<String> expressions = new ArrayList<>();
        
        // Ignorar si la línea es solo una asignación simple (variable = valor)
        if (line.matches("^[a-zA-Z_][a-zA-Z0-9_']*\\s*=\\s*[a-zA-Z0-9_']+\\s*$")) {
            return expressions;
        }
        
        // Buscar expresiones aritméticas binarias: var op var o num op num
        // Ejemplos: x + y, a * b, 10 + 20
        Pattern arithPattern = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_']*|\\d+(?:\\.\\d+)?)\\s*([+\\-*/%^])\\s*([a-zA-Z_][a-zA-Z0-9_']*|\\d+(?:\\.\\d+)?)"
        );
        Matcher matcher = arithPattern.matcher(line);
        
        while (matcher.find()) {
            String left = matcher.group(1);
            String op = matcher.group(2);
            String right = matcher.group(3);
            
            // Reconstruir con formato consistente (un espacio alrededor del operador)
            String normalizedExpr = left + " " + op + " " + right;
            expressions.add(normalizedExpr);
        }
        
        // Buscar expresiones con paréntesis simples: (expr)
        Pattern parenPattern = Pattern.compile(
            "\\(([a-zA-Z_][a-zA-Z0-9_']*|\\d+(?:\\.\\d+)?)\\s*([+\\-*/%^])\\s*([a-zA-Z_][a-zA-Z0-9_']*|\\d+(?:\\.\\d+)?)\\)"
        );
        matcher = parenPattern.matcher(line);
        
        while (matcher.find()) {
            String inner = matcher.group(1) + " " + matcher.group(2) + " " + matcher.group(3);
            // Solo agregar si es diferente de las expresiones ya encontradas
            if (!expressions.contains(inner)) {
                expressions.add(inner);
            }
        }
        
        return expressions;
    }
    
    /**
     * Determina si una expresión es optimizable
     * Solo optimiza expresiones aritméticas no triviales
     */
    private boolean isOptimizableExpression(String expr) {
        if (expr == null || expr.isEmpty()) {
            return false;
        }
        
        // Debe tener al menos un operador
        if (!expr.matches(".*[+\\-*/%^].*")) {
            return false;
        }
        
        // No optimizar expresiones muy cortas (menos de 3 caracteres sin espacios)
        String compact = expr.replaceAll("\\s+", "");
        if (compact.length() < 3) {
            return false;
        }
        
        // No optimizar literales numéricos simples
        if (expr.matches("^\\d+(\\.\\d+)?$")) {
            return false;
        }
        
        // No optimizar strings
        if (expr.contains("\"") || expr.contains("'")) {
            return false;
        }
        
        // Debe contener al menos una variable o dos números con operador
        boolean hasVariable = expr.matches(".*[a-zA-Z_][a-zA-Z0-9_']*.*");
        boolean hasOperator = expr.matches(".*[+\\-*/%^].*");
        
        return hasVariable && hasOperator;
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
