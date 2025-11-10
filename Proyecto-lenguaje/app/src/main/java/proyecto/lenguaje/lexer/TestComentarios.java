package proyecto.lenguaje.lexer;

import java.util.List;

public class TestComentarios {
    public static void main(String[] args) {
        HaskellLexer lexer = new HaskellLexer();
        
        String codigo = 
            "-- Este es un comentario\n" +
            "x = 5 -- comentario al final\n" +
            "y = 10\n" +
            "{- comentario multilínea\n" +
            "   continúa aquí -}\n" +
            "z = x + y\n";
        
        System.out.println("=== CÓDIGO DE PRUEBA ===");
        System.out.println(codigo);
        System.out.println("\n=== TOKENS GENERADOS ===");
        
        List<Token> tokens = lexer.tokenize(codigo);
        
        for (Token token : tokens) {
            System.out.println(token);
        }
        
        System.out.println("\n=== RESUMEN ===");
        System.out.println("Total de tokens: " + tokens.size());
        
        // Verificar que no hay tokens ERROR
        long errorCount = tokens.stream()
            .filter(t -> t.getType() == Token.Type.ERROR)
            .count();
        
        if (errorCount == 0) {
            System.out.println("✅ Todos los comentarios fueron procesados correctamente");
        } else {
            System.out.println("❌ Se encontraron " + errorCount + " errores");
        }
    }
}
