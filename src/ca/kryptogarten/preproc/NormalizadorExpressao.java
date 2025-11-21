package ca.kryptogarten.preproc;

import ca.kryptogarten.utils.GerenciadorEntrada;
import ca.kryptogarten.utils.GerenciadorSaida;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Classe responsável por normalizar expressões em um corpus de texto,
 * baseando-se em um arquivo de campos lexicais previamente normalizado.
 * </p>
 *
 * @author Julio Cesar Torres dos Santos
 * @version 1.1
 * @since 2025-11-08
 */
public class NormalizadorExpressao {

    private static final String COMMENT_PREFIX = "#";
    private static final String DEFAULT_OUTPUT_FILE = "norm_corpus_inter.txt";
    private static final Pattern LEXICAL_ENTRY_PATTERN = Pattern.compile("\\[(.*?)\\]");

    private final List<Path> arquivosCorpus;
    private final Path arquivoLexical;
    private final String nomeArquivoSaida;
    private final List<Substituicao> regrasDeSubstituicao;

    /**
     * Classe auxiliar interna para armazenar uma regra de substituição pré-compilada.
     */
    private static class Substituicao {
        final Pattern padraoBusca;
        final String termoSubstituto;

        Substituicao(String termoLexical) {
            this.termoSubstituto = termoLexical;
            String regexString = "(?i)\\b" + termoLexical.replaceAll("_", "[\\\\s-]+") + "\\b";
            this.padraoBusca = Pattern.compile(regexString);
        }
    }

    /**
     * Construtor da classe.
     * Prepara o normalizador carregando as regras, mas não executa o processamento.
     *
     * @param arquivosCorpus Lista de arquivos de texto do corpus a serem processados.
     * @param arquivoLexical O caminho para o arquivo contendo os campos lexicais normalizados.
     * @param nomeArquivoSaida O nome do arquivo de saída (pode ser null, assumirá padrão).
     */
    public NormalizadorExpressao(List<Path> arquivosCorpus, Path arquivoLexical, String nomeArquivoSaida) {
        if (arquivosCorpus == null || arquivosCorpus.isEmpty()) {
            throw new IllegalArgumentException("A lista de arquivos do corpus não pode ser vazia.");
        }
        if (arquivoLexical == null || !Files.exists(arquivoLexical)) {
            throw new IllegalArgumentException("O arquivo de campos lexicais é inválido ou não existe: " + arquivoLexical);
        }

        this.arquivosCorpus = arquivosCorpus;
        this.arquivoLexical = arquivoLexical;
        this.nomeArquivoSaida = (nomeArquivoSaida != null && !nomeArquivoSaida.isEmpty()) ? nomeArquivoSaida : DEFAULT_OUTPUT_FILE;
        this.regrasDeSubstituicao = new ArrayList<>();

        carregarRegrasDeSubstituicao();
    }

    /**
     * Método utilitário estático para localizar arquivos de corpus.
     * Pode ser usado tanto pelo método main quanto por classes Runner externas.
     *
     * @param caminhoEntrada Caminho para um diretório ou arquivo único.
     * @return Lista de Paths contendo arquivos .txt encontrados.
     * @throws IOException Se houver erro ao acessar o sistema de arquivos.
     */
    public static List<Path> listarArquivosDoCorpus(Path caminhoEntrada) throws IOException {
        return GerenciadorEntrada.listarArquivosEntrada(caminhoEntrada);
    }

