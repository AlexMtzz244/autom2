import proyecto.lenguaje.codegen.CodeOptimizer;
import proyecto.lenguaje.lexer.HaskellLexer;
import proyecto.lenguaje.parser.Parser;
import proyecto.lenguaje.parser.AstNode;
import java.nio.file.*;
import java.util.*;
public class TestOptimizer {
    public static void main(String[] args) throws Exception {
        String path = "ejemplo_comentarios_completo.txt";
        String code = new String(Files.readAllBytes(Paths.get(path)), java.nio.charset.StandardCharsets.UTF_8);
        CodeOptimizer opt = new CodeOptimizer();
        CodeOptimizer.OptimizationResult res = opt.optimize(code);
        System.out.println("--- LOG ---");
        for (String l : res.log) System.out.println(l);
        System.out.println("--- OPTIMIZED ---");
        System.out.println(res.optimizedCode);
        // Intentar parsear el código optimizado para verificar errores sintácticos
        System.out.println("--- PARSE CHECK ---");
        HaskellLexer lexer = new HaskellLexer();
        java.util.List<proyecto.lenguaje.lexer.Token> tokens = lexer.tokenize(res.optimizedCode);
        System.out.println("--- TOKENS (first 120) ---");
        for (int i = 0; i < Math.min(tokens.size(), 120); i++) {
            proyecto.lenguaje.lexer.Token tk = tokens.get(i);
            System.out.printf("%3d: %s (%s) at line %d pos %d\n", i+1, tk.getValue(), tk.getType(), tk.getLine(), tk.getPosition());
        }
        Parser parser = new Parser(tokens);
        try {
            AstNode program = parser.parseProgram();
            System.out.println("Parse OK. AST size: (printed below)");
            System.out.println(program.toTreeString());
        } catch (Exception e) {
            System.out.println("Parse failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
