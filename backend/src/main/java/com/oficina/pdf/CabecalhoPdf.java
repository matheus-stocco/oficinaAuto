package com.oficina.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.oficina.empresa.Empresa;
import com.oficina.empresa.EmpresaService;
import com.oficina.erro.NegocioException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Cabeçalho único de todos os PDFs do sistema: logo, nome da empresa, endereço, CNPJ e telefone.
 * Sem logo ou sem CNPJ o PDF não é gerado, e a mensagem diz o que falta preencher.
 */
@Component
public class CabecalhoPdf {
    private static final Font NOME = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font LINHA = new Font(Font.HELVETICA, 9);

    private final EmpresaService empresas;

    public CabecalhoPdf(EmpresaService empresas) {
        this.empresas = empresas;
    }

    public void escrever(Document doc) throws DocumentException {
        Empresa e = empresas.buscar();
        byte[] logo = empresas.ler(e.getLogoArquivo());
        exigirDados(e, logo);

        PdfPTable tabela = new PdfPTable(new float[]{1.2f, 4f});
        tabela.setWidthPercentage(100);

        PdfPCell celulaLogo = new PdfPCell();
        celulaLogo.setBorder(Rectangle.NO_BORDER);
        try {
            Image imagem = Image.getInstance(logo);
            imagem.scaleToFit(90, 60);
            celulaLogo.addElement(imagem);
        } catch (java.io.IOException ex) {
            throw new NegocioException(HttpStatus.CONFLICT, "O logo enviado não pôde ser lido. Envie outro em Configurações.");
        }
        tabela.addCell(celulaLogo);

        PdfPCell celulaTexto = new PdfPCell();
        celulaTexto.setBorder(Rectangle.NO_BORDER);
        celulaTexto.addElement(new Paragraph(e.getNome(), NOME));
        if (e.getEndereco() != null) celulaTexto.addElement(new Paragraph(e.getEndereco(), LINHA));
        celulaTexto.addElement(new Paragraph("CNPJ " + cnpj(e.getCnpj())
                + (e.getTelefone() == null ? "" : "   |   Tel. " + e.getTelefone()), LINHA));
        tabela.addCell(celulaTexto);

        tabela.setSpacingAfter(6);
        doc.add(tabela);
        com.lowagie.text.pdf.draw.LineSeparator linha = new com.lowagie.text.pdf.draw.LineSeparator(0.5f, 100, java.awt.Color.GRAY, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(linha));
        doc.add(Chunk.NEWLINE);
    }

    /** Dados que o cabeçalho exige. Telefone e endereço são opcionais; logo e CNPJ não. */
    static void exigirDados(Empresa e, byte[] logo) {
        List<String> falta = new ArrayList<>();
        if (e.getNome() == null || e.getNome().isBlank()) falta.add("nome da empresa");
        if (logo == null) falta.add("logo");
        if (e.getCnpj() == null) falta.add("CNPJ");
        if (!falta.isEmpty()) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Para gerar o PDF, preencha em Configurações: " + String.join(" e ", falta) + ".");
        }
    }

    static String cnpj(String d) {
        return d.length() != 14 ? d : d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8)
                + "/" + d.substring(8, 12) + "-" + d.substring(12);
    }
}