    /**
     * Executa o processo de normalização no corpus.
     *
     * @throws IOException Se ocorrer erro de leitura/escrita.
     */
    public void executar() throws IOException {
        // OBS: Assumindo que a classe GerenciadorSaida existe no pacote conforme seu código original
        GerenciadorSaida gerenciadorSaida = new GerenciadorSaida(nomeArquivoSaida);

        try {
            int countArquivos = 0;

            for (Path arquivo : arquivosCorpus) {
                System.out.println("Processando corpus: " + arquivo.getFileName());
                countArquivos++;

                List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);

                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- [NormalizadorExpressao.class] Início do processamento de: " + arquivo.getFileName() + " ---");

                for (String linha : linhas) {
                    if (linha.trim().startsWith(COMMENT_PREFIX)) {
                        gerenciadorSaida.escreverLinha(linha);
                        continue;
                    }
                    String linhaProcessada = aplicarSubstituicoes(linha);
                    gerenciadorSaida.escreverLinha(linhaProcessada);
                }

                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- Fim de: " + arquivo.getFileName() + " ---");
                gerenciadorSaida.escreverLinha("");
            }

            String msgFinal = String.format(
                    """
                            %s ===========================================
                            %s Fim da normalização de expressões.
                            %s Total de %d arquivos de corpus processados.
                            %s Resultado gravado em: %s""",
                    COMMENT_PREFIX, COMMENT_PREFIX, COMMENT_PREFIX, countArquivos, COMMENT_PREFIX, gerenciadorSaida.getNomeArquivoSaida()
            );

            gerenciadorSaida.escreverLinha(msgFinal);
            System.out.println(msgFinal.replace(COMMENT_PREFIX + " ", ""));

        } finally {
            gerenciadorSaida.fechar();
        }
    }

    private void carregarRegrasDeSubstituicao() {
        System.out.println("Carregando regras do arquivo lexical: " + arquivoLexical);
        List<String> todosTermos = new ArrayList<>();

        try {
            List<String> linhas = Files.readAllLines(arquivoLexical, StandardCharsets.UTF_8);
            for (String linha : linhas) {
                Matcher matcher = LEXICAL_ENTRY_PATTERN.matcher(linha);
                if (matcher.find()) {
                    String conteudo = matcher.group(1);
                    String[] termos = conteudo.split(",");
                    for (String t : termos) {
                        String termoLimpo = t.trim();
                        if (termoLimpo.contains("_")) {
                            todosTermos.add(termoLimpo);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo lexical: " + e.getMessage(), e);
        }

        todosTermos.sort((s1, s2) -> s2.length() - s1.length());

        for (String termo : todosTermos) {
            regrasDeSubstituicao.add(new Substituicao(termo));
        }

        System.out.println("Regras de substituição carregadas: " + regrasDeSubstituicao.size() + " expressões compostas identificadas.");
    }

    private String aplicarSubstituicoes(String linha) {
        String linhaAtual = linha;
        for (Substituicao regra : regrasDeSubstituicao) {
            linhaAtual = regra.padraoBusca.matcher(linhaAtual).replaceAll(regra.termoSubstituto);
        }
        return linhaAtual;
    }

    // --------------------------------------------------------------------------------
    // Método MAIN para execução standalone ou testes unitários
    // --------------------------------------------------------------------------------
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java NormalizadorExpressao <diretorio_ou_arquivo_corpus> <arquivo_campos_lexicais.txt>");
            System.exit(1);
        }

        try {
            String caminhoCorpusStr = args[0];
            String caminhoLexicalStr = args[1];

            // 1. Usa o método estático para resolver os arquivos (mesma lógica que o Runner usaria)
            List<Path> arquivosCorpus = listarArquivosDoCorpus(Paths.get(caminhoCorpusStr));

            if (arquivosCorpus.isEmpty()) {
                System.err.println("Nenhum arquivo de texto encontrado no caminho do corpus.");
                System.exit(1);
            }

            // 2. Instancia a classe via construtor (Invocação padrão via new)
            NormalizadorExpressao normalizador = new NormalizadorExpressao(
                    arquivosCorpus,
                    Paths.get(caminhoLexicalStr),
                    DEFAULT_OUTPUT_FILE
            );

            // 3. Executa a lógica
            normalizador.executar();

        } catch (Exception e) {
            System.err.println("Erro durante a execução: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}