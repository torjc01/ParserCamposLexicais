package ca.kryptogarten.preproc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Classe responsável por normalizar expressões em um corpus de texto,
 * baseando-se em um arquivo de campos lexicais previamente normalizado.
 * </p>
 *
 * <p>
 * A classe lê um arquivo de definições lexicais (onde expressões compostas são unidas por underscores,
 * ex: "franco_maçonaria") e varre os textos do corpus procurando por essas expressões
 * em sua forma natural (separadas por espaços ou hífens). Quando encontradas,
 * elas são convertidas para o formato com underscore.
 * </p>
 *
 * <p>
 * O processo respeita a precedência de expressões mais longas para evitar
 * substituições parciais incorretas. Linhas iniciadas com '#' são tratadas
 * como comentários e preservadas.
 * </p>
 *
 * @author Julio Cesar Torres dos Santos
 * @version 1.0
 * @since 2025-11-08
 */
public class NormalizadorExpressao {

    private static final String COMMENT_PREFIX = "#";
    private static final String DEFAULT_OUTPUT_FILE = "norm_corpus_inter.txt";

    // Regex para extrair termos dentro dos colchetes do arquivo lexical: [termo1, termo2]
    private static final Pattern LEXICAL_ENTRY_PATTERN = Pattern.compile("\\[(.*?)\\]");

    private final List<Path> arquivosCorpus;
    private final Path arquivoLexical;
    private final String nomeArquivoSaida;

    // Lista de pares (Padrão Regex, String de Substituição)
    private final List<Substituicao> regrasDeSubstituicao;

    /**
     * Classe auxiliar interna para armazenar uma regra de substituição pré-compilada.
     */
    private static class Substituicao {
        final Pattern padraoBusca;
        final String termoSubstituto;

        Substituicao(String termoLexical) {
            this.termoSubstituto = termoLexical;
            // Cria um regex dinâmico:
            // 1. Substitui os underscores do termo lexical por regex de espaço ou hífen [\s-]+
            // 2. Adiciona \b (word boundary) no início e fim para casar apenas palavras inteiras
            // 3. Adiciona (?i) para ser case-insensitive (ignorar maiúsculas/minúsculas na busca)
            String regexString = "(?i)\\b" + termoLexical.replaceAll("_", "[\\\\s-]+") + "\\b";
            this.padraoBusca = Pattern.compile(regexString);
        }
    }

    /**
     * Construtor da classe.
     *
     * @param arquivosCorpus Lista de arquivos de texto do corpus a serem processados.
     * @param arquivoLexical O caminho para o arquivo contendo os campos lexicais normalizados.
     * @param nomeArquivoSaida O nome do arquivo de saída (se null, usa o padrão).
     */
    public NormalizadorExpressao(List<Path> arquivosCorpus, Path arquivoLexical, String nomeArquivoSaida) {
        if (arquivosCorpus == null || arquivosCorpus.isEmpty()) {
            throw new IllegalArgumentException("A lista de arquivos do corpus não pode ser vazia.");
        }
        if (arquivoLexical == null || !Files.exists(arquivoLexical)) {
            throw new IllegalArgumentException("O arquivo de campos lexicais é inválido ou não existe.");
        }

        this.arquivosCorpus = arquivosCorpus;
        this.arquivoLexical = arquivoLexical;
        this.nomeArquivoSaida = (nomeArquivoSaida != null && !nomeArquivoSaida.isEmpty()) ? nomeArquivoSaida : DEFAULT_OUTPUT_FILE;
        this.regrasDeSubstituicao = new ArrayList<>();

        carregarRegrasDeSubstituicao();
    }

