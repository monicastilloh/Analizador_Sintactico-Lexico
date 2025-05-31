// Paquete e imports
package interfaz_componentes;

import analizador_componentes.Parser;
import analizador_componentes.Sym;
import analizador_componentes.Lexer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.StringReader;
import java_cup.runtime.Symbol;
import java.util.HashSet;

public class AnalizadorVentana1 extends JFrame {
    private HashSet<String> variablesDeclaradas = new HashSet<>();
    
    private JTextPane codigo;
    private JTable tablaLexico;
    private JTable tablaSintactico;
    private JButton btnAnalizar, btnLimpiar;
    public static JTextPane sintactico = new JTextPane();
    

    public AnalizadorVentana1() {
        setTitle("ANALIZADOR LÉXICO Y SINTÁCTICO");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1700, 800);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel titulo = new JLabel("ANALIZADOR LÉXICO Y SINTÁCTICO", SwingConstants.CENTER);
        titulo.setFont(new Font("Serif", Font.PLAIN, 30));
        titulo.setBounds(400, 10, 600, 40);
        add(titulo);

        codigo = new JTextPane();
        codigo.setFont(new Font("Monospaced", Font.PLAIN, 30));
        JScrollPane scrollCodigo = new JScrollPane(codigo);
        scrollCodigo.setBounds(25, 80, 700, 650);
        add(scrollCodigo);

        NumeroDeLinea numeroDeLinea = new NumeroDeLinea(codigo);
        scrollCodigo.setRowHeaderView(numeroDeLinea);

        btnAnalizar = new JButton("Analizar");
        btnAnalizar.setFont(new Font("Arial", Font.BOLD, 20));
        btnAnalizar.setBounds(750, 190, 110, 40);
        add(btnAnalizar);

        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setFont(new Font("Arial", Font.BOLD, 20));
        btnLimpiar.setBounds(750, 240, 110, 40);
        add(btnLimpiar);

        DefaultTableModel modeloLexico = new DefaultTableModel(new Object[]{"Lexema", "Token"}, 0);
        tablaLexico = new JTable(modeloLexico);
        tablaLexico.setFont(new Font("Monospaced", Font.PLAIN, 22));
        tablaLexico.setRowHeight(26);
        JScrollPane scrollLexico = new JScrollPane(tablaLexico);
        scrollLexico.setBounds(900, 590, 450, 200);
        add(scrollLexico);
        tablaLexico.getColumnModel().getColumn(0).setPreferredWidth(100);
        tablaLexico.getColumnModel().getColumn(1).setPreferredWidth(240);
        tablaLexico.setEnabled(false);
        JTableHeader header = tablaLexico.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 20));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);

        sintactico.setFont(new Font("Monospaced", Font.PLAIN, 20));
        JScrollPane scrollSintactico = new JScrollPane(sintactico);
        scrollSintactico.setBounds(900, 80, 450, 500);
        sintactico.setForeground(Color.BLACK);
        sintactico.setEditable(false);
        add(scrollSintactico);

        btnLimpiar.addActionListener(e -> {
            codigo.setText("");
            modeloLexico.setRowCount(0);
            sintactico.setText("");
            limpiarResaltado();
            numeroDeLinea.repaint();
        });

    btnAnalizar.addActionListener(e -> {
       // Limpiar todo completamente
    
    limpiarResaltado();  
    variablesDeclaradas.clear();
    modeloLexico.setRowCount(0);
    sintactico.setText("");
    
    String entrada = codigo.getText();
    StringBuilder errores = new StringBuilder();
    StringBuilder advertencias = new StringBuilder();

    try {
        // 2. Análisis léxico
        Lexer lexer = new Lexer(new StringReader(entrada));
        Symbol token;
        while ((token = lexer.next_token()).sym != Sym.EOF) {
            // Registrar en tabla léxica
            modeloLexico.addRow(new Object[]{
                token.value,
                Sym.terminalNames[token.sym]
            });
            
            // Registrar variables declaradas (ROBOT + IDENTIFICADOR)
            if (token.sym == Sym.ROBOT) {
                Symbol nextToken = lexer.next_token();
                if (nextToken.sym == Sym.IDENTIFICADOR) {
                    variablesDeclaradas.add(nextToken.value.toString());
                    modeloLexico.addRow(new Object[]{
                        nextToken.value,
                        Sym.terminalNames[nextToken.sym]
                    });
                }
            }
        }

        // 3. Verificar IDENTIFICADORES no declarados
        for (int i = 0; i < modeloLexico.getRowCount(); i++) {
            String tokenType = (String) modeloLexico.getValueAt(i, 1);
            if (tokenType.equals("IDENTIFICADOR")) {
                String identificador = (String) modeloLexico.getValueAt(i, 0);
                if (!variablesDeclaradas.contains(identificador)) {
                    advertencias.append("Variable no declarada: '")
                              .append(identificador)
                              .append("'\n");
                    resaltarEnCodigo(identificador,Color.RED);
                }
            }
        }

        // 4. Análisis sintáctico
        lexer = new Lexer(new StringReader(entrada));
        Parser parser = new Parser(lexer) {
            @Override
            public void report_error(String message, Object info) {
                if (info instanceof Symbol) {
                    Symbol sym = (Symbol) info;
                    int linea = sym.left;
                    errores.append("Error línea ").append(linea).append(": ").append(message).append("\n");
                    resaltarLineaError(sym.left);
                }
            }
        };
        
        parser.parse();

        // Mostrar resultados
        if (errores.length() > 0 || advertencias.length() > 0) {
            sintactico.setText(errores.toString() + advertencias.toString());
        } else {
            sintactico.setText("Análisis sintáctico correcto.");
        }

    } catch (Exception ex) {
        sintactico.setText("Error durante el análisis: " + ex.getMessage());
        ex.printStackTrace();
    }
});


        codigo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                numeroDeLinea.repaint();
            }
        });

        setVisible(true);
    }
    // Método para registrar errores de forma consistente


