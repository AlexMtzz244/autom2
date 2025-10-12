package proyecto.lenguaje.codegen;

import proyecto.lenguaje.parser.*;
import proyecto.lenguaje.lexer.*;
import java.util.*;

/**
 * Clase de prueba para demostrar la integración del algoritmo de conversión
 * infijo a prefijo en el compilador de Haskell.
 * 
 * Esta clase demuestra las capacidades del ArithmeticExpressionConverter:
 * - Conversión de expresiones a notación prefijo
 * - Generación de código intermedio usando tripletas
 * - Generación de código intermedio usando cuádruplos
 * 
 * @author Diego
 * @version 1.0
 */
public class ExpressionConverterDemo {
    
    public static void main(String[] args) {
        System.out.println("=== DEMOSTRACIÓN DEL CONVERSOR DE EXPRESIONES ARITMÉTICAS ===\n");
        
        ExpressionConverterDemo demo = new ExpressionConverterDemo();
        
        // Ejemplo 1: Expresión simple
        demo.testSimpleExpression();
        
        // Ejemplo 2: Expresión compleja
        demo.testComplexExpression();
        
        // Ejemplo 3: Solo algoritmo de string
        demo.testStringConversion();
    }
    
    public void testSimpleExpression() {
        System.out.println("=== PRUEBA 1: EXPRESIÓN SIMPLE (A + B) ===");
        
        ArithmeticExpressionConverter converter = new ArithmeticExpressionConverter();
        
        // Crear AST manualmente para A + B
        AstNode left = createIdentifierNode("A");
        AstNode right = createIdentifierNode("B");  
        AstNode expression = createBinaryOpNode("+", left, right);
        
        // Demostrar todas las conversiones
        converter.demonstrateConversions(expression, "A+B");
        System.out.println();
    }
    
    public void testComplexExpression() {
        System.out.println("=== PRUEBA 2: EXPRESIÓN COMPLEJA ((A + B) * C) ===");
        
        ArithmeticExpressionConverter converter = new ArithmeticExpressionConverter();
        
        // Crear AST para (A + B) * C
        AstNode a = createIdentifierNode("A");
        AstNode b = createIdentifierNode("B");
        AstNode c = createIdentifierNode("C");
        
        AstNode sum = createBinaryOpNode("+", a, b);
        AstNode expression = createBinaryOpNode("*", sum, c);
        
        // Demostrar todas las conversiones
        converter.demonstrateConversions(expression, "(A+B)*C");
        System.out.println();
    }
    
    public void testStringConversion() {
        System.out.println("=== PRUEBA 3: CONVERSIÓN SOLO DE STRINGS ===");
        
        ArithmeticExpressionConverter converter = new ArithmeticExpressionConverter();
        
        String[] expressions = {
            "A+B*C",
            "(A+B)*C", 
            "A^B+C",
            "A+B^C*D",
            "(A+B)*(C-D)"
        };
        
        System.out.println("Demostrando el algoritmo original de InfijoAPrefijo integrado:");
        
        for (String expr : expressions) {
            String prefix = converter.convertInfixStringToPrefix(expr);
            System.out.println("Infijo:  " + expr);
            System.out.println("Prefijo: " + prefix);
            System.out.println();
        }
        
        // Respuesta final sobre tripletas vs cuádruplos
        System.out.println("=== RESPUESTA A LA PREGUNTA ===");
        System.out.println("¿El algoritmo usa tripletas o cuádruplos?");
        System.out.println("RESPUESTA: El algoritmo puede generar AMBOS tipos de código intermedio:");
        System.out.println("• TRIPLETAS: Representación (operador, operando1, operando2, resultado)");
        System.out.println("• CUÁDRUPLOS: Representación (operador, operando1, operando2, resultado)");
        System.out.println("• La diferencia es principalmente conceptual y de implementación.");
        System.out.println("• Ambos generan código de 3 direcciones útil para la compilación.");
        System.out.println("• Las tripletas son más compactas, los cuádruplos más explícitos.");
        System.out.println("• El convertidor integrado soporta ambos formatos según la necesidad.");
    }
    
    /**
     * Métodos auxiliares para crear nodos del AST usando reflexión
     */
    private AstNode createIdentifierNode(String name) {
        try {
            Class<?> identifierClass = Class.forName("proyecto.lenguaje.parser.IdentifierNode");
            return (AstNode) identifierClass.getConstructor(String.class).newInstance(name);
        } catch (Exception e) {
            System.err.println("Error creando IdentifierNode: " + e.getMessage());
            return null;
        }
    }
    
    private AstNode createBinaryOpNode(String op, AstNode left, AstNode right) {
        try {
            Class<?> binaryOpClass = Class.forName("proyecto.lenguaje.parser.BinaryOpNode");
            return (AstNode) binaryOpClass.getConstructor(String.class, AstNode.class, AstNode.class)
                                          .newInstance(op, left, right);
        } catch (Exception e) {
            System.err.println("Error creando BinaryOpNode: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Método para integrar con el lexer y parser existentes
     */
    public void testWithLexerAndParser(String sourceCode) {
        try {
            System.out.println("=== PRUEBA CON LEXER Y PARSER COMPLETO ===");
            System.out.println("Código fuente: " + sourceCode);
            
            // Usar el lexer existente
            HaskellLexer lexer = new HaskellLexer();
            List<Token> tokens = lexer.tokenize(sourceCode);
            
            // Usar el parser existente
            Parser parser = new Parser(tokens);
            AstNode ast = parser.parseProgram();
            
            // Buscar expresiones aritméticas en el AST y convertirlas
            ArithmeticExpressionConverter converter = new ArithmeticExpressionConverter();
            extractAndConvertExpressions(ast, converter);
            
        } catch (Exception e) {
            System.err.println("Error en prueba completa: " + e.getMessage());
        }
    }
    
    private void extractAndConvertExpressions(AstNode node, ArithmeticExpressionConverter converter) {
        if (node == null) return;
        
        String className = node.getClass().getSimpleName();
        
        if ("BinaryOpNode".equals(className)) {
            System.out.println("Expresión aritmética encontrada:");
            converter.demonstrateConversions(node, null);
        }
        
        // Recursivamente buscar en subnodos (usando reflexión)
        try {
            java.lang.reflect.Field[] fields = node.getClass().getFields();
            for (java.lang.reflect.Field field : fields) {
                if (AstNode.class.isAssignableFrom(field.getType())) {
                    AstNode subNode = (AstNode) field.get(node);
                    extractAndConvertExpressions(subNode, converter);
                } else if (List.class.isAssignableFrom(field.getType())) {
                    Object listObj = field.get(node);
                    if (listObj instanceof List) {
                        List<?> list = (List<?>) listObj;
                        for (Object item : list) {
                            if (item instanceof AstNode) {
                                extractAndConvertExpressions((AstNode) item, converter);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorar errores de reflexión
        }
    }
}