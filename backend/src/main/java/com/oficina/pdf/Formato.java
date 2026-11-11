package com.oficina.pdf;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação em português do Brasil usada nos PDFs. */
public final class Formato {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formato() {}

    public static String moeda(BigDecimal v) {
        // NumberFormat usa espaço não separável; troca por espaço comum, que a fonte do PDF desenha
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(v).replace(' ', ' ');
    }

    public static String data(LocalDate d) {
        return d == null ? "" : DATA.format(d);
    }
}