// Método para sugerencias contextuales
private String obtenerSugerencia(String mensajeError) {
    if (mensajeError.contains("PUNTO_COMA")) {
        return "Falta un ';' al final de la línea";
    } else if (mensajeError.contains("PARENTESIS_CIERRA")) {
        return "Falta cerrar paréntesis ')'";
    } else if (mensajeError.contains("Token inesperado")) {
        return "Verifique la ortografía o si falta declaración";
    }
    return "Revise la sintaxis del comando";
}

    private void sugerirCorreccion(String error) {
        // Ejemplo simple de autocompletado
        if (error.contains("Token inesperado")) {
            sintactico.setText(sintactico.getText() + "\n🔍 Sugerencia: Revisa si olvidaste un punto y coma o escribiste mal una palabra clave.");
        }
    }

   private void resaltarLineaError(int lineaParser) {
    try {
        limpiarResaltado();  // <-- Limpiar antes de resaltar nueva línea
        
        // Convertir de línea parser (1-based) a documento (0-based)
        int linea = Math.max(0, lineaParser - 1);
        
        Element root = codigo.getDocument().getDefaultRootElement();
        linea = Math.min(linea, root.getElementCount() - 1);
        
        int start = root.getElement(linea).getStartOffset();
        int end = root.getElement(linea).getEndOffset();
        
        // Resaltar con color temporal
        codigo.getHighlighter().addHighlight(
            start, 
            end, 
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 150, 150))
        );
    } catch (Exception e) {
        System.err.println("Error resaltando línea: " + e.getMessage());
    }
}
    
    private void resaltarVariableEnCodigo(String variable) {
    try {
        String texto = codigo.getText();
        Highlighter hilite = codigo.getHighlighter();
        Highlighter.HighlightPainter painter = 
            new DefaultHighlighter.DefaultHighlightPainter(Color.PINK);
        
        int pos = 0;
        while ((pos = texto.indexOf(variable, pos)) >= 0) {
            hilite.addHighlight(pos, pos + variable.length(), painter);
            pos += variable.length();
        }
    } catch (Exception ex) {
        System.err.println("Error al resaltar: " + ex.getMessage());
    }
}

    private void resaltarEnCodigo(String texto, Color color) {
    try {
        Highlighter hilite = codigo.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(color);
        String contenido = codigo.getText();
        int pos = 0;
        
        // Buscar todas las ocurrencias del texto
        while ((pos = contenido.indexOf(texto, pos)) >= 0) {
            // Verificar que sea una palabra completa (no parte de otra palabra)
            if ((pos == 0 || !Character.isLetterOrDigit(contenido.charAt(pos-1))) &&
                (pos + texto.length() == contenido.length() || 
                 !Character.isLetterOrDigit(contenido.charAt(pos + texto.length())))) {
                
                hilite.addHighlight(pos, pos + texto.length(), painter);
            }
            pos += texto.length();
        }
    } catch (Exception ex) {
        System.err.println("Error al resaltar '" + texto + "': " + ex.getMessage());
    }
}

private void limpiarResaltado() {
    codigo.getHighlighter().removeAllHighlights();
}

class NumeroDeLinea extends JComponent {
    private final JTextPane textPane;
    private final FontMetrics fm;
    private final int lineHeight;
    private final int baseLine;

    public NumeroDeLinea(JTextPane textPane) {
        this.textPane = textPane;
        this.fm = textPane.getFontMetrics(textPane.getFont());
        this.lineHeight = fm.getHeight();
        this.baseLine = fm.getAscent() + fm.getLeading();
        
        setPreferredSize(new Dimension(50, Integer.MAX_VALUE));
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Rectangle clip = g.getClipBounds();
        Element root = textPane.getDocument().getDefaultRootElement();
        
        int startLine = clip.y / lineHeight;
        int endLine = Math.min(startLine + (clip.height / lineHeight) + 1, 
                       root.getElementCount());
        
        g.setColor(getForeground());
        for (int i = startLine; i < endLine; i++) {
            int y = (i * lineHeight) + baseLine;
            g.drawString(String.valueOf(i + 1), 10, y);
        }
    }
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AnalizadorVentana1::new);
    }
}