    /**
     * Lê o arquivo de campos lexicais, extrai as expressões compostas (com underscore),
     * ordena-as por tamanho (decrescente) e cria os padrões regex para busca.
     */
    private void carregarRegrasDeSubstituicao() {
        System.out.println("Carregando regras do arquivo lexical: " + arquivoLexical);
        List<String> todosTermos = new ArrayList<>();

        try {
            List<String> linhas = Files.readAllLines(arquivoLexical, StandardCharsets.UTF_8);
            for (String linha : linhas) {
                Matcher matcher = LEXICAL_ENTRY_PATTERN.matcher(linha);
                if (matcher.find()) {
                    String conteudo = matcher.group(1);
                    // Divide por vírgula e limpa espaços
                    String[] termos = conteudo.split(",");
                    for (String t : termos) {
                        String termoLimpo = t.trim();
                        // Só nos interessam termos que tenham underscore (são expressões compostas)
                        if (termoLimpo.contains("_")) {
                            todosTermos.add(termoLimpo);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo lexical: " + e.getMessage(), e);
        }

        // ORDENAÇÃO IMPORTANTE: Do mais longo para o mais curto.
        // Isso evita que "casa_branca" substitua parte de "casa_branca_de_neve" incorretamente.
        todosTermos.sort((s1, s2) -> s2.length() - s1.length());

        // Compila os padrões regex
        for (String termo : todosTermos) {
            regrasDeSubstituicao.add(new Substituicao(termo));
        }

        System.out.println("Regras de substituição carregadas: " + regrasDeSubstituicao.size() + " expressões compostas identificadas.");
    }

    /**
     * Executa o processo de normalização no corpus.
     *
     * @throws IOException Se ocorrer erro de leitura/escrita.
     */
    public void executar() throws IOException {
        GerenciadorSaida gerenciadorSaida = new GerenciadorSaida(nomeArquivoSaida);

        try {
            int countArquivos = 0;

            for (Path arquivo : arquivosCorpus) {
                System.out.println("Processando corpus: " + arquivo.getFileName());
                countArquivos++;

                List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);

                // Cabeçalho visual no arquivo de saída para separar arquivos originais
                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- [NormalizadorExpressao.class] Início do processamento de: " + arquivo.getFileName() + " ---");

                for (String linha : linhas) {
                    // 1. Se for comentário, copia ipsi litteris
                    if (linha.trim().startsWith(COMMENT_PREFIX)) {
                        gerenciadorSaida.escreverLinha(linha);
                        continue;
                    }

                    // 2. Processa a linha aplicando as substituições
                    String linhaProcessada = aplicarSubstituicoes(linha);
                    gerenciadorSaida.escreverLinha(linhaProcessada);
                }

                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- Fim de: " + arquivo.getFileName() + " ---");
                gerenciadorSaida.escreverLinha(""); // Linha em branco
            }

            // Mensagem final
            String msgFinal = String.format(
                    """
                            %s ===========================================
                            %s Fim da normalização de expressões.
                            %s Total de %d arquivos de corpus processados.
                            %s Resultado gravado em: %s""",
                    COMMENT_PREFIX, COMMENT_PREFIX, COMMENT_PREFIX, countArquivos, COMMENT_PREFIX, gerenciadorSaida.getNomeArquivoSaida()
            );

            gerenciadorSaida.escreverLinha(msgFinal);
            System.out.println(msgFinal.replace(COMMENT_PREFIX + " ", "")); // Print limpo no console

        } finally {
            gerenciadorSaida.fechar();
        }
    }

    /**
     * Aplica todas as regras de substituição carregadas em uma linha de texto.
     *
     * @param linha A linha original do corpus.
     * @return A linha com as expressões convertidas para o formato com underscore.
     */
    private String aplicarSubstituicoes(String linha) {
        String linhaAtual = linha;

        for (Substituicao regra : regrasDeSubstituicao) {
            // O matcher procura o padrão (ex: "franco maçonaria") e substitui pelo termo (ex: "franco_maçonaria")
            // Como o regex é (?i), ele acha "Franco Maçonaria", mas substitui pelo termo do dicionário (geralmente minúsculo).
            linhaAtual = regra.padraoBusca.matcher(linhaAtual).replaceAll(regra.termoSubstituto);
        }

        return linhaAtual;
    }

    /**
     * Método principal para execução via linha de comando.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java NormalizadorExpressoes <diretorio_ou_arquivo_corpus> <arquivo_campos_lexicais.txt>");
            System.exit(1);
        }

        String caminhoCorpus = args[0];
        String caminhoLexical = args[1];

        // Lista arquivos do corpus
        List<Path> arquivosCorpus = new ArrayList<>();
        Path entradaCorpus = Paths.get(caminhoCorpus);

        if (Files.isDirectory(entradaCorpus)) {
            try (Stream<Path> stream = Files.walk(entradaCorpus)) {
                arquivosCorpus = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                System.err.println("Erro ao listar corpus: " + e.getMessage());
                System.exit(1);
            }
        } else {
            if (Files.isRegularFile(entradaCorpus)) {
                arquivosCorpus.add(entradaCorpus);
            } else {
                System.err.println("Arquivo de corpus inválido: " + caminhoCorpus);
                System.exit(1);
            }
        }

        if (arquivosCorpus.isEmpty()) {
            System.err.println("Nenhum arquivo de texto encontrado no caminho do corpus.");
            System.exit(1);
        }

        try {
            // Instancia e executa
            NormalizadorExpressao normalizador = new NormalizadorExpressao(
                    arquivosCorpus,
                    Paths.get(caminhoLexical),
                    DEFAULT_OUTPUT_FILE
            );

            normalizador.executar();

        } catch (Exception e) {
            System.err.println("Erro durante a execução: " + e.getMessage());
            e.printStackTrace();
        }
    }
}