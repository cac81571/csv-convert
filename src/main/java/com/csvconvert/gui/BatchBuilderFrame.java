package com.csvconvert.gui;

import com.csvconvert.BatchScriptBuilder;
import com.csvconvert.LaunchConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class BatchBuilderFrame extends JFrame {
    private final JTextField jarField = new JTextField();
    private final JTextField columnsField = new JTextField();
    private final JTextField headerRowsField = new JTextField("1");
    private final JComboBox<String> encodingCombo = editableEncodingCombo();
    private final JCheckBox splitEncodingCheck = new JCheckBox("入出力で文字コードを分ける");
    private final JComboBox<String> inputEncodingCombo = editableEncodingCombo();
    private final JComboBox<String> outputEncodingCombo = editableEncodingCombo();
    private final JComboBox<String> delimiterCombo = new JComboBox<>(new String[]{
            "COMMA (,)", "TAB", "SEMI (;)", "PIPE (|)", "SPACE", "カスタム..."
    });
    private final JTextField customDelimiterField = new JTextField(4);
    private final JComboBox<String> quoteCombo = new JComboBox<>(new String[]{
            "DOUBLE (\")", "SINGLE (')", "NONE", "カスタム..."
    });
    private final JTextField customQuoteField = new JTextField(4);
    private final JCheckBox reverseCheck = new JCheckBox("データ行を逆順にする", false);
    private final JCheckBox pauseCheck = new JCheckBox("終了時に pause する", true);
    private final JTextArea previewArea = new JTextArea();
    private final BatchScriptBuilder builder = new BatchScriptBuilder();
    private Path lastDirectory = Path.of(".").toAbsolutePath().normalize();
    private Path absoluteJarPath;
    private boolean updatingJarField;

    public BatchBuilderFrame() {
        super("csv-convert 起動バッチ生成");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 620));
        setPreferredSize(new Dimension(840, 680));

        Path defaultJar = Path.of("target", "csv-convert-1.0.0.jar").toAbsolutePath().normalize();
        if (Files.isRegularFile(defaultJar)) {
            absoluteJarPath = defaultJar;
            jarField.setText(defaultJar.getFileName().toString());
            lastDirectory = defaultJar.getParent();
        } else {
            jarField.setText("csv-convert-1.0.0.jar");
        }

        customDelimiterField.setEnabled(false);
        customQuoteField.setEnabled(false);
        inputEncodingCombo.setEnabled(false);
        outputEncodingCombo.setEnabled(false);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.add(buildForm(), BorderLayout.NORTH);
        root.add(buildPreview(), BorderLayout.CENTER);
        root.add(buildActions(), BorderLayout.SOUTH);
        setContentPane(root);

        wireEvents();
        refreshPreview();
        pack();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        row = addPathRow(form, c, row, "JAR（bat基準の相対パス）", jarField, this::browseJar);

        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        form.add(label("入力ファイル"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        JLabel inputHint = new JLabel("バッチ引数 %1（CSVを bat にドラッグ＆ドロップ）");
        inputHint.setToolTipText("生成した bat にファイルをドロップするか、第1引数で渡します。");
        form.add(inputHint, c);
        c.gridwidth = 1;
        row++;

        c.gridx = 0;
        c.gridy = row;
        form.add(label("出力ファイル"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        form.add(new JLabel("<入力名>_conv.<拡張子>  （例: data.csv → data_conv.csv）"), c);
        c.gridwidth = 1;
        row++;

        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        form.add(label("抽出カラム"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        columnsField.setToolTipText("例: name,city または 2,4。空欄で全カラム。");
        form.add(columnsField, c);
        c.gridwidth = 1;
        row++;

        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        form.add(label("ヘッダ行数"), c);
        c.gridx = 1;
        c.weightx = 0;
        headerRowsField.setHorizontalAlignment(SwingConstants.LEFT);
        headerRowsField.setColumns(6);
        headerRowsField.setToolTipText("0以上の整数。0でヘッダなし。");
        form.add(headerRowsField, c);
        row++;

        c.gridx = 0;
        c.gridy = row;
        form.add(label("文字コード"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        form.add(encodingCombo, c);
        c.gridwidth = 1;
        row++;

        c.gridx = 1;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(splitEncodingCheck, c);
        c.gridwidth = 1;
        row++;

        c.gridx = 0;
        c.gridy = row;
        form.add(label("入力文字コード"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        form.add(inputEncodingCombo, c);
        c.gridwidth = 1;
        row++;

        c.gridx = 0;
        c.gridy = row;
        form.add(label("出力文字コード"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        form.add(outputEncodingCombo, c);
        c.gridwidth = 1;
        row++;

        c.gridx = 0;
        c.gridy = row;
        form.add(label("区切り文字"), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(delimiterCombo, c);
        c.gridx = 2;
        c.weightx = 0;
        customDelimiterField.setToolTipText("1文字、または TAB / COMMA など");
        form.add(customDelimiterField, c);
        row++;

        c.gridx = 0;
        c.gridy = row;
        form.add(label("囲み文字"), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(quoteCombo, c);
        c.gridx = 2;
        c.weightx = 0;
        customQuoteField.setToolTipText("1文字、または DOUBLE / SINGLE / NONE");
        form.add(customQuoteField, c);
        row++;

        c.gridx = 1;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(reverseCheck, c);
        row++;

        c.gridx = 1;
        c.gridy = row;
        c.gridwidth = 2;
        form.add(pauseCheck, c);

        return form;
    }

    private JPanel buildPreview() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(label("生成プレビュー (.bat)"), BorderLayout.NORTH);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        previewArea.setRows(14);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(previewArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton refreshButton = new JButton("プレビュー更新");
        refreshButton.addActionListener(e -> refreshPreview());
        JButton saveButton = new JButton("バッチファイルを保存...");
        saveButton.addActionListener(e -> saveBatchFile());
        panel.add(refreshButton);
        panel.add(saveButton);
        return panel;
    }

    private void wireEvents() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!updatingJarField) {
                    refreshPreview();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!updatingJarField) {
                    refreshPreview();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!updatingJarField) {
                    refreshPreview();
                }
            }
        };
        jarField.getDocument().addDocumentListener(listener);
        columnsField.getDocument().addDocumentListener(listener);
        headerRowsField.getDocument().addDocumentListener(listener);
        customDelimiterField.getDocument().addDocumentListener(listener);
        customQuoteField.getDocument().addDocumentListener(listener);

        encodingCombo.addActionListener(e -> refreshPreview());
        inputEncodingCombo.addActionListener(e -> refreshPreview());
        outputEncodingCombo.addActionListener(e -> refreshPreview());
        reverseCheck.addActionListener(e -> refreshPreview());
        pauseCheck.addActionListener(e -> refreshPreview());

        splitEncodingCheck.addActionListener(e -> {
            boolean split = splitEncodingCheck.isSelected();
            encodingCombo.setEnabled(!split);
            inputEncodingCombo.setEnabled(split);
            outputEncodingCombo.setEnabled(split);
            if (split) {
                if (Objects.toString(inputEncodingCombo.getSelectedItem(), "").isBlank()) {
                    inputEncodingCombo.setSelectedItem(encodingCombo.getSelectedItem());
                }
                if (Objects.toString(outputEncodingCombo.getSelectedItem(), "").isBlank()) {
                    outputEncodingCombo.setSelectedItem(encodingCombo.getSelectedItem());
                }
            }
            refreshPreview();
        });

        delimiterCombo.addActionListener(e -> {
            boolean custom = delimiterCombo.getSelectedIndex() == delimiterCombo.getItemCount() - 1;
            customDelimiterField.setEnabled(custom);
            refreshPreview();
        });
        quoteCombo.addActionListener(e -> {
            boolean custom = quoteCombo.getSelectedIndex() == quoteCombo.getItemCount() - 1;
            customQuoteField.setEnabled(custom);
            refreshPreview();
        });
    }

    private void refreshPreview() {
        try {
            normalizeJarFieldDisplay();
            LaunchConfig config = currentConfig(BatchScriptBuilder.toBatRelativeJar(jarField.getText()));
            previewArea.setText(builder.build(config));
            previewArea.setCaretPosition(0);
        } catch (RuntimeException ex) {
            previewArea.setText("設定エラー: " + ex.getMessage());
        }
    }

    /** Keep the text field bat-relative; remember absolute path only internally. */
    private void normalizeJarFieldDisplay() {
        String typed = jarField.getText().strip();
        if (typed.isEmpty()) {
            return;
        }
        Path typedPath = Path.of(typed);
        if (!BatchScriptBuilder.isAbsolutePath(typed, typedPath)) {
            String windows = typed.replace('/', '\\');
            if (!windows.equals(jarField.getText())) {
                updatingJarField = true;
                try {
                    jarField.setText(windows);
                } finally {
                    updatingJarField = false;
                }
            }
            return;
        }
        absoluteJarPath = typedPath.toAbsolutePath().normalize();
        String relative = absoluteJarPath.getFileName().toString();
        if (!relative.equals(jarField.getText())) {
            updatingJarField = true;
            try {
                jarField.setText(relative);
            } finally {
                updatingJarField = false;
            }
        }
    }

    private void saveBatchFile() {
        JFileChooser chooser = new JFileChooser(lastDirectory.toFile());
        chooser.setDialogTitle("バッチファイルの保存先");
        chooser.setSelectedFile(Path.of("run-csv-convert.bat").toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("バッチファイル (*.bat)", "bat"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase().endsWith(".bat")) {
            target = target.resolveSibling(target.getFileName() + ".bat");
        }
        lastDirectory = target.getParent() != null ? target.getParent() : lastDirectory;

        String relativeJar = resolveJarRelativeToBat(target);
        jarField.setText(relativeJar);

        LaunchConfig config;
        try {
            config = currentConfig(relativeJar);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "入力エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // MS932 + CRLF: rem comments may include Japanese; UTF-8 bats break under cmd.exe.
            Files.writeString(target, builder.build(config), Charset.forName("MS932"));
            JOptionPane.showMessageDialog(
                    this,
                    "保存しました:\n" + target
                            + "\n\n使い方: CSVファイルをこの bat にドラッグ＆ドロップしてください。"
                            + "\n出力: <入力名>_conv.<拡張子>"
                            + "\nJAR: " + relativeJar + " （bat と同じ基準の相対パス）",
                    "完了",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存に失敗しました:\n" + ex.getMessage(), "エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String resolveJarRelativeToBat(Path batFile) {
        normalizeJarFieldDisplay();
        if (absoluteJarPath != null && Files.isRegularFile(absoluteJarPath)) {
            return BatchScriptBuilder.jarPathRelativeToBat(absoluteJarPath, batFile);
        }
        String typed = jarField.getText().strip();
        if (typed.isEmpty()) {
            return "csv-convert-1.0.0.jar";
        }
        Path typedPath = Path.of(typed);
        if (BatchScriptBuilder.isAbsolutePath(typed, typedPath) && Files.isRegularFile(typedPath)) {
            absoluteJarPath = typedPath.toAbsolutePath().normalize();
            return BatchScriptBuilder.jarPathRelativeToBat(absoluteJarPath, batFile);
        }
        return BatchScriptBuilder.toBatRelativeJar(typed);
    }

    private LaunchConfig currentConfig(String jarRelativePath) {
        String encoding = textOf(encodingCombo);
        String inputEncoding = "";
        String outputEncoding = "";
        if (splitEncodingCheck.isSelected()) {
            inputEncoding = textOf(inputEncodingCombo);
            outputEncoding = textOf(outputEncodingCombo);
        }
        return new LaunchConfig(
                jarRelativePath,
                columnsField.getText().strip(),
                parseHeaderRows(),
                encoding,
                inputEncoding,
                outputEncoding,
                selectedDelimiter(),
                selectedQuote(),
                reverseCheck.isSelected(),
                pauseCheck.isSelected()
        );
    }

    private int parseHeaderRows() {
        String text = headerRowsField.getText().strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("ヘッダ行数を入力してください。");
        }
        try {
            int value = Integer.parseInt(text);
            if (value < 0) {
                throw new IllegalArgumentException("ヘッダ行数は0以上で指定してください: " + value);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("ヘッダ行数は整数で入力してください: " + text);
        }
    }

    private String selectedDelimiter() {
        return switch (delimiterCombo.getSelectedIndex()) {
            case 1 -> "TAB";
            case 2 -> "SEMI";
            case 3 -> "PIPE";
            case 4 -> "SPACE";
            case 5 -> customDelimiterField.getText().strip();
            default -> "COMMA";
        };
    }

    private String selectedQuote() {
        return switch (quoteCombo.getSelectedIndex()) {
            case 1 -> "SINGLE";
            case 2 -> "NONE";
            case 3 -> customQuoteField.getText().strip();
            default -> "DOUBLE";
        };
    }

    private void browseJar() {
        JFileChooser chooser = new JFileChooser(lastDirectory.toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("JAR (*.jar)", "jar"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            absoluteJarPath = selected;
            updatingJarField = true;
            try {
                jarField.setText(selected.getFileName().toString());
            } finally {
                updatingJarField = false;
            }
            lastDirectory = selected.getParent() != null ? selected.getParent() : lastDirectory;
            refreshPreview();
        }
    }

    private static int addPathRow(JPanel form, GridBagConstraints c, int row, String title, JTextField field, Runnable browse) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        form.add(label(title), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
        c.gridx = 2;
        c.weightx = 0;
        JButton button = new JButton("参照...");
        button.addActionListener(e -> browse.run());
        form.add(button, c);
        return row + 1;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private static JComboBox<String> editableEncodingCombo() {
        JComboBox<String> combo = new JComboBox<>(new String[]{
                "UTF-8", "MS932", "Shift_JIS", "Windows-31J", "EUC-JP", "ISO-8859-1", "UTF-16"
        });
        combo.setEditable(true);
        combo.setSelectedItem("UTF-8");
        return combo;
    }

    private static String textOf(JComboBox<String> combo) {
        Object value = combo.getSelectedItem();
        return value == null ? "" : value.toString().strip();
    }
}
