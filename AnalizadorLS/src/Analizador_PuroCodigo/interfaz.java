package Analizador_PuroCodigo;

import Analizador_PuroCodigo.analizadorlexicorobot;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;

public class interfaz extends JFrame {
    private JTextPane codigo;
    private JTable tablaLexico;
    private JButton btnAnalizar, btnLimpiar;
    private JTextPane sintactico;
    private DefaultTableModel modeloTabla;

    public interfaz() {
        setTitle("ANALIZADOR LÉXICO Y SINTÁCTICO SIN COMPONENTES");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1700, 800);
        setLocationRelativeTo(null);
        setLayout(null);

        // Título
        JLabel titulo = new JLabel("ANALIZADOR LÉXICO Y SINTÁCTICO", SwingConstants.CENTER);
        titulo.setFont(new Font("Serif", Font.PLAIN, 30));
        titulo.setBounds(400, 10, 600, 40);
        add(titulo);

        // Área de código
        codigo = new JTextPane();
        codigo.setFont(new Font("Monospaced", Font.PLAIN, 30));
        JScrollPane scrollCodigo = new JScrollPane(codigo);
        scrollCodigo.setBounds(25, 80, 700, 650);
        add(scrollCodigo);
        scrollCodigo.setRowHeaderView(new LineNumberPanel(codigo));

        // Botones
        btnAnalizar = new JButton("Analizar");
        btnAnalizar.setFont(new Font("Arial", Font.BOLD, 20));
        btnAnalizar.setBounds(750, 190, 110, 40);
        add(btnAnalizar);

        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setFont(new Font("Arial", Font.BOLD, 20));
        btnLimpiar.setBounds(750, 240, 110, 40);
        add(btnLimpiar);

        // Tabla léxica
        modeloTabla = new DefaultTableModel(new Object[]{"Lexema", "Tipo"}, 0);
        tablaLexico = new JTable(modeloTabla);
        tablaLexico.setFont(new Font("Monospaced", Font.PLAIN, 22));
        tablaLexico.setRowHeight(26);
        JScrollPane scrollLexico = new JScrollPane(tablaLexico);
        scrollLexico.setBounds(900, 550, 450, 200);
        add(scrollLexico);
        
        // Configurar header de tabla
        JTableHeader header = tablaLexico.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 20));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);

        // Área de resultados sintácticos
        sintactico = new JTextPane();
        sintactico.setFont(new Font("Monospaced", Font.PLAIN, 20));
        JScrollPane scrollSintactico = new JScrollPane(sintactico);
        scrollSintactico.setBounds(900, 80, 450, 450);
        sintactico.setForeground(Color.BLACK);
        sintactico.setEditable(false);
        add(scrollSintactico);

        // Eventos
        btnAnalizar.addActionListener(e -> analizarCodigo());
        btnLimpiar.addActionListener(e -> limpiarTodo());
    }

private void analizarCodigo() {
        modeloTabla.setRowCount(0);
        sintactico.setText("");
        String texto = codigo.getText();
        
        java.util.List<analizadorlexicorobot.Token> tokens = analizadorlexicorobot.analizarLexico(texto);
        
        // Mostrar tokens en tabla
        for (analizadorlexicorobot.Token token : tokens) {
            modeloTabla.addRow(new Object[]{
                token.lexema,
                token.tipo.toString(),
                token.linea
            });
        }
        
        // Analizar sintácticamente y obtener errores
        java.util.List<String> errores = analizadorlexicorobot.analizarSintactico(tokens);
        
        // Procesar errores para resaltar
        if (!errores.isEmpty()) {
            sintactico.setText(String.join("\n", errores));
            sintactico.setForeground(Color.RED);
            
            // Resaltar líneas con errores
            for (String error : errores) {
                // Extraer número de línea del mensaje de error
                Pattern pattern = Pattern.compile("Línea (\\d+)");
                Matcher matcher = pattern.matcher(error);
                if (matcher.find()) {
                    int lineaError = Integer.parseInt(matcher.group(1));
                    resaltarLineaError(lineaError);
                }
            }
        } else {
            sintactico.setText("Análisis completado sin errores.");
            sintactico.setForeground(Color.BLACK);
        }
    }


    private void limpiarTodo() {
        modeloTabla.setRowCount(0);
        sintactico.setText("");
        codigo.setText("");
    }

    class LineNumberPanel extends JComponent {
        private final JTextPane textPane;
        private final FontMetrics fm;
        private final int lineHeight;
        private final int baseLine;

        public LineNumberPanel(JTextPane textPane) {
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
     private void limpiarResaltado() {
    codigo.getHighlighter().removeAllHighlights();
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            interfaz ven = new interfaz();
            ven.setVisible(true);
        });
    }
}