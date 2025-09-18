package proyecto.lenguaje.gui;

import proyecto.lenguaje.lexer.*;
import proyecto.lenguaje.parser.*; // Nuevo import para el parser
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class IDEFrame extends JFrame {
    private JTextArea codeEditor;
    private JTextArea lineNumbers;
    private JEditorPane outputArea;
    private JButton lexButton, saveButton, saveAsButton, semanticButton;
    private JButton parseButton; // nuevo botón
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

        JPanel buttonPanel = new JPanel();
        lexButton = new JButton("Análisis Léxico");
        parseButton = new JButton("Análisis Sintáctico");
        saveButton = new JButton("Guardar Cambios");
        saveAsButton = new JButton("Guardar Como");
        semanticButton = new JButton("Validación Semántica");
        buttonPanel.add(lexButton);
        buttonPanel.add(parseButton); // añadir al panel
        buttonPanel.add(semanticButton);
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
        HaskellLexer lexer = new HaskellLexer();
        String code = codeEditor.getText();
        List<Token> tokens = lexer.tokenize(code);
        String result = SemanticValidator.validateCycles(tokens);
        
        // Agregar mensaje de éxito al inicio si no hay errores semánticos
        String successPrefix = "";
        if (!result.contains("ERROR") && !result.contains("ERRORES SEMÁNTICOS")) {
            successPrefix = "<span style='color: green; font-weight: bold;'>✅ VALIDACIÓN SEMÁNTICA EXITOSA</span><br>" +
                           "<span style='color: green;'>No se encontraron errores semánticos en el código.</span><br><br>" +
                           "<span style='color: blue; font-weight: bold;'>Resultado del análisis:</span><br><br>";
        }
        
        // Convertir texto plano a HTML básico para mantener formato
        String htmlResult = "<html><body style='font-family: monospace; white-space: pre;'>" 
                          + successPrefix
                          + escapeHtml(result).replace("\n", "<br>") 
                          + "</body></html>";
        outputArea.setText(htmlResult);
    }

    private void runLexicalAnalysis() {
        HaskellLexer lexer = new HaskellLexer();
        String code = codeEditor.getText();
        List<Token> tokens = lexer.tokenize(code);
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
    
    // Nuevo: ejecutar lexer + parser y mostrar árbol o errores
    private void runParser() {
        HaskellLexer lexer = new HaskellLexer();
        String code = codeEditor.getText();
        List<Token> tokens = lexer.tokenize(code);
        Parser parser = new Parser(tokens);
        try {
            AstNode program = parser.parseProgram();
            String tree = program.toTreeString();
            
            // Mensaje de éxito agregado
            String successMessage = "<span style='color: green; font-weight: bold;'>✅ ANÁLISIS SINTÁCTICO EXITOSO</span><br>" +
                                   "<span style='color: green;'>El programa ha sido analizado correctamente sin errores sintácticos.</span><br><br>" +
                                   "<span style='color: blue; font-weight: bold;'>Árbol de Sintaxis Abstracta (AST):</span><br><br>";
            
            String htmlResult = "<html><body style='font-family: monospace; white-space: pre;'>" 
                              + successMessage
                              + escapeHtml(tree).replace("\n", "<br>") 
                              + "</body></html>";
            outputArea.setText(htmlResult);
        } catch (Parser.ParseException ex) {
            // Formatear múltiples errores, cada uno en una línea separada
            String errorMessage = ex.getMessage();
            String[] errors = errorMessage.split("\\n");
            
            StringBuilder formattedErrors = new StringBuilder();
            formattedErrors.append("<span style='color: red; font-weight: bold;'>❌ ERRORES SINTÁCTICOS ENCONTRADOS:</span><br><br>");
            
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
            String err = "<html><body style='font-family: monospace; color: red; white-space: pre;'>Unexpected error: "
                       + escapeHtml(ex.toString()) + "</body></html>";
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IDEFrame().setVisible(true));
    }
}
