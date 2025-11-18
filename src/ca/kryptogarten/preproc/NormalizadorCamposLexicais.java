package ca.kryptogarten.preproc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Representa um normalizador de entradas de campos lexicais configurável.
 * Esta classe é responsável por coordenar a leitura de arquivos de texto,
 * a normalização de linhas que contêm campos lexicais e a escrita dos resultados
 * em um arquivo de saída.
 * </p>
 *
 * <p>
 * As entradas de campos lexicais devem estar no formato:
 * "[termoPrincipal, variacão1, variação2, ...]".
 * </p>
 *
 * <p>
 * A normalização de cada termo individual inclui:
 * <ol>
 *     <li>Extração dos termos da string de entrada, preservando a ordem original.</li>
 *     <li>Para cada termo, conversão de espaços e hífens em underscores ('_').</li>
 *     <li>Remoção de pontuação (exceto o underscore introduzido).</li>
 *     <li>Remoção de diacríticos.</li>
 *     <li>Conversão para minúsculas.</li>
 *     <li>Remoção de termos duplicados na lista final, mantendo a ordem da primeira ocorrência.</li>
 * </ol>
 * </p>
 *
 * <p>
 * A classe também trata linhas que começam com '#' como comentários,
 * copiando-as diretamente para o arquivo de saída sem normalização.
 * </p>
 *
 * <p>
 * Exemplo de uso programático:
 * <pre>{@code
 * List<Path> arquivos = Arrays.asList(Paths.get("entrada1.txt"));
 * NormalizadorCamposLexicais normalizador = new NormalizadorCamposLexicais(arquivos, "saida.txt");
 * normalizador.executarNormalizacao();
 * }</pre>
 * </p>
 *
 * @author SeuNome
 * @version 1.3
 * @since 2023-10-27
 */
public class NormalizadorCamposLexicais {

    private static final String COMMENT_PREFIX = "#";
    public static final String FLAG_OUTPUT_FILE_SHORT = "-o";
    public static final String FLAG_OUTPUT_FILE_LONG = "--output-file";

    // Padrão para capturar o conteúdo dentro dos colchetes da entrada lexical.
    private static final Pattern ENTRADA_LEXICAL_PATTERN = Pattern.compile("\\[(.*?)]");

    // Padrão para dividir a string de termos por vírgula seguida de zero ou mais espaços.
    private static final Pattern SEPARADOR_TERMOS_PATTERN = Pattern.compile(",\\s*");

    private final List<Path> arquivosDeEntrada;
    private final String nomeArquivoSaida;
    private final List<String> nomesArquivosTratados;
    private int contadorArquivosTratados;

    /**
     * Construtor para a classe NormalizadorCamposLexicais.
     * Prepara a instância para processar uma lista de arquivos de entrada
     * e gravar o resultado em um arquivo de saída especificado.
     *
     * @param arquivosDeEntrada Lista de objetos Path que apontam para os arquivos de texto a serem processados.
     * @param nomeArquivoSaida  O nome desejado para o arquivo de saída onde os resultados serão gravados.
     *                          Se {@code null} ou vazio, um nome padrão com timestamp será gerado.
     * @throws IllegalArgumentException Se a lista de arquivos de entrada for nula ou vazia.
     */
    public NormalizadorCamposLexicais(List<Path> arquivosDeEntrada, String nomeArquivoSaida) {
        if (arquivosDeEntrada == null || arquivosDeEntrada.isEmpty()) {
            throw new IllegalArgumentException("A lista de arquivos de entrada não pode ser nula ou vazia.");
        }
        this.arquivosDeEntrada = arquivosDeEntrada;
        this.nomeArquivoSaida = nomeArquivoSaida;
        this.nomesArquivosTratados = new ArrayList<>();
        this.contadorArquivosTratados = 0;
    }

