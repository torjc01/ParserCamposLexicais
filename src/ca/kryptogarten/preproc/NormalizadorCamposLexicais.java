package ca.kryptogarten.preproc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet; // Importar LinkedHashSet
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * <p>
 * Classe principal para normalizar entradas de campos lexicais.
 * Ela recebe um diretório ou uma lista de arquivos de texto como entrada.
 * Cada arquivo de entrada deve conter uma ou mais linhas no formato de campo lexical:
 * "[termoPrincipal, variacão1, variação2, ...]".
 * </p>
 *
 * <p>
 * Para cada linha de campo lexical encontrada, a normalização inclui:
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
 * O programa também trata linhas que começam com '#' como comentários,
 * copiando-as diretamente para o arquivo de saída sem normalização.
 * </p>
 *
 * <p>
 * O resultado normalizado de cada linha/entrada lexical é gravado em um arquivo de saída,
 * com o nome especificado pelo usuário (ou um nome padrão com timestamp).
 * </p>
 *
 * <p>
 * Exemplo de uso via linha de comando:
 * <ul>
 *     <li><code>java NormalizadorCamposLexicais /caminho/para/meu/diretorio_com_campos_lexicais -o saida_lexical.txt</code></li>
 *     <li><code>java NormalizadorCamposLexicais /caminho/para/arquivo_lexical1.txt /caminho/para/arquivo_lexical2.txt</code></li>
 * </ul>
 * </p>
 *
 * @author SeuNome
 * @version 1.2
 * @since 2023-10-27
 */
public class NormalizadorCamposLexicais {

    private static final String COMMENT_PREFIX = "#";
    private static final String FLAG_OUTPUT_FILE_SHORT = "-o";
    private static final String FLAG_OUTPUT_FILE_LONG = "--output-file";

    // Padrão para capturar o conteúdo dentro dos colchetes da entrada lexical.
    private static final Pattern ENTRADA_LEXICAL_PATTERN = Pattern.compile("\\[(.*?)\\]");

    // Padrão para dividir a string de termos por vírgula seguida de zero ou mais espaços.
    private static final Pattern SEPARADOR_TERMOS_PATTERN = Pattern.compile(",\\s*");

    /**
     * O metodo principal que inicia a execução do programa NormalizadorCamposLexicais.
     * Analisa os argumentos da linha de comando para determinar os arquivos de entrada,
     * o nome do arquivo de saída, e coordena o processo de normalização lexical.
     *
     * @param args Array de Strings contendo os caminhos para os arquivos
     *             ou o diretório de entrada, e a flag opcional para o arquivo de saída.
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

        GerenciadorSaida gerenciadorSaida = new GerenciadorSaida(nomeArquivoSaida); // Usa o construtor com nome
        List<String> nomesArquivosTratados = new ArrayList<>();
        int contadorArquivosTratados = 0;

        try {
            for (Path arquivoEntrada : arquivosParaProcessar) {
                System.out.println("Processando arquivo: " + arquivoEntrada.getFileName());
                nomesArquivosTratados.add(arquivoEntrada.getFileName().toString());
                contadorArquivosTratados++;

                List<String> linhas = Files.readAllLines(arquivoEntrada, StandardCharsets.UTF_8);

                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- Conteúdo normalizado de " + arquivoEntrada.getFileName() + " ---");

                for (String linha : linhas) {
                    // Trata linhas que começam com # como comentários e as copia diretamente
                    if (linha.trim().startsWith(COMMENT_PREFIX)) {
                        gerenciadorSaida.escreverLinha(linha);
                        continue; // Pula para a próxima linha
                    }
                    // Ignora linhas vazias após o trim
                    if (linha.trim().isEmpty()) {
                        gerenciadorSaida.escreverLinha(""); // Copia linha vazia
                        continue;
                    }

                    // Apenas processar linhas que se parecem com uma entrada lexical
                    if (ENTRADA_LEXICAL_PATTERN.matcher(linha).matches()) {
                        String linhaNormalizada = normalizarEntradaLexical(linha);
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

        } catch (IOException e) {
            System.err.println("Erro de E/S durante o processamento: " + e.getMessage());
        } finally {
            try {
                gerenciadorSaida.fechar();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o arquivo de saída: " + e.getMessage());
            }
        }
    }

    /**
     * Normaliza uma string de entrada de campo lexical.
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
                String termoFinalNormalizado = NormalizadorTexto.normalizar(termoComUnderscores, false,false, true);

                termosNormalizadosSet.add(termoFinalNormalizado);
            }
        }

        // Formata o LinkedHashSet de volta para uma String no formato "[termo1, termo2, ...]"
        return termosNormalizadosSet.stream().collect(Collectors.joining(", ", "[", "]"));
    }
}
