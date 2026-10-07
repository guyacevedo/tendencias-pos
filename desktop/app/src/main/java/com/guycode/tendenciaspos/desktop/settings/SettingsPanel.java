package com.guycode.tendenciaspos.desktop.settings;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.contracts.settings.InvoiceFormat;
import com.guycode.tendenciaspos.contracts.settings.StoreSettingsResponse;
import com.guycode.tendenciaspos.desktop.core.Dates;
import com.guycode.tendenciaspos.ui.Fields;
import com.guycode.tendenciaspos.ui.Icons;
import com.guycode.tendenciaspos.ui.LoadingOverlay;
import com.guycode.tendenciaspos.ui.Toast;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

/** Datos de la tienda que encabezan facturas y reportes. */
public final class SettingsPanel extends JPanel implements SettingsView {
    private static final long serialVersionUID = 1L;

    private final LoadingOverlay overlay = new LoadingOverlay();
    private final JTextField storeName = Fields.text("Nombre de la tienda");
    private final JTextField taxId = Fields.text("NIT");
    private final JTextField address = Fields.text("Dirección");
    private final JTextField phone = Fields.text("Teléfono");
    private final JRadioButton letter = new JRadioButton("Carta");
    private final JRadioButton receipt = new JRadioButton("Tirilla 80 mm");
    private final JButton save = new JButton("Guardar", Icons.of(MaterialDesignC.CONTENT_SAVE_OUTLINE, Icons.SMALL));
    private final JLabel updatedAt = new JLabel();
    private final JLabel error = new JLabel();
    private final Map<String, JLabel> fieldErrors = new LinkedHashMap<>();

    public SettingsPanel() {
        super(new MigLayout("fill, insets 24", "[grow, fill]", "[grow, fill]"));

        var form = new JPanel(new MigLayout("wrap 2, insets 24, gap 10 6", "[140!][grow, fill, 260::520]", ""));
        form.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $Tpos.sidebarBackground");

        var title = new JLabel("Configuración de la tienda");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        form.add(title, "span 2, gapbottom 12");

        addField(form, "Nombre", storeName, "storeName");
        addField(form, "NIT", taxId, "taxId");
        addField(form, "Dirección", address, "address");
        addField(form, "Teléfono", phone, "phone");

        var formats = new ButtonGroup();
        formats.add(letter);
        formats.add(receipt);
        var formatRow = new JPanel(new MigLayout("insets 0, gap 16", "", ""));
        formatRow.setOpaque(false);
        formatRow.add(receipt);
        formatRow.add(letter);
        form.add(caption("Factura"));
        form.add(formatRow);

        error.setIcon(Icons.of(MaterialDesignA.ALERT_OUTLINE, Icons.SMALL, "Tpos.dangerColor"));
        error.setIconTextGap(8);
        error.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.dangerColor");
        error.setVisible(false);
        form.add(error, "span 2, gaptop 8, hidemode 3");

        updatedAt.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        form.add(updatedAt, "span 2, gaptop 4");
        form.add(save, "span 2, alignx left, h 34!, gaptop 8");

        add(overlay.wrap(form), "aligny top");
    }

    /** Registra el guardado con lo que haya en el formulario. */
    public void onSave(Runnable action) {
        save.addActionListener(e -> action.run());
    }

    public String storeNameValue() {
        return storeName.getText();
    }

    public String taxIdValue() {
        return taxId.getText();
    }

    public String addressValue() {
        return address.getText();
    }

    public String phoneValue() {
        return phone.getText();
    }

    public InvoiceFormat invoiceFormatValue() {
        return letter.isSelected() ? InvoiceFormat.LETTER : InvoiceFormat.RECEIPT_80MM;
    }

    /** Un cajero puede ver la configuración pero no cambiarla. */
    public void setEditable(boolean editable) {
        storeName.setEditable(editable);
        taxId.setEditable(editable);
        address.setEditable(editable);
        phone.setEditable(editable);
        letter.setEnabled(editable);
        receipt.setEnabled(editable);
        save.setVisible(editable);
    }

    @Override
    public void showLoading(boolean loading) {
        overlay.setActive(loading);
        if (loading) {
            error.setVisible(false);
        }
    }

    @Override
    public void showSettings(StoreSettingsResponse settings) {
        storeName.setText(settings.storeName());
        taxId.setText(settings.taxId());
        address.setText(settings.address());
        phone.setText(settings.phone());
        letter.setSelected(settings.invoiceFormat() == InvoiceFormat.LETTER);
        receipt.setSelected(settings.invoiceFormat() != InvoiceFormat.LETTER);
        updatedAt.setText("Última actualización: " + Dates.dateTime(settings.updatedAt()));
        showFieldErrors(Map.of());
    }

    @Override
    public void showFieldErrors(Map<String, String> errors) {
        fieldErrors.forEach((field, label) -> {
            var message = errors.get(field);
            label.setText(message == null ? "" : message);
            label.setVisible(message != null);
        });
        Fields.markInvalid(storeName, errors.containsKey("storeName"));
        Fields.markInvalid(taxId, errors.containsKey("taxId"));
        Fields.markInvalid(address, errors.containsKey("address"));
        Fields.markInvalid(phone, errors.containsKey("phone"));
        revalidate();
    }

    @Override
    public void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        revalidate();
    }

    @Override
    public void showSaved() {
        Toast.success(this, "Configuración guardada.");
    }

    private void addField(JPanel form, String label, JTextField field, String name) {
        form.add(caption(label));
        form.add(field);
        var message = new JLabel();
        message.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.dangerColor");
        message.setVisible(false);
        fieldErrors.put(name, message);
        form.add(message, "skip 1, hidemode 3, gapbottom 2");
    }

    private static JComponent caption(String text) {
        var label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        return label;
    }
}
