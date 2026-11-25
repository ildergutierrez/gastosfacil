package com.example.gastofacil;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    static {
        currencyFormat.setMaximumFractionDigits(2);
    }

    /**
     * Formato completo sin abreviaciones.
     * Usado en el modal de detalles.
     */
    public static String formatFull(double value) {
        return currencyFormat.format(value);
    }

    /**
     * Formato corto (k, M, B) para etiquetas pequeñas.
     */
    public static String formatShort(double value) {
        double absValue = Math.abs(value);
        String prefix = value < 0 ? "-" : "";
        
        if (absValue >= 1_000_000_000) {
            double valB = absValue / 1_000_000_000.0;
            if (valB == (long) valB) {
                return String.format(new Locale("es", "CO"), "%s$%dB", prefix, (long) valB);
            }
            return String.format(new Locale("es", "CO"), "%s$%.1fB", prefix, valB);
        } else if (absValue >= 1_000_000) {
            double valM = absValue / 1_000_000.0;
            if (valM == (long) valM) {
                return String.format(new Locale("es", "CO"), "%s$%dM", prefix, (long) valM);
            }
            return String.format(new Locale("es", "CO"), "%s$%.1fM", prefix, valM);
        } else if (absValue >= 1_000) {
            double valK = absValue / 1_000.0;
            if (valK == (long) valK) {
                return String.format(new Locale("es", "CO"), "%s$%dk", prefix, (long) valK);
            }
            return String.format(new Locale("es", "CO"), "%s$%.1fk", prefix, valK);
        } else {
            return currencyFormat.format(value);
        }
    }

    /**
     * Formato para el balance principal.
     * Muestra hasta 12 dígitos, luego usa abreviación.
     */
    public static String formatBalance(double value) {
        // 12 dígitos es hasta 999.999.999.999 (999 Billones en escala corta, pero aquí el usuario pidió B para Billones)
        // Si el valor absoluto supera 999,999,999,999 entonces abreviar.
        if (Math.abs(value) >= 1_000_000_000_000.0) {
            return formatShort(value);
        }
        return currencyFormat.format(value);
    }
}
