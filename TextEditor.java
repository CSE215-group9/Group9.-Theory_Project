import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;

public class TextEditor extends JFrame implements ActionListener {

    JTextPane textPane;
    JScrollPane scrollPane;
    JLabel fontLabel;
    JSpinner fontSizeSpinner;
    JButton fontColorButton;
    JComboBox<String> fontBox;

    JMenuBar menuBar;
    JMenu fileMenu;
    JMenuItem openItem;
    JMenuItem saveItem;
    JMenuItem exitItem;

    SyntaxHighlighter highlighter;
    Thread fileHandlerThread;

    TextEditor() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Text Editor with Syntax Highlighting");
        this.setSize(600, 600);
        this.setLayout(new FlowLayout());
        this.setLocationRelativeTo(null);

        textPane = new JTextPane();
        textPane.setFont(new Font("Arial", Font.PLAIN, 20));
        scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(550, 500));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        highlighter = new SyntaxHighlighter(textPane);
        textPane.getDocument().addDocumentListener(highlighter);

        fontLabel = new JLabel("Font: ");

        fontSizeSpinner = new JSpinner();
        fontSizeSpinner.setPreferredSize(new Dimension(50, 25));
        fontSizeSpinner.setValue(20);
        fontSizeSpinner.addChangeListener(e -> 
            textPane.setFont(new Font(textPane.getFont().getFamily(), Font.PLAIN, (int) fontSizeSpinner.getValue()))
        );

        fontColorButton = new JButton("Color");
        fontColorButton.addActionListener(this);

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontBox = new JComboBox<>(fonts);
        fontBox.addActionListener(this);
        fontBox.setSelectedItem("Arial");

        // ----- Menubar -----
        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        openItem = new JMenuItem("Open");
        saveItem = new JMenuItem("Save");
        exitItem = new JMenuItem("Exit");

        openItem.addActionListener(this);
        saveItem.addActionListener(this);
        exitItem.addActionListener(this);

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // ----- /Menubar -----
        this.setJMenuBar(menuBar);
        this.add(fontLabel);
        this.add(fontSizeSpinner);
        this.add(fontColorButton);
        this.add(fontBox);
        this.add(scrollPane);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == fontColorButton) {
            Color color = JColorChooser.showDialog(null, "Choose a color", Color.black);
            textPane.setForeground(color);
        }

        if (e.getSource() == fontBox) {
            textPane.setFont(new Font((String) fontBox.getSelectedItem(), Font.PLAIN, textPane.getFont().getSize()));
        }

        if (e.getSource() == openItem) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "java", "py", "html"));

            int response = fileChooser.showOpenDialog(null);
            if (response == JFileChooser.APPROVE_OPTION) {
                File file = new File(fileChooser.getSelectedFile().getAbsolutePath());
                fileHandlerThread = new Thread(() -> loadFile(file));
                fileHandlerThread.start();
            }
        }

        if (e.getSource() == saveItem) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));

            int response = fileChooser.showSaveDialog(null);
            if (response == JFileChooser.APPROVE_OPTION) {
                File file = new File(fileChooser.getSelectedFile().getAbsolutePath());
                fileHandlerThread = new Thread(() -> saveFile(file));
                fileHandlerThread.start();
            }
        }

        if (e.getSource() == exitItem) {
            System.exit(0);
        }
    }

    private void loadFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            textPane.setText("");
            String line;
            while ((line = reader.readLine()) != null) {
                textPane.getDocument().insertString(textPane.getDocument().getLength(), line + "\n", null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveFile(File file) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.write(textPane.getText());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new TextEditor();
    }
}

class SyntaxHighlighter implements DocumentListener {
    private final JTextPane textPane;
    private final StyledDocument doc;
    private final SimpleAttributeSet keywordAttr;
    private final SimpleAttributeSet stringAttr;
    private final SimpleAttributeSet commentAttr;

    private static final Pattern PATTERN_KEYWORDS = Pattern.compile("\\b(int|double|float|String|public|private|class|if|else|abstruct)\\b");
    private static final Pattern PATTERN_STRINGS = Pattern.compile("\"(.*?)\"");
    private static final Pattern PATTERN_COMMENTS = Pattern.compile("//[^\n]*");

    public SyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        this.doc = textPane.getStyledDocument();

        keywordAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(keywordAttr, Color.BLUE);
        StyleConstants.setBold(keywordAttr, true);

        stringAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(stringAttr, Color.ORANGE);

        commentAttr = new SimpleAttributeSet();
        StyleConstants.setForeground(commentAttr, Color.GRAY);
        StyleConstants.setItalic(commentAttr, true);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        applySyntaxHighlighting();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        applySyntaxHighlighting();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        applySyntaxHighlighting();
    }

    private void applySyntaxHighlighting() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Remove the listener temporarily to avoid recursive events
                textPane.getDocument().removeDocumentListener(this);

                String text = doc.getText(0, doc.getLength());
                
                // Clear all existing styles
                doc.setCharacterAttributes(0, text.length(), new SimpleAttributeSet(), true);

                // Apply syntax highlighting for keywords
                Matcher matcher = PATTERN_KEYWORDS.matcher(text);
                while (matcher.find()) {
                    doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), keywordAttr, false);
                }

                // Apply syntax highlighting for strings
                matcher = PATTERN_STRINGS.matcher(text);
                while (matcher.find()) {
                    doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), stringAttr, false);
                }

                // Apply syntax highlighting for comments
                matcher = PATTERN_COMMENTS.matcher(text);
                while (matcher.find()) {
                    doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), commentAttr, false);
                }

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            } finally {
                // Re-add the listener after updating the styles
                textPane.getDocument().addDocumentListener(this);
            }
        });
    }

}
