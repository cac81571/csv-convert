package com.csvconvert.gui;

import com.csvconvert.ConsoleEncoding;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {
    public static void main(String[] args) {
        ConsoleEncoding.install();
        FlatLightLaf.setup();
        System.setProperty("flatlaf.useWindowDecorations", "true");
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        SwingUtilities.invokeLater(() -> {
            BatchBuilderFrame frame = new BatchBuilderFrame();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private App() {
    }
}
