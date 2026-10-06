package com.guycode.tendenciaspos.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.math.BigDecimal;
import java.util.Objects;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Campo de dinero en pesos: acepta solo dígitos y coma decimal, agrupa miles al escribir y entrega
 * {@link BigDecimal} con escala 2. Dispara la propiedad {@value #VALUE_PROPERTY} al cambiar.
 */
public final class MoneyField extends JTextField {
    private static final long serialVersionUID = 1L;

    public static final String VALUE_PROPERTY = "value";

    private transient BigDecimal value;

    public MoneyField() {
        super(12);
        setHorizontalAlignment(TRAILING);
        putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, new JLabel("$"));
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0");
        ((AbstractDocument) getDocument()).setDocumentFilter(new Filter());
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshValue();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshValue();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshValue();
            }
        });
    }

    /** Valor actual o {@code null} si el campo está vacío. */
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal newValue) {
        setText(MoneyFormat.format(newValue));
    }

    private void refreshValue() {
        var old = value;
        value = MoneyFormat.parse(getText());
        if (!Objects.equals(old, value)) {
            firePropertyChange(VALUE_PROPERTY, old, value);
        }
    }

    private final class Filter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            var current = fb.getDocument().getText(0, fb.getDocument().getLength());
            // Borrar un separador de miles borra el dígito anterior (retroceso natural).
            if (length == 1 && current.charAt(offset) == '.' && offset > 0) {
                offset--;
            }
            replace(fb, offset, length, "", null);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            var current = fb.getDocument().getText(0, fb.getDocument().getLength());
            var insert = text == null ? "" : text;
            var raw = current.substring(0, offset) + insert + current.substring(offset + length);
            var edit = MoneyFormat.normalize(raw, offset + insert.length());
            fb.replace(0, current.length(), edit.text(), attrs);
            SwingUtilities.invokeLater(
                    () -> setCaretPosition(Math.min(edit.caret(), getDocument().getLength())));
        }
    }
}
