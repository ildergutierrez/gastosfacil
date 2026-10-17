package com.example.gastofacil;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyTextWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isUpdating = false;
    private final DecimalFormat decimalFormat;
    private final Runnable onTextChangedCallback;

    public CurrencyTextWatcher(EditText editText, Runnable onTextChangedCallback) {
        this.editText = editText;
        this.onTextChangedCallback = onTextChangedCallback;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "ES"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        decimalFormat = new DecimalFormat("#,##0", symbols);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (isUpdating) {
            return;
        }

        isUpdating = true;

        try {
            String originalString = s.toString();

            if (originalString.isEmpty()) {
                isUpdating = false;
                if (onTextChangedCallback != null) {
                    onTextChangedCallback.run();
                }
                return;
            }

            String cleanString = originalString.replaceAll("[^\\d]", "");

            if (cleanString.isEmpty()) {
                editText.setText("");
                editText.setSelection(editText.getText().length());
                isUpdating = false;
                if (onTextChangedCallback != null) {
                    onTextChangedCallback.run();
                }
                return;
            }

            long parsedValue = Long.parseLong(cleanString);
            String formattedString = decimalFormat.format(parsedValue);

            editText.setText(formattedString);
            editText.setSelection(editText.getText().length());

        } catch (NumberFormatException e) {
            // ignore
        }

        isUpdating = false;
        if (onTextChangedCallback != null) {
            onTextChangedCallback.run();
        }
    }
}