    /**
     * Executa o processo completo de normalização dos campos lexicais.
     * Itera sobre os arquivos de entrada, lê cada linha, aplica a normalização
     * (incluindo tratamento de comentários e formato lexical) e escreve o resultado
     * no arquivo de saída. Ao final, gera uma mensagem de resumo.
     *
     * @throws IOException Se ocorrer um erro de E/S durante a leitura ou escrita dos arquivos.
     */
    public void executarNormalizacao() throws IOException {
        GerenciadorSaida gerenciadorSaida = new GerenciadorSaida(nomeArquivoSaida);

        try {
            for (Path arquivoEntrada : arquivosDeEntrada) {
                System.out.println("Processando arquivo: " + arquivoEntrada.getFileName());
                nomesArquivosTratados.add(arquivoEntrada.getFileName().toString());
                contadorArquivosTratados++;

                List<String> linhas = Files.readAllLines(arquivoEntrada, StandardCharsets.UTF_8);

                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- Conteúdo normalizado de " + arquivoEntrada.getFileName() + " ---");

                for (String linha : linhas) {
                    String linhaTrimmed = linha.trim();

                    // Trata linhas que começam com # como comentários e as copia diretamente
                    if (linhaTrimmed.startsWith(COMMENT_PREFIX)) {
                        gerenciadorSaida.escreverLinha(linha); // Copia a linha original, não o trimmed
                        continue;
                    }
                    // Ignora linhas vazias após o trim
                    if (linhaTrimmed.isEmpty()) {
                        gerenciadorSaida.escreverLinha("");
                        continue;
                    }

                    // Apenas processar linhas que se parecem com uma entrada lexical
                    if (ENTRADA_LEXICAL_PATTERN.matcher(linhaTrimmed).matches()) {
                        String linhaNormalizada = normalizarEntradaLexical(linhaTrimmed);
                        gerenciadorSaida.escreverLinha(linhaNormalizada);
                    } else {
                        // Opcional: Avisar se uma linha não corresponde ao formato esperado, como comentário
                        gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " Aviso: Linha ignorada por não ser formato lexical esperado: " + linha);
                    }
                }
                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- Fim do arquivo " + arquivoEntrada.getFileName() + " ---");
                gerenciadorSaida.escreverLinha(""); // Linha em branco para separar arquivos na saída
            }

            // Mensagem final (mantida no formato de comentário)
            String outputFileNameFinal = gerenciadorSaida.getNomeArquivoSaida();
            String listaArquivosComComentario = nomesArquivosTratados.stream()
                    .map(s -> COMMENT_PREFIX + " " + s)
                    .collect(Collectors.joining("\n"));

            String mensagemFinal = String.format(
                    COMMENT_PREFIX + " ===========================================\n" +
                            COMMENT_PREFIX + " Fim do pré-processamento de campos lexicais.\n" +
                            COMMENT_PREFIX + " Arquivos de entrada tratados nesta execução:\n" +
                            "%s\n" +
                            COMMENT_PREFIX + " Total de %d arquivos tratados\n" +
                            COMMENT_PREFIX + " Resultado gravado no arquivo de saída %s\n",
                    listaArquivosComComentario, contadorArquivosTratados, outputFileNameFinal
            );

            gerenciadorSaida.escreverLinha(mensagemFinal);
            System.out.println(mensagemFinal);

        } finally {
            gerenciadorSaida.fechar();
        }
    }

    /**
     * Normaliza uma string de entrada de campo lexical individual.
     * A entrada deve estar no formato "[termo1, termo2, ...]", onde cada termo
     * pode conter espaços ou hífens que serão convertidos em underscores.
     * Pontuação (exceto underscore), diacríticos e letras maiúsculas serão removidos/convertidos.
     * Termos duplicados na saída normalizada serão eliminados, mantendo a ordem da primeira ocorrência.
     *
     * @param entradaBruta A string de entrada no formato de campo lexical.
     *                     Ex: "[franco-maçonaria, franco maçonaria, franco maçonarias, maçonaria]"
     * @return Uma string representando a lista de termos normalizados, sem duplicatas,
     *         no formato "[termo_normalizado1, termo_normalizado2, ...]".
     *         Retorna uma string vazia "[]" se a entrada não for válida ou não contiver termos.
     */
    public static String normalizarEntradaLexical(String entradaBruta) {
        // Usamos LinkedHashSet para manter a ordem de inserção e evitar duplicatas.
        Set<String> termosNormalizadosSet = new LinkedHashSet<>();

        Matcher matcher = ENTRADA_LEXICAL_PATTERN.matcher(entradaBruta);
        if (matcher.find()) {
            String conteudoTermos = matcher.group(1);

            String[] termosArray = SEPARADOR_TERMOS_PATTERN.split(conteudoTermos);

            for (String termoOriginal : termosArray) {
                String termoTrimmed = termoOriginal.trim();

                if (termoTrimmed.isEmpty()) {
                    continue;
                }

                // 1. Converter espaços e hífens em underscores
                String termoComUnderscores = termoTrimmed.replaceAll("[ -]", "_");

                // 2. Aplicar normalização de texto (remover pontuação extra, diacríticos, minúsculas)
                // Chamada à NormalizadorTexto para remover diacríticos, converter para minúsculas
                // e remover qualquer outra pontuação que não seja o underscore.
                // A flag 'false' indica que não devemos preservar colchetes/chaves neste nível de normalização,
                // já que estamos tratando termos individuais.
                String termoFinalNormalizado = NormalizadorTexto.normalizar(termoComUnderscores, false, false, false);

                termosNormalizadosSet.add(termoFinalNormalizado);
            }
        }

        // Formata o LinkedHashSet de volta para uma String no formato "[termo1, termo2, ...]"
        return termosNormalizadosSet.stream().collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Metodo main para execução via linha de comando.
     * Este metodo agora serve como um "driver" que analisa os argumentos
     * e instancia/executa a classe NormalizadorCamposLexicais.
     *
     * @param args Argumentos de linha de comando.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java NormalizadorCamposLexicais <diretorio_ou_arquivo1.txt> [arquivo2.txt ...] [" + FLAG_OUTPUT_FILE_SHORT + " <arquivo_saida.txt>]");
            System.err.println("Flags disponíveis:");
            System.err.println("  " + FLAG_OUTPUT_FILE_SHORT + " ou " + FLAG_OUTPUT_FILE_LONG + ": Especifica o nome do arquivo de saída.");
            System.exit(1);
        }

        List<String> argumentosLista = new ArrayList<>(Arrays.asList(args));
        String nomeArquivoSaida = null;

        // Analisar flag de arquivo de saída
        int outputFlagIndex = argumentosLista.indexOf(FLAG_OUTPUT_FILE_SHORT);
        if (outputFlagIndex == -1) {
            outputFlagIndex = argumentosLista.indexOf(FLAG_OUTPUT_FILE_LONG);
        }

        if (outputFlagIndex != -1) {
            if (outputFlagIndex + 1 < argumentosLista.size()) {
                nomeArquivoSaida = argumentosLista.get(outputFlagIndex + 1);
                argumentosLista.remove(outputFlagIndex + 1); // Remove o nome do arquivo
                argumentosLista.remove(outputFlagIndex);     // Remove a flag
            } else {
                System.err.println("Erro: A flag " + FLAG_OUTPUT_FILE_SHORT + " ou " + FLAG_OUTPUT_FILE_LONG + " requer um nome de arquivo.");
                System.exit(1);
            }
        }

        if (argumentosLista.isEmpty()) {
            System.err.println("Nenhum arquivo ou diretório de entrada especificado.");
            System.err.println("Uso: java NormalizadorCamposLexicais <diretorio_ou_arquivo1.txt> [arquivo2.txt ...] [" + FLAG_OUTPUT_FILE_SHORT + " <arquivo_saida.txt>]");
            System.exit(1);
        }

        List<Path> arquivosParaProcessar = new ArrayList<>();
        Path entradaInicial = Paths.get(argumentosLista.getFirst());

        // Verifica se o primeiro argumento (após as flags) é um diretório
        if (Files.isDirectory(entradaInicial)) {
            System.out.println("Diretório detectado: " + entradaInicial);
            try (Stream<Path> stream = Files.walk(entradaInicial)) {
                arquivosParaProcessar = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                System.err.println("Erro ao listar arquivos no diretório " + entradaInicial + ": " + e.getMessage());
                System.exit(1);
            }
        } else {
            // Assume que todos os argumentos restantes são arquivos individuais
            for (String arg : argumentosLista) {
                Path arquivo = Paths.get(arg);
                if (Files.isRegularFile(arquivo) && arquivo.toString().toLowerCase().endsWith(".txt")) {
                    arquivosParaProcessar.add(arquivo);
                } else {
                    System.err.println("Aviso: Ignorando entrada inválida ou não .txt: " + arquivo);
                }
            }
        }

        if (arquivosParaProcessar.isEmpty()) {
            System.out.println("Nenhum arquivo .txt encontrado para processar. Encerrando.");
            System.exit(0);
        }

        try {
            NormalizadorCamposLexicais normalizador = new NormalizadorCamposLexicais(arquivosParaProcessar, nomeArquivoSaida);
            normalizador.executarNormalizacao();
        } catch (IllegalArgumentException e) {
            System.err.println("Erro de configuração: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Erro de E/S durante a execução: " + e.getMessage());
            System.exit(1);
        }
    }
}