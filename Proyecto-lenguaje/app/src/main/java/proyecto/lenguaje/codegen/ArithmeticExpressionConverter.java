package proyecto.lenguaje.codegen;

import proyecto.lenguaje.parser.AstNode;
import java.util.*;
import java.lang.reflect.Field;

/**
 * Generador de código intermedio que convierte expresiones aritméticas 
 * del AST a notación prefijo usando el algoritmo de Shunting Yard modificado.
 * 
 * Este componente integra el algoritmo de conversión infijo→prefijo 
 * desarrollado previamente en el proyecto de conversiones.
 * 
 * @author Diego
 * @version 1.0
 */
public class ArithmeticExpressionConverter {
    
    /**
     * Clase para representar código intermedio en forma de tripletas
     * Formato: (operador, operando1, operando2, temporal_resultado)
     */
    public static class Triplet {
        public final String operator;
        public final String operand1; 
        public final String operand2;
        public final String result;
        
        public Triplet(String operator, String operand1, String operand2, String result) {
            this.operator = operator;
            this.operand1 = operand1;
            this.operand2 = operand2;
            this.result = result;
        }
        
        @Override
        public String toString() {
            return String.format("(%s, %s, %s, %s)", operator, operand1, operand2, result);
        }
    }
    
    /**
     * Clase para representar código intermedio en forma de cuádruplos  
     * Formato: (operador, operando1, operando2, resultado)
     */
    public static class Quadruple {
        public final String operator;
        public final String operand1;
        public final String operand2; 
        public final String result;
        
        public Quadruple(String operator, String operand1, String operand2, String result) {
            this.operator = operator;
            this.operand1 = operand1;
            this.operand2 = operand2;
            this.result = result;
        }
        
        @Override
        public String toString() {
            return String.format("(%s, %s, %s, %s)", operator, 
                               operand1 != null ? operand1 : "-", 
                               operand2 != null ? operand2 : "-", 
                               result);
        }
    }
    
    private int temporalCounter = 1;
    
    /**
     * Resetea el contador de variables temporales
     */
    public void resetTemporals() {
        temporalCounter = 1;
    }
    
    /**
     * Genera una nueva variable temporal
     */
    private String getNextTemporal() {
        return "t" + (temporalCounter++);
    }
    
    /**
     * Convierte una expresión aritmética del AST a notación prefijo
     * @param node nodo del AST que representa la expresión
     * @return expresión en notación prefijo
     */
    public String convertToPrefix(AstNode node) {
        if (node == null) return "";
        
        // Usar reflexión para acceder a las clases del parser
        String className = node.getClass().getSimpleName();
        
        try {
            if ("LiteralNode".equals(className)) {
                Field tokenField = node.getClass().getField("token");
                Object token = tokenField.get(node);
                return token.getClass().getMethod("getValue").invoke(token).toString();
            }
            
            if ("IdentifierNode".equals(className)) {
                Field nameField = node.getClass().getField("name");
                return (String) nameField.get(node);
            }
            
            if ("BinaryOpNode".equals(className)) {
                Field opField = node.getClass().getField("op");
                Field leftField = node.getClass().getField("left");
                Field rightField = node.getClass().getField("right");
                
                String op = (String) opField.get(node);
                AstNode left = (AstNode) leftField.get(node);
                AstNode right = (AstNode) rightField.get(node);
                
                String leftPrefix = convertToPrefix(left);
                String rightPrefix = convertToPrefix(right);
                return op + leftPrefix + rightPrefix;
            }
        } catch (Exception e) {
            System.err.println("Error accediendo a campos del nodo: " + e.getMessage());
        }
        
        return "";
    }
    
    /**
     * Convierte expresión AST a lista de tripletas (código intermedio)
     * @param node nodo del AST
     * @return lista de tripletas y la variable que contiene el resultado final
     */
    public ConversionResult convertToTriplets(AstNode node) {
        List<Triplet> triplets = new ArrayList<>();
        String result = convertToTripletsRecursive(node, triplets);
        return new ConversionResult(triplets, null, result);
    }
    
