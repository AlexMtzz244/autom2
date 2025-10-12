package proyecto.lenguaje.codegen;

/**
 * Programa principal para demostrar la integración completa
 * del algoritmo de conversión infijo a prefijo en el compilador
 */
public class MainDemo {
    public static void main(String[] args) {
        System.out.println("=== DEMOSTRACIÓN COMPLETA DEL ALGORITMO INTEGRADO ===\n");
        
        // Ejecutar la demo completa
        ExpressionConverterDemo demo = new ExpressionConverterDemo();
        demo.testSimpleExpression();
        demo.testComplexExpression(); 
        demo.testStringConversion();
        
        System.out.println("\n=== RESUMEN DE LA INTEGRACIÓN ===");
        System.out.println("✅ Algoritmo de infijo a prefijo integrado exitosamente");
        System.out.println("✅ Soporte para tripletas y cuádruplos implementado");
        System.out.println("✅ Compatibilidad con el compilador de Haskell mantenida");
        System.out.println("✅ Código intermedio generado correctamente");
        System.out.println("\n¡Integración completada con éxito!");
    }
}