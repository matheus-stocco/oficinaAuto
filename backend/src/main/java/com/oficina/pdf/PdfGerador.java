package com.oficina.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import com.oficina.erro.NegocioException;
import java.io.ByteArrayOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Monta qualquer PDF do sistema: página A4, cabeçalho padrão da empresa e o corpo de cada documento. */
@Component
public class PdfGerador {
    @FunctionalInterface
    public interface Corpo {
        void escrever(Document doc) throws DocumentException;
    }

    private final CabecalhoPdf cabecalho;

    public PdfGerador(CabecalhoPdf cabecalho) {
        this.cabecalho = cabecalho;
    }

    public byte[] gerar(Corpo corpo) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 36, 40);
        try {
            PdfWriter.getInstance(doc, saida);
            doc.open();
            cabecalho.escrever(doc); // recusa (409) se faltar logo ou CNPJ
            corpo.escrever(doc);
            doc.close();
        } catch (DocumentException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível gerar o PDF.");
        }
        return saida.toByteArray();
    }
}
