package proyecto.lenguaje.gui;

import proyecto.lenguaje.lexer.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;

public class IDEFrame extends JFrame {
    private JTextArea codeEditor;
    private JTextArea lineNumbers;
    private JTextArea outputArea;
    private JButton lexButton, saveButton, saveAsButton;
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
        outputArea = new JTextArea();
        outputArea.setEditable(false);
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
        lexButton = new JButton("Validar Léxicamente");
        saveButton = new JButton("Guardar Cambios");
        saveAsButton = new JButton("Guardar Como");
        buttonPanel.add(lexButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(saveAsButton);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Crear split pane con el editor y el panel derecho
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainScrollPane, rightPanel);
        splitPane.setDividerLocation(600);
        add(splitPane, BorderLayout.CENTER);

        fileChooser = new JFileChooser();

        lexButton.addActionListener(e -> runLexicalAnalysis());
        saveButton.addActionListener(e -> saveFile());
        saveAsButton.addActionListener(e -> saveFileAs());
        
        updateLineNumbers(); // Inicializar números de línea
    }

    private void updateLineNumbers() {
        String text = codeEditor.getText();
        int lines = text.split("\n", -1).length;
        if (text.isEmpty()) lines = 1;
        
        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            numbers.append(i).append("\n");
        }
        lineNumbers.setText(numbers.toString());
        
        // Actualizar el tamaño preferido de los números de línea
        int width = String.valueOf(lines).length();
        int charWidth = lineNumbers.getFontMetrics(lineNumbers.getFont()).charWidth('0');
        lineNumbers.setPreferredSize(new Dimension((width + 2) * charWidth + 10, lineNumbers.getPreferredSize().height));
        lineNumbers.revalidate();
    }
    

    private void runLexicalAnalysis() {
        HaskellLexer lexer = new HaskellLexer();
        String code = codeEditor.getText();
        List<Token> tokens = lexer.tokenize(code);
        StringBuilder sb = new StringBuilder();
        
        int errorCount = 0;
        for (Token t : tokens) {
            sb.append(t.toString()).append("\n");
            if (t.getType() == Token.Type.ERROR) {
                errorCount++;
            }
        }
        
        sb.append("\n--- RESUMEN ---\n");
        sb.append("Total de tokens: ").append(tokens.size()).append("\n");
        sb.append("Errores léxicos: ").append(errorCount).append("\n");
        
        if (errorCount > 0) {
            sb.append("\n--- ERRORES ENCONTRADOS ---\n");
            for (Token t : tokens) {
                if (t.getType() == Token.Type.ERROR) {
                    sb.append("ERROR: Carácter inválido '").append(t.getValue())
                      .append("' en línea ").append(t.getLine())
                      .append(", posición ").append(t.getPosition()).append("\n");
                }
            }
        }
        
        outputArea.setText(sb.toString());
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }
        try (FileWriter fw = new FileWriter(currentFile)) {
            fw.write(codeEditor.getText());
            outputArea.setText("Archivo guardado: " + currentFile.getAbsolutePath());
        } catch (IOException ex) {
            outputArea.setText("Error al guardar: " + ex.getMessage());
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
