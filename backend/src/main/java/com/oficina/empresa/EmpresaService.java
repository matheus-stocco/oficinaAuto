package com.oficina.empresa;

import com.oficina.erro.NegocioException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmpresaService {
    static final long MAX_LOGO = 2L * 1024 * 1024;
    static final long MAX_FUNDO = 5L * 1024 * 1024;

    private final EmpresaRepository repo;
    private final Path pasta;

    public EmpresaService(EmpresaRepository repo, @Value("${app.uploads.dir}") String pasta) {
        this.repo = repo;
        this.pasta = Path.of(pasta);
    }

    public Empresa buscar() {
        return repo.findById(1L).orElseThrow();
    }

    @Transactional
    public Empresa atualizar(EmpresaForm form) {
        Empresa e = buscar();
        e.setNome(form.nome().trim());
        // shortcut: só confere os 14 dígitos do CNPJ, não os dígitos verificadores; validar se virar problema
        String cnpj = form.cnpj() == null ? "" : form.cnpj().replaceAll("\\D", "");
        if (!cnpj.isEmpty() && cnpj.length() != 14) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "O CNPJ precisa ter 14 dígitos.");
        }
        e.setCnpj(cnpj.isEmpty() ? null : cnpj);
        e.setTelefone(vazioParaNulo(form.telefone()));
        e.setEndereco(vazioParaNulo(form.endereco()));
        if ("IMAGEM".equals(form.fundoTipo()) && e.getFundoArquivo() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Envie a imagem de fundo antes de escolhê-la.");
        }
        if ("COR".equals(form.fundoTipo()) && form.fundoCor() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Escolha a cor de fundo.");
        }
        e.setFundoTipo(form.fundoTipo());
        e.setFundoCor(form.fundoCor());
        e.setOrcamentoRascunhoHoras(form.orcamentoRascunhoHoras());
        return tocar(e);
    }

    @Transactional
    public Empresa salvarLogo(MultipartFile arquivo) {
        Empresa e = buscar();
        e.setLogoArquivo(gravar("logo", arquivo, MAX_LOGO));
        return tocar(e);
    }

    @Transactional
    public Empresa removerLogo() {
        Empresa e = buscar();
        apagar(e.getLogoArquivo());
        e.setLogoArquivo(null);
        return tocar(e);
    }

    @Transactional
    public Empresa salvarFundo(MultipartFile arquivo) {
        Empresa e = buscar();
        e.setFundoArquivo(gravar("fundo", arquivo, MAX_FUNDO));
        e.setFundoTipo("IMAGEM");
        return tocar(e);
    }

    @Transactional
    public Empresa removerFundo() {
        Empresa e = buscar();
        apagar(e.getFundoArquivo());
        e.setFundoArquivo(null);
        if ("IMAGEM".equals(e.getFundoTipo())) e.setFundoTipo(null);
        return tocar(e);
    }

    /** O arquivo está mesmo no disco? O banco pode apontar para um logo cujo volume de uploads não foi montado. */
    public boolean existe(String nomeArquivo) {
        return nomeArquivo != null && Files.isRegularFile(pasta.resolve(nomeArquivo));
    }

    /** Lê o arquivo salvo (logo ou fundo) ou devolve null se não existir. */
    public byte[] ler(String nomeArquivo) {
        if (nomeArquivo == null) return null;
        try {
            return Files.readAllBytes(pasta.resolve(nomeArquivo));
        } catch (IOException ex) {
            return null;
        }
    }

    /** Tipo pela extensão que nós mesmos gravamos (png ou jpg). */
    public static String tipoDe(String nomeArquivo) {
        return nomeArquivo.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    /** Aceita só PNG e JPG, pela assinatura do conteúdo (a extensão e o content-type enviados são ignorados). */
    String gravar(String base, MultipartFile arquivo, long limite) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "Escolha um arquivo.");
        }
        if (arquivo.getSize() > limite) {
            throw new NegocioException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Arquivo muito grande (máximo " + limite / 1024 / 1024 + " MB).");
        }
        try {
            byte[] b = arquivo.getBytes();
            String ext = extensaoPelaAssinatura(b);
            Files.createDirectories(pasta);
            apagar(base + ".png");
            apagar(base + ".jpg");
            Files.write(pasta.resolve(base + "." + ext), b);
            return base + "." + ext;
        } catch (IOException ex) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível salvar o arquivo.");
        }
    }

    static String extensaoPelaAssinatura(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return "png";
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        throw new NegocioException(HttpStatus.BAD_REQUEST, "Envie uma imagem PNG ou JPG.");
    }

    private void apagar(String nomeArquivo) {
        if (nomeArquivo == null) return;
        try {
            Files.deleteIfExists(pasta.resolve(nomeArquivo));
        } catch (IOException ignorado) {
            // arquivo órfão não quebra nada; some na próxima troca
        }
    }

    private Empresa tocar(Empresa e) {
        e.setAtualizadoEm(LocalDateTime.now());
        return repo.save(e);
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