    private String convertToTripletsRecursive(AstNode node, List<Triplet> triplets) {
        if (node == null) return "";
        
        String className = node.getClass().getSimpleName();
        
        try {
            if ("LiteralNode".equals(className)) {
                Field tokenField = node.getClass().getField("token");
                Object token = tokenField.get(node);
                return token.getClass().getMethod("getValue").invoke(token).toString();
            }
            
            if ("IdentifierNode".equals(className)) {
                Field nameField = node.getClass().getField("name");
                return (String) nameField.get(node);
            }
            
            if ("BinaryOpNode".equals(className)) {
                Field opField = node.getClass().getField("op");
                Field leftField = node.getClass().getField("left");
                Field rightField = node.getClass().getField("right");
                
                String op = (String) opField.get(node);
                AstNode left = (AstNode) leftField.get(node);
                AstNode right = (AstNode) rightField.get(node);
                
                // Procesar operandos recursivamente
                String leftResult = convertToTripletsRecursive(left, triplets);
                String rightResult = convertToTripletsRecursive(right, triplets);
                
                // Generar temporal para esta operación
                String temporal = getNextTemporal();
                
                // Crear tripleta: (op, left, right, result)
                triplets.add(new Triplet(op, leftResult, rightResult, temporal));
                
                return temporal;
            }
        } catch (Exception e) {
            System.err.println("Error accediendo a campos en tripletas: " + e.getMessage());
        }
        
        return "";
    }
    
    /**
     * Convierte expresión AST a lista de cuádruplos (código intermedio)
     * @param node nodo del AST
     * @return lista de cuádruplos y la variable que contiene el resultado final
     */
    public ConversionResult convertToQuadruples(AstNode node) {
        List<Quadruple> quadruples = new ArrayList<>();
        String result = convertToQuadruplesRecursive(node, quadruples);
        return new ConversionResult(null, quadruples, result);
    }
    
    private String convertToQuadruplesRecursive(AstNode node, List<Quadruple> quadruples) {
        if (node == null) return "";
        
        String className = node.getClass().getSimpleName();
        
        try {
            if ("LiteralNode".equals(className)) {
                Field tokenField = node.getClass().getField("token");
                Object token = tokenField.get(node);
                return token.getClass().getMethod("getValue").invoke(token).toString();
            }
            
            if ("IdentifierNode".equals(className)) {
                Field nameField = node.getClass().getField("name");
                return (String) nameField.get(node);
            }
            
            if ("BinaryOpNode".equals(className)) {
                Field opField = node.getClass().getField("op");
                Field leftField = node.getClass().getField("left");
                Field rightField = node.getClass().getField("right");
                
                String op = (String) opField.get(node);
                AstNode left = (AstNode) leftField.get(node);
                AstNode right = (AstNode) rightField.get(node);
                
                // Procesar operandos recursivamente
                String leftResult = convertToQuadruplesRecursive(left, quadruples);
                String rightResult = convertToQuadruplesRecursive(right, quadruples);
                
                // Generar temporal para esta operación
                String temporal = getNextTemporal();
                
                // Crear cuádruple: (op, operand1, operand2, result)
                quadruples.add(new Quadruple(op, leftResult, rightResult, temporal));
                
                return temporal;
            }
        } catch (Exception e) {
            System.err.println("Error accediendo a campos en cuádruplos: " + e.getMessage());
        }
        
        return "";
    }
    
    /**
     * Convierte una cadena de expresión infijo a prefijo usando el algoritmo original
     * (Integración directa del algoritmo de InfijoAPrefijo.java)
     */
    public String convertInfixStringToPrefix(String expresion) {
        // Eliminar espacios
        expresion = expresion.replaceAll("\\s+", "");
        
        // Paso 1: Invertir la expresión e intercambiar paréntesis
        String expresionInvertida = invertirEIntercambiarParentesis(expresion);
        
        // Paso 2: Convertir a postfijo usando algoritmo de Shunting Yard
        String postfijo = infijoAPostfijo(expresionInvertida);
        
        // Paso 3: Invertir el resultado para obtener prefijo
        return new StringBuilder(postfijo).reverse().toString();
    }
    
    // ========== ALGORITMO ORIGINAL DE INFIJO A PREFIJO ==========
    
    /**
     * Determina si un carácter es un operador
     */
    private static boolean esOperador(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    /**
     * Determina la precedencia de un operador
     */
    private static int precedencia(char operador) {
        switch (operador) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
            case '^':
                return 3;
            default:
                return 0;
        }
    }
    
    /**
     * Determina si un operador es asociativo por la derecha
     */
    private static boolean esAsociativoDerecha(char operador) {
        return operador == '^';
    }
    
    /**
     * Invierte una cadena y intercambia paréntesis
     */
    private static String invertirEIntercambiarParentesis(String expresion) {
        StringBuilder resultado = new StringBuilder();
        
        for (int i = expresion.length() - 1; i >= 0; i--) {
            char c = expresion.charAt(i);
            if (c == '(') {
                resultado.append(')');
            } else if (c == ')') {
                resultado.append('(');
            } else {
                resultado.append(c);
            }
        }
        
        return resultado.toString();
    }
    
