package com.oficina.veiculos;

import com.oficina.erro.NegocioException;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/** Placa normalizada (maiúscula, sem hífen ou espaço). Aceita o formato antigo e o Mercosul. */
public final class Placa {
    private static final Pattern ANTIGA = Pattern.compile("[A-Z]{3}[0-9]{4}");
    private static final Pattern MERCOSUL = Pattern.compile("[A-Z]{3}[0-9][A-Z][0-9]{2}");

    private Placa() {}

    public static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /** Devolve a placa normalizada, ou null se vier vazia (veículo sem placa). */
    public static String validar(String texto) {
        String p = normalizar(texto);
        if (p.isEmpty()) return null;
        if (!ANTIGA.matcher(p).matches() && !MERCOSUL.matcher(p).matches()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Placa inválida. Use o formato ABC-1234 ou ABC1D23.");
        }
        return p;
    }
}
