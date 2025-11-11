package proyecto.lenguaje.gui;

import proyecto.lenguaje.lexer.*;
import proyecto.lenguaje.parser.*; // Nuevo import para el parser
import proyecto.lenguaje.codegen.ArithmeticExpressionConverter; // Nuevo import para el conversor
import proyecto.lenguaje.codegen.CodeOptimizer; // Nuevo import para el optimizador
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class IDEFrame extends JFrame {
    private JTextArea codeEditor;
    private JTextArea lineNumbers;
    private JEditorPane outputArea;
    private JButton lexButton, saveButton, saveAsButton, semanticButton;
    private JButton parseButton; // nuevo botón
    private JButton expressionButton; // botón para conversión de expresiones
    private JButton optimizeButton; // botón para optimización de código
    private JFileChooser fileChooser;
    private File currentFile;
    private JScrollPane mainScrollPane; // Nuevo scroll pane principal

    public IDEFrame() {
        setTitle("Mini IDE - Evaluación de Lenguaje Haskell");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLayout(new BorderLayout());

        // Crear editor con números de línea
        codeEditor = new JTextArea();
        codeEditor.setFont(new Font("monospaced", Font.PLAIN, 12));
        
        lineNumbers = new JTextArea("1");
        lineNumbers.setEditable(false);
        lineNumbers.setBackground(Color.LIGHT_GRAY);
        lineNumbers.setFont(new Font("monospaced", Font.PLAIN, 12));
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        
        // Configurar el panel principal del editor
        mainScrollPane = new JScrollPane(codeEditor);
        mainScrollPane.setRowHeaderView(lineNumbers);
        mainScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Configurar el área de salida
        outputArea = new JEditorPane();
        outputArea.setEditable(false);
        outputArea.setContentType("text/html"); // Para soportar HTML
        JScrollPane outputScroll = new JScrollPane(outputArea);
        
        // Configurar actualizaciones de números de línea
        codeEditor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateLineNumbers(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateLineNumbers(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateLineNumbers(); }
        });

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(outputScroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 5, 5)); // Cambiar a 4x2 para incluir optimización
        lexButton = new JButton("Análisis Léxico");
        parseButton = new JButton("Análisis Sintáctico");
        semanticButton = new JButton("Validación Semántica");
        expressionButton = new JButton("Conversión Infijo→Prefijo");
        optimizeButton = new JButton("🚀 Optimizar Código");
        saveButton = new JButton("Guardar Cambios");
        saveAsButton = new JButton("Guardar Como");
        
        buttonPanel.add(lexButton);
        buttonPanel.add(parseButton);
        buttonPanel.add(semanticButton);
        buttonPanel.add(expressionButton);
        buttonPanel.add(optimizeButton); // agregar el botón de optimización
        buttonPanel.add(new JLabel()); // espacio vacío
        buttonPanel.add(saveButton);
        buttonPanel.add(saveAsButton);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Crear split pane con el editor y el panel derecho
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainScrollPane, rightPanel);
        splitPane.setDividerLocation(600);
        add(splitPane, BorderLayout.CENTER);

        fileChooser = new JFileChooser();

        lexButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { runLexicalAnalysis(); }
        });
        semanticButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { runSemanticCycleValidation(); }
        });
        parseButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { runParser(); }
        });
        expressionButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { runExpressionConversion(); }
        });
        optimizeButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { runCodeOptimization(); }
        });
        saveButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { saveFile(); }
        });
        saveAsButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { saveFileAs(); }
        });
        updateLineNumbers(); // Inicializar números de línea
    }

    // Método para actualizar los números de línea
    private void updateLineNumbers() {
        String text = codeEditor.getText();
        int lines = text.split("\n", -1).length;
        if (text.isEmpty()) lines = 1;
        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            numbers.append(i).append("\n");
        }
        lineNumbers.setText(numbers.toString());

        // Calcular ancho según número de dígitos (existente)
        int widthDigits = String.valueOf(lines).length();
        int charWidth = lineNumbers.getFontMetrics(lineNumbers.getFont()).charWidth('0');

        // Nuevo: calcular la altura total en píxeles y actualizar preferredSize
        int lineHeight = lineNumbers.getFontMetrics(lineNumbers.getFont()).getHeight();
        int totalHeight = lineHeight * lines;

        lineNumbers.setPreferredSize(new Dimension((widthDigits + 2) * charWidth + 10, totalHeight));
        lineNumbers.revalidate();
        lineNumbers.repaint();

        // Revalidar y repintar el scroll pane (para forzar actualización del row header y el scroll)
        if (mainScrollPane != null) {
            mainScrollPane.revalidate();
            mainScrollPane.repaint();
        }
    }

    // Validación semántica de ciclos
    private void runSemanticCycleValidation() {
        long startTime = System.currentTimeMillis();
        
        HaskellLexer lexer = new HaskellLexer();
        String code = codeEditor.getText();
        List<Token> tokens = lexer.tokenize(code);
        String result = SemanticValidator.validateCycles(tokens);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Agregar mensaje de éxito al inicio si no hay errores semánticos
        String successPrefix = "";
        if (!result.contains("ERROR") && !result.contains("ERRORES SEMÁNTICOS")) {
            successPrefix = "<span style='color: green; font-weight: bold;'>✅ VALIDACIÓN SEMÁNTICA EXITOSA</span><br>" +
                           "<span style='color: green;'>No se encontraron errores semánticos en el código.</span><br>" +
                           "<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>" + executionTime + " ms</b></span><br><br>" +
                           "<span style='color: blue; font-weight: bold;'>Resultado del análisis:</span><br><br>";
        } else {
            successPrefix = "<span style='color: red; font-weight: bold;'>❌ ERRORES SEMÁNTICOS ENCONTRADOS</span><br>" +
                           "<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>" + executionTime + " ms</b></span><br><br>";
        }
        
        // Convertir texto plano a HTML básico para mantener formato
        String htmlResult = "<html><body style='font-family: monospace; white-space: pre;'>" 
                          + successPrefix
                          + escapeHtml(result).replace("\n", "<br>") 
                          + "</body></html>";
        outputArea.setText(htmlResult);
    }

    private void runLexicalAnalysis() {
        long startTime = System.currentTimeMillis();
        
        HaskellLexer lexer = new HaskellLexer();
        String code = codeEditor.getText();
        List<Token> tokens = lexer.tokenize(code);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        StringBuilder sb = new StringBuilder();
        
        // Iniciar HTML
        sb.append("<html><body style='font-family: monospace;'>");
        
        int errorCount = 0;
        for (Token t : tokens) {
            if (t.getType() == Token.Type.ERROR) {
                // Mostrar errores en rojo
                sb.append("<span style='color: red; font-weight: bold;'>")
                  .append(escapeHtml(t.toString()))
                  .append("</span><br>");
                errorCount++;
            } else {
                // Mostrar tokens normales
                sb.append(escapeHtml(t.toString())).append("<br>");
            }
        }
        
        sb.append("<br><strong>--- RESUMEN ---</strong><br>");
        sb.append("Total de tokens: ").append(tokens.size()).append("<br>");
        sb.append("Errores léxicos: ").append(errorCount).append("<br>");
        sb.append("<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>").append(executionTime).append(" ms</b></span><br>");
        
        // Detectar comentarios en el código fuente
        long lineComments = code.lines().filter(line -> line.trim().startsWith("--")).count();
        boolean hasBlockComments = code.contains("{-") && code.contains("-}");
        
        if (lineComments > 0 || hasBlockComments) {
            sb.append("<br><span style='color: blue;'>📝 Comentarios procesados:</span><br>");
            if (lineComments > 0) {
                sb.append("  - Comentarios de línea (--): ").append(lineComments).append("<br>");
            }
            if (hasBlockComments) {
                sb.append("  - Comentarios multilínea ({- -}): Sí<br>");
            }
            sb.append("<span style='color: gray; font-size: 10px;'>(Los comentarios se ignoran durante el análisis léxico)</span><br>");
        }
        
        if (errorCount == 0) {
            sb.append("<br><span style='color: green; font-weight: bold;'>✅ ANÁLISIS LÉXICO EXITOSO</span><br>");
            sb.append("<span style='color: green;'>Todos los tokens han sido reconocidos correctamente.</span><br>");
        } else {
            sb.append("<br><strong style='color: red;'>--- ERRORES ENCONTRADOS ---</strong><br>");
            for (Token t : tokens) {
                if (t.getType() == Token.Type.ERROR) {
                    sb.append("<span style='color: red;'>ERROR: Carácter inválido '")
                      .append(escapeHtml(t.getValue()))
                      .append("' en línea ").append(t.getLine())
                      .append(", posición ").append(t.getPosition()).append("</span><br>");
                }
            }
        }
        
        sb.append("</body></html>");
        outputArea.setText(sb.toString());
    }
    
    // Nuevo: ejecutar conversión de expresiones aritméticas
    private void runExpressionConversion() {
        long startTime = System.currentTimeMillis();
        
        try {
            HaskellLexer lexer = new HaskellLexer();
            String code = codeEditor.getText();
            
            ArithmeticExpressionConverter converter = new ArithmeticExpressionConverter();
            
            StringBuilder result = new StringBuilder();
            result.append("<html><body style='font-family: monospace;'>");
            result.append("<span style='color: green; font-weight: bold;'>✅ CONVERSIÓN DE EXPRESIONES ARITMÉTICAS</span><br>");
            result.append("<span style='color: blue;'>Análisis completo del código fuente</span><br>");
            
            // PRIMERO: Buscar expresiones directamente en el código fuente
            List<ExpressionWithVariable> foundExpressions = extractExpressionsFromSourceCode(code);
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            result.append("<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>").append(executionTime).append(" ms</b></span><br><br>");
            
            if (foundExpressions.isEmpty()) {
                // Si no encontramos expresiones, intentar con el parser
                try {
                    List<Token> tokens = lexer.tokenize(code);
                    Parser parser = new Parser(tokens);
                    AstNode program = parser.parseProgram();
                    
                    List<ExpressionResult> expressions = findArithmeticExpressions(program, converter);
                    
                    if (expressions.isEmpty()) {
                        showNoExpressionsMessage(result, converter);
                    } else {
                        displayASTExpressions(expressions, result, converter);
                    }
                } catch (Exception parseEx) {
                    showParseErrorMessage(result, converter, parseEx.getMessage());
                }
            } else {
                // Mostrar expresiones encontradas directamente del código
                result.append("<span style='color: green; font-weight: bold;'>🔍 EXPRESIONES ENCONTRADAS: ").append(foundExpressions.size()).append("</span><br><br>");
                
                // Construir mapa de variables con sus valores
                java.util.Map<String, Double> variableValues = buildVariableMap(code);
                
                int count = 1;
                for (ExpressionWithVariable exprWithVar : foundExpressions) {
                    try {
                        String expr = exprWithVar.expression;
                        String varName = exprWithVar.variableName;
                        int lineNum = exprWithVar.lineNumber;
                        String cleanExpr = cleanExpression(expr);
                        String prefix = converter.convertInfixStringToPrefix(cleanExpr);
                        
                        result.append("<span style='color: purple; font-weight: bold;'>--- EXPRESIÓN ").append(count++).append(" ---</span>");
                        result.append(" <span style='color: gray; font-style: italic;'>(Línea ").append(lineNum).append(")</span><br>");
                        result.append("<span style='color: navy;'>Original:</span> ").append(escapeHtml(expr)).append("<br>");
                        result.append("<span style='color: darkblue;'>Limpia:</span> ").append(escapeHtml(cleanExpr)).append("<br>");
                        result.append("<span style='color: darkgreen;'>Prefijo:</span> ").append(escapeHtml(prefix)).append("<br>");
                        
                        // Intentar evaluar numéricamente
                        String evaluation = evaluateExpression(cleanExpr, variableValues);
                        result.append("<span style='color: darkmagenta;'>Evaluación:</span> ").append(escapeHtml(evaluation)).append("<br>");
                        
                        // Generar tripletas simuladas con resultado final
                        result.append("<span style='color: darkred;'>Tripletas (simuladas):</span><br>");
                        generateSimulatedTriplets(cleanExpr, result, prefix, varName);
                        
                        result.append("<br>");
                        
                    } catch (Exception exprEx) {
                        result.append("<span style='color: orange;'>Error procesando: ").append(escapeHtml(exprWithVar.expression)).append("</span><br><br>");
                    }
                }
                
                // Información técnica
                result.append("<span style='color: blue; font-weight: bold;'>📋 INFORMACIÓN TÉCNICA:</span><br>");
                result.append("• <span style='color: darkred;'>Tripletas:</span> Código intermedio (operador, operando1, operando2, resultado)<br>");
                result.append("• <span style='color: darkorange;'>Cuádruplos:</span> Similar a tripletas, formato explícito<br>");
                result.append("• <span style='color: darkgreen;'>Prefijo:</span> Operador precede a operandos<br>");
                result.append("• <span style='color: navy;'>Algoritmo:</span> Shunting Yard modificado<br>");
            }
            
            result.append("</body></html>");
            outputArea.setText(result.toString());
            
        } catch (Exception ex) {
            String errorResult = "<html><body style='font-family: monospace; color: red;'>";
            errorResult += "<span style='font-weight: bold;'>❌ ERROR EN CONVERSIÓN DE EXPRESIONES</span><br><br>";
            errorResult += "Error: " + escapeHtml(ex.getMessage()) + "<br><br>";
            errorResult += "Intenta con código válido como:<br>";
            errorResult += "• let resultado = x + y in resultado<br>";
            errorResult += "• suma a b = a + b<br>";
            errorResult += "</body></html>";
            outputArea.setText(errorResult);
        }
    }
    
    // Clase auxiliar para almacenar expresiones con su variable de asignación
    private static class ExpressionWithVariable {
        String variableName;
        String expression;
        int lineNumber;
        
        ExpressionWithVariable(String variableName, String expression, int lineNumber) {
            this.variableName = variableName;
            this.expression = expression;
            this.lineNumber = lineNumber;
        }
    }
    
    // Método mejorado para extraer expresiones del código fuente
    private List<ExpressionWithVariable> extractExpressionsFromSourceCode(String code) {
        List<ExpressionWithVariable> expressions = new ArrayList<>();
        String[] lines = code.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Buscar patrón: algo = expresión [in algo]
            if (line.contains("=")) {
                int equalsIndex = line.indexOf("=");
                if (equalsIndex > 0 && equalsIndex < line.length() - 1) {
                    String leftSide = line.substring(0, equalsIndex).trim();
                    String rightSide = line.substring(equalsIndex + 1).trim();
                    
                    // Si hay "in", tomar solo la parte antes del "in"
                    if (rightSide.contains(" in ")) {
                        rightSide = rightSide.substring(0, rightSide.indexOf(" in ")).trim();
                    }
                    
                    // Verificar si contiene operadores aritméticos
                    if (containsArithmeticOperator(rightSide) && rightSide.length() > 0) {
                        // Extraer solo el nombre de la variable (primera palabra)
                        String varName = leftSide.split("\\s+")[0];
                        expressions.add(new ExpressionWithVariable(varName, rightSide, i + 1)); // i+1 para línea basada en 1
                    }
                }
            }
        }
        
        return expressions;
    }
    
    // Método para construir un mapa de variables con sus valores numéricos
    private java.util.Map<String, Double> buildVariableMap(String code) {
        java.util.Map<String, Double> variables = new java.util.HashMap<>();
        String[] lines = code.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            
            // Ignorar comentarios y líneas vacías
            if (line.startsWith("--") || line.isEmpty()) {
                continue;
            }
            
            // Buscar patrón: variable = número
            if (line.contains("=")) {
                int equalsIndex = line.indexOf("=");
                if (equalsIndex > 0 && equalsIndex < line.length() - 1) {
                    String leftSide = line.substring(0, equalsIndex).trim();
                    String rightSide = line.substring(equalsIndex + 1).trim();
                    
                    // Extraer solo el nombre de la variable (primera palabra)
                    String varName = leftSide.split("\\s+")[0];
                    
                    // Intentar parsear el valor como número
                    try {
                        // Eliminar espacios y verificar si es un número
                        String cleanValue = rightSide.trim();
                        if (cleanValue.matches("-?\\d+(\\.\\d+)?")) {
                            double value = Double.parseDouble(cleanValue);
                            variables.put(varName, value);
                        }
                    } catch (NumberFormatException e) {
                        // No es un número, ignorar
                    }
                }
            }
        }
        
        return variables;
    }
    
    // Método para evaluar una expresión aritmética con valores reales
    private String evaluateExpression(String expr, java.util.Map<String, Double> variables) {
        try {
            // Reemplazar variables por sus valores
            String evaluatedExpr = expr;
            for (java.util.Map.Entry<String, Double> entry : variables.entrySet()) {
                String varName = entry.getKey();
                Double value = entry.getValue();
                
                // Reemplazar la variable con su valor (asegurarse de reemplazar palabras completas)
                evaluatedExpr = evaluatedExpr.replaceAll("\\b" + varName + "\\b", 
                    value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value));
            }
            
            // Evaluar la expresión resultante
            double result = evaluateArithmeticExpression(evaluatedExpr);
            
            // Formatear el resultado
            if (result % 1 == 0) {
                return String.format("%s = %.0f", evaluatedExpr, result);
            } else {
                return String.format("%s = %.2f", evaluatedExpr, result);
            }
        } catch (Exception e) {
            return expr + " (no se pudo evaluar)";
        }
    }
    
    // Método para evaluar expresiones aritméticas simples
    private double evaluateArithmeticExpression(String expr) throws Exception {
        // Eliminar espacios
        final String expression = expr.replaceAll("\\s+", "");
        
        // Usar un evaluador simple (para expresiones básicas)
        return new Object() {
            int pos = -1, ch;
            
            void nextChar() {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
            }
            
            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }
            
            double parse() throws Exception {
                nextChar();
                double x = parseExpression();
                if (pos < expression.length()) throw new Exception("Unexpected: " + (char)ch);
                return x;
            }
            
            double parseExpression() throws Exception {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }
            
            double parseTerm() throws Exception {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else if (eat('%')) x %= parseFactor();
                    else return x;
                }
            }
            
            double parseFactor() throws Exception {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();
                
                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expression.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    throw new Exception("Variable not substituted: " + expression.substring(startPos, this.pos));
                } else {
                    throw new Exception("Unexpected: " + (char)ch);
                }
                
                if (eat('^')) x = Math.pow(x, parseFactor());
                
                return x;
            }
        }.parse();
    }
    
    private boolean containsArithmeticOperator(String expr) {
        return expr.matches(".*[+\\-*/^%].*");
    }
    
    private String cleanExpression(String expr) {
        // Eliminar espacios extra y limpiar la expresión
        return expr.trim().replaceAll("\\s+", "");
    }
    
    private String generateSimulatedTriplets(String expr, StringBuilder result, String prefix, String varName) {
        // Simulación simple de tripletas basada en la expresión
        String finalTemp = "t1";
        
        // Para expresiones simples como "x+y*z"
        if (expr.matches("\\w+[+\\-]\\w+[*/]\\w+")) {
            // Ejemplo: x+y*z -> primero y*z, luego x+resultado
            char[] chars = expr.toCharArray();
            String var1 = "", op1 = "", var2 = "", op2 = "", var3 = "";
            
            int i = 0;
            while (i < chars.length && Character.isLetterOrDigit(chars[i])) {
                var1 += chars[i++];
            }
            if (i < chars.length) op1 = String.valueOf(chars[i++]);
            while (i < chars.length && Character.isLetterOrDigit(chars[i])) {
                var2 += chars[i++];
            }
            if (i < chars.length) op2 = String.valueOf(chars[i++]);
            while (i < chars.length && Character.isLetterOrDigit(chars[i])) {
                var3 += chars[i++];
            }
            
            if (!var3.isEmpty()) {
                // Precedencia: * y / antes que + y -
                if (op2.equals("*") || op2.equals("/")) {
                    result.append("  1: (").append(op2).append(", ").append(var2).append(", ").append(var3).append(", t1)<br>");
                    result.append("  2: (").append(op1).append(", ").append(var1).append(", t1, t2)<br>");
                    finalTemp = "t2";
                    if (varName != null && !varName.isEmpty()) {
                        result.append("  3: (=, t2, -, ").append(escapeHtml(varName)).append(")<br>");
                    }
                    result.append("<span style='color: darkred;'>Resultado final:</span> ");
                    if (varName != null && !varName.isEmpty()) {
                        result.append(escapeHtml(varName)).append("=");
                    }
                    result.append("t2=").append(escapeHtml(expr)).append("<br>");
                } else {
                    result.append("  1: (").append(op1).append(", ").append(var1).append(", ").append(var2).append(", t1)<br>");
                    result.append("  2: (").append(op2).append(", t1, ").append(var3).append(", t2)<br>");
                    finalTemp = "t2";
                    if (varName != null && !varName.isEmpty()) {
                        result.append("  3: (=, t2, -, ").append(escapeHtml(varName)).append(")<br>");
                    }
                    result.append("<span style='color: darkred;'>Resultado final:</span> ");
                    if (varName != null && !varName.isEmpty()) {
                        result.append(escapeHtml(varName)).append("=");
                    }
                    result.append("t2=").append(escapeHtml(expr)).append("<br>");
                }
            }
        } else if (expr.contains("(") && expr.contains(")")) {
            // Expresión con paréntesis - más detallada
            result.append("  1: (operación_interna, -, -, t1)<br>");
            result.append("  2: (operación_externa, t1, -, t2)<br>");
            finalTemp = "t2";
            if (varName != null && !varName.isEmpty()) {
                result.append("  3: (=, t2, -, ").append(escapeHtml(varName)).append(")<br>");
            }
            result.append("<span style='color: darkred;'>Resultado final:</span> ");
            if (varName != null && !varName.isEmpty()) {
                result.append(escapeHtml(varName)).append("=");
            }
            result.append("t2=").append(escapeHtml(expr)).append("<br>");
        } else if (expr.matches("\\w+[+\\-*/^%]\\w+")) {
            // Expresión simple binaria
            String[] parts = expr.split("[+\\-*/^%]");
            String op = expr.replaceAll("[\\w]+", "");
            if (parts.length == 2 && op.length() == 1) {
                result.append("  1: (").append(op).append(", ").append(parts[0]).append(", ").append(parts[1]).append(", t1)<br>");
                finalTemp = "t1";
                if (varName != null && !varName.isEmpty()) {
                    result.append("  2: (=, t1, -, ").append(escapeHtml(varName)).append(")<br>");
                }
                result.append("<span style='color: darkred;'>Resultado final:</span> ");
                if (varName != null && !varName.isEmpty()) {
                    result.append(escapeHtml(varName)).append("=");
                }
                result.append("t1=").append(escapeHtml(expr)).append("<br>");
            }
        } else {
            result.append("  1: (expresión_compleja, -, -, t1)<br>");
            finalTemp = "t1";
            if (varName != null && !varName.isEmpty()) {
                result.append("  2: (=, t1, -, ").append(escapeHtml(varName)).append(")<br>");
            }
            result.append("<span style='color: darkred;'>Resultado final:</span> ");
            if (varName != null && !varName.isEmpty()) {
                result.append(escapeHtml(varName)).append("=");
            }
            result.append("t1=").append(escapeHtml(expr)).append("<br>");
        }
        
        return finalTemp;
    }
    
    private void showNoExpressionsMessage(StringBuilder result, ArithmeticExpressionConverter converter) {
        result.append("<span style='color: orange; font-weight: bold;'>⚠️ No se encontraron expresiones aritméticas</span><br>");
        result.append("<span style='color: gray;'>El código no contiene operaciones aritméticas detectables</span><br><br>");
        
        result.append("<span style='color: blue; font-weight: bold;'>💡 Ejemplos correctos:</span><br>");
        result.append("• <code>let resultado = x + y * z in resultado</code><br>");
        result.append("• <code>suma a b = a + b</code><br>");
        result.append("• <code>let valor = (a + b) * c in valor</code><br><br>");
        
        showDemonstration(result, converter);
    }
    
    private void showParseErrorMessage(StringBuilder result, ArithmeticExpressionConverter converter, String error) {
        result.append("<span style='color: orange; font-weight: bold;'>⚠️ Error de sintaxis detectado</span><br>");
        result.append("<span style='color: gray;'>").append(escapeHtml(error)).append("</span><br><br>");
        
        result.append("<span style='color: blue; font-weight: bold;'>💡 Sintaxis correcta:</span><br>");
        result.append("• Use: <code>let variable = expresión in variable</code><br>");
        result.append("• O: <code>función parámetros = expresión</code><br><br>");
        
        showDemonstration(result, converter);
    }
    
    private void showDemonstration(StringBuilder result, ArithmeticExpressionConverter converter) {
        result.append("<span style='color: purple; font-weight: bold;'>🚀 DEMOSTRACIÓN:</span><br><br>");
        String[] examples = {"x+y*z", "(a+b)*c", "a^b", "x+y"};
        
        for (String example : examples) {
            String prefix = converter.convertInfixStringToPrefix(example);
            result.append("<span style='color: navy;'>Infijo:</span> ").append(example).append("<br>");
            result.append("<span style='color: darkgreen;'>Prefijo:</span> ").append(prefix).append("<br><br>");
        }
    }
    
    private void displayASTExpressions(List<ExpressionResult> expressions, StringBuilder result, ArithmeticExpressionConverter converter) {
        result.append("<span style='color: green; font-weight: bold;'>🔍 EXPRESIONES DEL AST: ").append(expressions.size()).append("</span><br><br>");
        
        int count = 1;
        for (ExpressionResult expr : expressions) {
            String cleanExpr = cleanExpression(expr.originalExpression);
            
            result.append("<span style='color: purple; font-weight: bold;'>--- EXPRESIÓN ").append(count++).append(" ---</span><br>");
            result.append("<span style='color: navy;'>Original:</span> ").append(escapeHtml(expr.originalExpression)).append("<br>");
            result.append("<span style='color: darkblue;'>Limpia:</span> ").append(escapeHtml(cleanExpr)).append("<br>");
            result.append("<span style='color: darkgreen;'>Prefijo:</span> ").append(escapeHtml(expr.prefixNotation)).append("<br>");
            
            if (!expr.tripletsResult.triplets.isEmpty()) {
                result.append("<span style='color: darkred;'>Tripletas (simuladas):</span><br>");
                for (int i = 0; i < expr.tripletsResult.triplets.size(); i++) {
                    result.append("  ").append(i + 1).append(": ").append(escapeHtml(expr.tripletsResult.triplets.get(i).toString())).append("<br>");
                }
                // Agregar resultado final con operador de asignación
                String finalResult = expr.tripletsResult.finalResult;
                result.append("<span style='color: darkred;'>Resultado final:</span> ")
                      .append(finalResult).append("=").append(escapeHtml(cleanExpr)).append("<br>");
            }
            
            result.append("<br>");
        } 
    }
    
    // Clase auxiliar para almacenar resultados de expresiones
    private static class ExpressionResult {
        String originalExpression;
        String prefixNotation;
        ArithmeticExpressionConverter.ConversionResult tripletsResult;
        
        ExpressionResult(String original, String prefix, 
                        ArithmeticExpressionConverter.ConversionResult triplets) {
            this.originalExpression = original;
            this.prefixNotation = prefix;
            this.tripletsResult = triplets;
        }
    }
    
    // Método para buscar expresiones aritméticas en el AST
    private List<ExpressionResult> findArithmeticExpressions(AstNode node, ArithmeticExpressionConverter converter) {
        List<ExpressionResult> results = new ArrayList<>();
        findArithmeticExpressionsRecursive(node, converter, results);
        return results;
    }
    
    private void findArithmeticExpressionsRecursive(AstNode node, ArithmeticExpressionConverter converter, List<ExpressionResult> results) {
        if (node == null) return;
        
        String className = node.getClass().getSimpleName();
        
        // Si encontramos una expresión binaria (aritmética)
        if ("BinaryOpNode".equals(className)) {
            try {
                // Obtener la representación original de la expresión
                String original = getExpressionString(node);
                
                // Convertir a prefijo
                String prefix = converter.convertToPrefix(node);
                
                // Generar tripletas
                converter.resetTemporals();
                ArithmeticExpressionConverter.ConversionResult triplets = converter.convertToTriplets(node);
                
                results.add(new ExpressionResult(original, prefix, triplets));
                
            } catch (Exception e) {
                // Si hay error al procesar una expresión, continuar con las otras
                System.err.println("Error procesando expresión: " + e.getMessage());
            }
        }
        
        // Buscar recursivamente en subnodos usando reflexión
        try {
            java.lang.reflect.Field[] fields = node.getClass().getFields();
            for (java.lang.reflect.Field field : fields) {
                if (AstNode.class.isAssignableFrom(field.getType())) {
                    AstNode subNode = (AstNode) field.get(node);
                    findArithmeticExpressionsRecursive(subNode, converter, results);
                } else if (List.class.isAssignableFrom(field.getType())) {
                    Object listObj = field.get(node);
                    if (listObj instanceof List) {
                        List<?> list = (List<?>) listObj;
                        for (Object item : list) {
                            if (item instanceof AstNode) {
                                findArithmeticExpressionsRecursive((AstNode) item, converter, results);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorar errores de reflexión
        }
    }
    
    // Método para obtener representación string de una expresión
    private String getExpressionString(AstNode node) {
        if (node == null) return "";
        
        String className = node.getClass().getSimpleName();
        
        try {
            if ("LiteralNode".equals(className)) {
                java.lang.reflect.Field tokenField = node.getClass().getField("token");
                Object token = tokenField.get(node);
                return token.getClass().getMethod("getValue").invoke(token).toString();
            }
            
            if ("IdentifierNode".equals(className)) {
                java.lang.reflect.Field nameField = node.getClass().getField("name");
                return (String) nameField.get(node);
            }
            
            if ("BinaryOpNode".equals(className)) {
                java.lang.reflect.Field opField = node.getClass().getField("op");
                java.lang.reflect.Field leftField = node.getClass().getField("left");
                java.lang.reflect.Field rightField = node.getClass().getField("right");
                
                String op = (String) opField.get(node);
                AstNode left = (AstNode) leftField.get(node);
                AstNode right = (AstNode) rightField.get(node);
                
                String leftStr = getExpressionString(left);
                String rightStr = getExpressionString(right);
                
                // Determinar si necesita paréntesis (simplificado)
                return "(" + leftStr + " " + op + " " + rightStr + ")";
            }
        } catch (Exception e) {
            return "expresión";
        }
        
        return "";
    }

    // Nuevo: ejecutar lexer + parser y mostrar árbol o errores
    private void runParser() {
        long startTime = System.currentTimeMillis();
        
        HaskellLexer lexer = new HaskellLexer();
        String code = codeEditor.getText();
        List<Token> tokens = lexer.tokenize(code);
        Parser parser = new Parser(tokens);
        
        try {
            AstNode program = parser.parseProgram();
            String tree = program.toTreeString();
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Mensaje de éxito agregado
            String successMessage = "<span style='color: green; font-weight: bold;'>✅ ANÁLISIS SINTÁCTICO EXITOSO</span><br>" +
                                   "<span style='color: green;'>El programa ha sido analizado correctamente sin errores sintácticos.</span><br>" +
                                   "<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>" + executionTime + " ms</b></span><br><br>" +
                                   "<span style='color: blue; font-weight: bold;'>Árbol de Sintaxis Abstracta (AST):</span><br><br>";
            
            String htmlResult = "<html><body style='font-family: monospace; white-space: pre;'>" 
                              + successMessage
                              + escapeHtml(tree).replace("\n", "<br>") 
                              + "</body></html>";
            outputArea.setText(htmlResult);
        } catch (Parser.ParseException ex) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // Formatear múltiples errores, cada uno en una línea separada
            String errorMessage = ex.getMessage();
            String[] errors = errorMessage.split("\\n");
            
            StringBuilder formattedErrors = new StringBuilder();
            formattedErrors.append("<span style='color: red; font-weight: bold;'>❌ ERRORES SINTÁCTICOS ENCONTRADOS:</span><br>");
            formattedErrors.append("<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>").append(executionTime).append(" ms</b></span><br><br>");
            
            for (int i = 0; i < errors.length; i++) {
                String error = errors[i].trim();
                if (!error.isEmpty()) {
                    formattedErrors.append("<span style='color: red;'>• Error ").append(i + 1).append(":</span> ")
                                  .append(escapeHtml(error))
                                  .append("<br>");
                }
            }
            
            String htmlResult = "<html><body style='font-family: monospace; white-space: pre;'>" 
                              + formattedErrors.toString() 
                              + "</body></html>";
            outputArea.setText(htmlResult);
        } catch (Exception ex) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            String err = "<html><body style='font-family: monospace; color: red; white-space: pre;'>" +
                        "<span style='color: red; font-weight: bold;'>❌ ERROR INESPERADO</span><br>" +
                        "<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>" + executionTime + " ms</b></span><br>" +
                        "Unexpected error: " + escapeHtml(ex.toString()) + "</body></html>";
            outputArea.setText(err);
        }
    }
    
    // Método auxiliar para escapar caracteres HTML
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }
        try (FileWriter fw = new FileWriter(currentFile)) {
            fw.write(codeEditor.getText());
            String message = "<html><body style='font-family: monospace;'>Archivo guardado: " 
                           + escapeHtml(currentFile.getAbsolutePath()) + "</body></html>";
            outputArea.setText(message);
        } catch (IOException ex) {
            String errorMessage = "<html><body style='font-family: monospace; color: red;'>Error al guardar: " 
                                 + escapeHtml(ex.getMessage()) + "</body></html>";
            outputArea.setText(errorMessage);
        }
    }

    private void saveFileAs() {
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            saveFile();
        }
    }

    // Nuevo: Optimización de código
    private void runCodeOptimization() {
        long startTime = System.currentTimeMillis();
        
        try {
            String code = codeEditor.getText();
            
            if (code.trim().isEmpty()) {
                outputArea.setText("<html><body style='font-family: monospace; color: orange;'>" +
                                 "⚠️ No hay código para optimizar</body></html>");
                return;
            }
            
            // Calcular tamaño original (en bytes)
            int originalSize = code.getBytes().length;
            
            CodeOptimizer optimizer = new CodeOptimizer();
            CodeOptimizer.OptimizationResult result = optimizer.optimize(code);
            
            // Calcular tamaño optimizado (en bytes)
            int optimizedSize = result.optimizedCode.getBytes().length;
            int sizeDifference = originalSize - optimizedSize;
            double reductionPercentage = originalSize > 0 ? (sizeDifference * 100.0 / originalSize) : 0;
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            StringBuilder output = new StringBuilder();
            output.append("<html><body style='font-family: monospace;'>");
            
            if (result.success) {
                output.append("<span style='color: green; font-weight: bold; font-size: 14px;'>")
                      .append("✅ OPTIMIZACIÓN EXITOSA</span><br>");
                output.append("<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>").append(executionTime).append(" ms</b></span><br><br>");
                
                output.append("<span style='color: blue; font-weight: bold;'>📊 ESTADÍSTICAS:</span><br>");
                output.append("  • Comentarios eliminados: <b>").append(result.commentsRemoved).append("</b><br>");
                output.append("  • Espacios optimizados: <b>").append(result.spacesOptimized).append(" caracteres</b><br>");
                output.append("  • Subexpresiones comunes eliminadas: <b>").append(result.subexpressionsEliminated).append("</b><br><br>");
                
                output.append("<span style='color: blue; font-weight: bold;'>💾 TAMAÑO DEL ARCHIVO:</span><br>");
                output.append("  • Tamaño original: <b>").append(originalSize).append(" bytes</b><br>");
                output.append("  • Tamaño optimizado: <b>").append(optimizedSize).append(" bytes</b><br>");
                output.append("  • Reducción: <b>").append(sizeDifference).append(" bytes</b>");
                output.append(" (<span style='color: ").append(sizeDifference > 0 ? "green" : "orange").append(";'>")
                      .append(String.format("%.2f%%", reductionPercentage)).append("</span>)<br><br>");
                
                output.append("<span style='color: blue; font-weight: bold;'>📝 LOG DE OPTIMIZACIÓN:</span><br>");
                output.append("<div style='background-color: #f0f0f0; padding: 10px; border-left: 3px solid #4CAF50;'>");
                for (String log : result.log) {
                    output.append(escapeHtml(log)).append("<br>");
                }
                output.append("</div><br>");
                
                // Preguntar al usuario dónde guardar el archivo optimizado
                JFileChooser saveChooser = new JFileChooser();
                saveChooser.setDialogTitle("Guardar código optimizado");
                
                // Sugerir nombre de archivo
                if (currentFile != null) {
                    String baseName = currentFile.getName().replaceFirst("[.][^.]+$", "");
                    saveChooser.setSelectedFile(new File(currentFile.getParent(), baseName + "_optimizado.txt"));
                } else {
                    saveChooser.setSelectedFile(new File("codigo_optimizado.txt"));
                }
                
                int userSelection = saveChooser.showSaveDialog(this);
                
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = saveChooser.getSelectedFile();
                    
                    try (FileWriter fw = new FileWriter(fileToSave)) {
                        fw.write(result.optimizedCode);
                        output.append("<br><span style='color: green; font-weight: bold;'>")
                              .append("💾 Código optimizado guardado en:</span><br>");
                        output.append("<span style='color: #0066cc;'>")
                              .append(escapeHtml(fileToSave.getAbsolutePath()))
                              .append("</span><br><br>");
                        
                        output.append("<span style='color: gray; font-size: 11px;'>")
                              .append("Nota: El archivo optimizado está listo para su uso. ")
                              .append("Puede abrirlo y verificar los cambios realizados.")
                              .append("</span>");
                    } catch (IOException ex) {
                        output.append("<br><span style='color: red; font-weight: bold;'>")
                              .append("❌ ERROR AL GUARDAR: ")
                              .append(escapeHtml(ex.getMessage()))
                              .append("</span>");
                    }
                } else {
                    output.append("<br><span style='color: orange;'>")
                          .append("⚠️ Guardado cancelado. El código optimizado no se guardó.")
                          .append("</span>");
                }
                
            } else {
                output.append("<span style='color: red; font-weight: bold; font-size: 14px;'>")
                      .append("❌ OPTIMIZACIÓN FALLIDA</span><br>");
                output.append("<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>").append(executionTime).append(" ms</b></span><br><br>");
                output.append("<span style='color: red;'>Error: ")
                      .append(escapeHtml(result.errorMessage))
                      .append("</span><br><br>");
                
                output.append("<span style='color: blue; font-weight: bold;'>📝 LOG:</span><br>");
                for (String log : result.log) {
                    output.append(escapeHtml(log)).append("<br>");
                }
            }
            
            output.append("</body></html>");
            outputArea.setText(output.toString());
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            String errorOutput = "<html><body style='font-family: monospace;'>" +
                               "<span style='color: red; font-weight: bold;'>❌ ERROR INESPERADO</span><br>" +
                               "<span style='color: blue;'>⏱️ Tiempo de ejecución: <b>" + executionTime + " ms</b></span><br>" +
                               "<span style='color: red;'>Excepción: " + escapeHtml(e.getMessage()) + "</span><br>" +
                               "<span style='color: gray;'>Por favor, reporte este error.</span>" +
                               "</body></html>";
            outputArea.setText(errorOutput);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IDEFrame().setVisible(true));
    }
}