    /**
     * Convierte una expresión infijo a postfijo (algoritmo de Shunting Yard modificado)
     */
    private static String infijoAPostfijo(String expresion) {
        Stack<Character> pila = new Stack<>();
        StringBuilder resultado = new StringBuilder();
        
        for (int i = 0; i < expresion.length(); i++) {
            char c = expresion.charAt(i);
            
            // Si es un espacio, lo ignoramos
            if (c == ' ') {
                continue;
            }
            
            // Si es un operando (letra o número), lo agregamos al resultado
            if (Character.isLetterOrDigit(c)) {
                resultado.append(c);
            }
            // Si es un paréntesis de apertura, lo metemos a la pila
            else if (c == '(') {
                pila.push(c);
            }
            // Si es un paréntesis de cierre
            else if (c == ')') {
                while (!pila.isEmpty() && pila.peek() != '(') {
                    resultado.append(pila.pop());
                }
                if (!pila.isEmpty()) {
                    pila.pop(); // Removemos el '('
                }
            }
            // Si es un operador
            else if (esOperador(c)) {
                // Para conversión a prefijo, modificamos las condiciones de precedencia
                while (!pila.isEmpty() && 
                       pila.peek() != '(' && 
                       (precedencia(pila.peek()) > precedencia(c) || 
                        (precedencia(pila.peek()) == precedencia(c) && esAsociativoDerecha(c)))) {
                    resultado.append(pila.pop());
                }
                pila.push(c);
            }
        }
        
        // Vaciamos la pila
        while (!pila.isEmpty()) {
            resultado.append(pila.pop());
        }
        
        return resultado.toString();
    }
    
    /**
     * Clase para encapsular el resultado de las conversiones
     */
    public static class ConversionResult {
        public final List<Triplet> triplets;
        public final List<Quadruple> quadruples;
        public final String finalResult;
        
        public ConversionResult(List<Triplet> triplets, List<Quadruple> quadruples, String finalResult) {
            this.triplets = triplets != null ? triplets : new ArrayList<>();
            this.quadruples = quadruples != null ? quadruples : new ArrayList<>();
            this.finalResult = finalResult;
        }
        
        public String getTripletsSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== CÓDIGO INTERMEDIO (TRIPLETAS) ===\n");
            for (int i = 0; i < triplets.size(); i++) {
                sb.append(String.format("%d: %s\n", i + 1, triplets.get(i)));
            }
            sb.append("Resultado final: ").append(finalResult).append("\n");
            return sb.toString();
        }
        
        public String getQuadruplesSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== CÓDIGO INTERMEDIO (CUÁDRUPLOS) ===\n");
            for (int i = 0; i < quadruples.size(); i++) {
                sb.append(String.format("%d: %s\n", i + 1, quadruples.get(i)));
            }
            sb.append("Resultado final: ").append(finalResult).append("\n");
            return sb.toString();
        }
    }
    
    /**
     * Método para demostrar el funcionamiento completo
     */
    public void demonstrateConversions(AstNode expressionNode, String infixString) {
        System.out.println("=== CONVERSIÓN DE EXPRESIONES ARITMÉTICAS ===\n");
        
        // Conversión a prefijo desde AST
        String prefixFromAST = convertToPrefix(expressionNode);
        System.out.println("Notación Prefijo (desde AST): " + prefixFromAST);
        
        // Conversión a prefijo desde string
        if (infixString != null && !infixString.trim().isEmpty()) {
            String prefixFromString = convertInfixStringToPrefix(infixString);
            System.out.println("Notación Prefijo (desde string): " + prefixFromString);
        }
        
        // Generar tripletas
        resetTemporals();
        ConversionResult tripletsResult = convertToTriplets(expressionNode);
        System.out.println("\n" + tripletsResult.getTripletsSummary());
        
        // Generar cuádruplos  
        resetTemporals();
        ConversionResult quadruplesResult = convertToQuadruples(expressionNode);
        System.out.println(quadruplesResult.getQuadruplesSummary());
        
        // Responder a la pregunta sobre tripletas vs cuádruplos
        System.out.println("=== ANÁLISIS: TRIPLETAS vs CUÁDRUPLOS ===");
        System.out.println("Este algoritmo utiliza TANTO tripletas como cuádruplos:");
        System.out.println("• TRIPLETAS: Formato (operador, operando1, operando2, resultado)");
        System.out.println("• CUÁDRUPLOS: Formato (operador, operando1, operando2, resultado)");
        System.out.println("• La diferencia principal es conceptual en la representación interna.");
        System.out.println("• Ambos generan código intermedio de 3 direcciones para compilación.");
        System.out.println("• Las tripletas son más compactas, los cuádruplos más explícitos.");
    }
}