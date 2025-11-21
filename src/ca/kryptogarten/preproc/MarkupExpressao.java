package ca.kryptogarten.preproc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Classe responsável por normalizar a formatação de arquivos de campos lexicais.
 * </p>
 *
 * <p>
 * O objetivo é ler arquivos contendo listas de termos no formato [termo1, termo 2, ...]
 * e converter termos compostos (separados por espaço, hífen ou apóstrofo)
 * para o formato unido por underscore (ex: termo_2).
 * </p>
 *
 * <p>
 * Exemplo de transformação:
 * Entrada: [termo, termo composto, termo-composto-hifem, L'étendard]
 * Saída:   [termo, termo_composto, termo_composto_hifem, L_étendard]
 * </p>
 *
 * @author Julio Cesar Torres dos Santos
 * @version 1.0
 * @since 2025-11-08
 */
public class MarkupExpressao {

    private static final String COMMENT_PREFIX = "#";
    private static final String DEFAULT_OUTPUT_FILE = "campos_lexicais_norm.txt";

    // Regex para capturar o conteúdo dentro dos colchetes: [conteudo]
    private static final Pattern BRACKET_CONTENT_PATTERN = Pattern.compile("\\[(.*?)\\]");

    // Regex para identificar os separadores que devem virar underscore:
    // \s (espaço), - (hífen), ' (apóstrofo)
    private static final String SEPARATOR_REGEX = "[\\s\\-']+";

    private final List<Path> arquivosEntrada;
    private final String nomeArquivoSaida;

    /**
     * Construtor da classe.
     *
     * @param arquivosEntrada Lista de arquivos (campos lexicais brutos) a serem processados.
     * @param nomeArquivoSaida O nome do arquivo de saída consolidado (se null, usa o padrão).
     */
    public MarkupExpressao(List<Path> arquivosEntrada, String nomeArquivoSaida) {
        if (arquivosEntrada == null || arquivosEntrada.isEmpty()) {
            throw new IllegalArgumentException("A lista de arquivos de entrada não pode ser vazia.");
        }

        this.arquivosEntrada = arquivosEntrada;
        this.nomeArquivoSaida = (nomeArquivoSaida != null && !nomeArquivoSaida.isEmpty()) ? nomeArquivoSaida : DEFAULT_OUTPUT_FILE;
    }

    /**
     * Método utilitário estático para localizar arquivos de entrada.
     *
     * @param caminhoEntrada Caminho para um diretório ou arquivo único.
     * @return Lista de Paths contendo arquivos .txt encontrados.
     * @throws IOException Se houver erro ao acessar o sistema de arquivos.
     */
    public static List<Path> listarArquivosEntrada(Path caminhoEntrada) throws IOException {
        if (Files.isDirectory(caminhoEntrada)) {
            try (Stream<Path> stream = Files.walk(caminhoEntrada)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                        .collect(Collectors.toList());
            }
        } else if (Files.isRegularFile(caminhoEntrada)) {
            return Collections.singletonList(caminhoEntrada);
        } else {
            throw new IOException("O caminho de entrada informado não existe ou não é válido: " + caminhoEntrada);
        }
    }

    /**
     * Executa o processo de normalização dos campos lexicais.
     *
     * @throws IOException Se ocorrer erro de leitura/escrita.
     */
    public void executar() throws IOException {
        // Assume-se a existência da classe GerenciadorSaida no mesmo pacote (conforme contexto anterior)
        GerenciadorSaida gerenciadorSaida = new GerenciadorSaida(nomeArquivoSaida);

        try {
            int countArquivos = 0;

            for (Path arquivo : arquivosEntrada) {
                System.out.println("Processando arquivo lexical: " + arquivo.getFileName());
                countArquivos++;

                List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);

                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- [MarkupExpressao.class] Processando origem: " + arquivo.getFileName() + " ---");

                for (String linha : linhas) {
                    // 1. Preserva comentários
                    if (linha.trim().startsWith(COMMENT_PREFIX)) {
                        gerenciadorSaida.escreverLinha(linha);
                        continue;
                    }

                    // 2. Normaliza a linha
                    String linhaNormalizada = normalizarLinha(linha);
                    gerenciadorSaida.escreverLinha(linhaNormalizada);
                }

                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- Fim de: " + arquivo.getFileName() + " ---");
                gerenciadorSaida.escreverLinha("");
            }

            String msgFinal = String.format(
                    """
                            %s ===========================================
                            %s Fim da marcação de expressões.
                            %s Total de %d arquivos processados.
                            %s Resultado gravado em: %s""",
                    COMMENT_PREFIX, COMMENT_PREFIX, COMMENT_PREFIX, countArquivos, COMMENT_PREFIX, gerenciadorSaida.getNomeArquivoSaida()
            );

            gerenciadorSaida.escreverLinha(msgFinal);
            System.out.println(msgFinal.replace(COMMENT_PREFIX + " ", ""));

        } finally {
            gerenciadorSaida.fechar();
        }
    }

    /**
     * Processa uma linha procurando pelo padrão [termo1, termo2] e aplica a normalização
     * (underscores) nos termos encontrados.
     *
     * @param linha A linha original.
     * @return A linha com os termos normalizados.
     */
    private String normalizarLinha(String linha) {
        Matcher matcher = BRACKET_CONTENT_PATTERN.matcher(linha);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            // O grupo 1 contém o texto dentro dos colchetes: "termo, termo composto, ..."
            String conteudoInterno = matcher.group(1);
            String conteudoProcessado = processarTermosInternos(conteudoInterno);

            // Substitui o match original pelo novo conteúdo envolto em colchetes
            // appendReplacement lida com a substituição na string original
            matcher.appendReplacement(sb, "[" + Matcher.quoteReplacement(conteudoProcessado) + "]");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Recebe a string interna dos colchetes, separa por vírgula e normaliza cada termo.
     *
     * @param conteudoBruto String ex: "termo, termo composto, L'étendard"
     * @return String ex: "termo, termo_composto, L_étendard"
     */
    private String processarTermosInternos(String conteudoBruto) {
        if (conteudoBruto == null || conteudoBruto.trim().isEmpty()) {
            return conteudoBruto;
        }

        String[] termos = conteudoBruto.split(",");
        List<String> termosNormalizados = new ArrayList<>();

        for (String termo : termos) {
            String t = termo.trim();
            if (!t.isEmpty()) {
                // Substitui espaços, hifens e apóstrofos por underscore
                String tNorm = t.replaceAll(SEPARATOR_REGEX, "_");
                termosNormalizados.add(tNorm);
            }
        }

        // Reconstrói a string separada por vírgula e espaço
        return String.join(", ", termosNormalizados);
    }

    // --------------------------------------------------------------------------------
    // Método MAIN para execução standalone ou testes locais
    // --------------------------------------------------------------------------------
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java MarkupExpressao <diretorio_ou_arquivo_input> [arquivo_saida_opcional]");
            System.exit(1);
        }

        try {
            String caminhoInputStr = args[0];
            String arquivoSaidaStr = (args.length >= 2) ? args[1] : null;

            // 1. Identifica arquivos de entrada (reutilizável pelo Runner)
            List<Path> arquivosInput = MarkupExpressao.listarArquivosEntrada(Paths.get(caminhoInputStr));

            if (arquivosInput.isEmpty()) {
                System.err.println("Nenhum arquivo .txt encontrado no caminho especificado.");
                System.exit(1);
            }

            // 2. Instanciação via new()
            MarkupExpressao markup = new MarkupExpressao(arquivosInput, arquivoSaidaStr);

            // 3. Execução
            markup.executar();

        } catch (Exception e) {
            System.err.println("Erro durante a execução: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}