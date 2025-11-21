package ca.kryptogarten.preproc;

import ca.kryptogarten.utils.GerenciadorEntrada;
import ca.kryptogarten.utils.GerenciadorSaida;

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
 * @author SeuNome
 * @version 1.4
 * @since 2023-10-27
 */
public class NormalizadorCamposLexicais {

    private static final String COMMENT_PREFIX = "#";
    public static final String FLAG_OUTPUT_FILE_SHORT = "-o";
    public static final String FLAG_OUTPUT_FILE_LONG = "--output-file";

    // Padrão para capturar o conteúdo dentro dos colchetes da entrada lexical.
    private static final Pattern ENTRADA_LEXICAL_PATTERN = Pattern.compile("\\[(.*?)\\]");

    // Padrão para dividir a string de termos por vírgula seguida de zero ou mais espaços.
    private static final Pattern SEPARADOR_TERMOS_PATTERN = Pattern.compile(",\\s*");

    private final List<Path> arquivosDeEntrada;
    private final String nomeArquivoSaida;
    private final List<String> nomesArquivosTratados;
    private int contadorArquivosTratados;

    /**
     * Construtor para a classe NormalizadorCamposLexicais.
     * Prepara a instância para processar uma lista de arquivos de entrada.
     *
     * @param arquivosDeEntrada Lista de objetos Path que apontam para os arquivos de texto a serem processados.
     * @param nomeArquivoSaida  O nome desejado para o arquivo de saída. Se null, o GerenciadorSaida definirá o padrão.
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
     * Método utilitário estático para localizar arquivos de entrada.
     * Pode ser usado tanto pelo método main quanto por classes Runner externas.
     *
     * @param caminhoEntrada Caminho para um diretório ou arquivo único.
     * @return Lista de Paths contendo arquivos .txt encontrados.
     * @throws IOException Se houver erro ao acessar o sistema de arquivos.
     */
    public static List<Path> listarArquivosEntrada(Path caminhoEntrada) throws IOException {
        return GerenciadorEntrada.listarArquivosEntrada(caminhoEntrada);
    }

    /**
     * Executa o processo completo de normalização dos campos lexicais.
     *
     * @throws IOException Se ocorrer um erro de E/S durante a leitura ou escrita.
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

                    // Comentários
                    if (linhaTrimmed.startsWith(COMMENT_PREFIX)) {
                        gerenciadorSaida.escreverLinha(linha);
                        continue;
                    }
                    // Linhas vazias
                    if (linhaTrimmed.isEmpty()) {
                        gerenciadorSaida.escreverLinha("");
                        continue;
                    }

                    // Processamento Lexical
                    if (ENTRADA_LEXICAL_PATTERN.matcher(linhaTrimmed).matches()) {
                        String linhaNormalizada = normalizarEntradaLexical(linhaTrimmed);
                        gerenciadorSaida.escreverLinha(linhaNormalizada);
                    } else {
                        gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " Aviso: Linha ignorada por não ser formato lexical esperado: " + linha);
                    }
                }
                gerenciadorSaida.escreverLinha(COMMENT_PREFIX + " --- Fim do arquivo " + arquivoEntrada.getFileName() + " ---");
                gerenciadorSaida.escreverLinha("");
            }

            // Mensagem Final
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
     *
     * @param entradaBruta A string de entrada no formato de campo lexical.
     * @return Uma string representando a lista de termos normalizados, sem duplicatas.
     */
    public static String normalizarEntradaLexical(String entradaBruta) {
        Set<String> termosNormalizadosSet = new LinkedHashSet<>();

        Matcher matcher = ENTRADA_LEXICAL_PATTERN.matcher(entradaBruta);
        if (matcher.find()) {
            String conteudoTermos = matcher.group(1);
            String[] termosArray = SEPARADOR_TERMOS_PATTERN.split(conteudoTermos);

            for (String termoOriginal : termosArray) {
                String termoTrimmed = termoOriginal.trim();
                if (termoTrimmed.isEmpty()) continue;

                // 1. Converter espaços e hífens em underscores
                String termoComUnderscores = termoTrimmed.replaceAll("[\\' -]", "_");

                // 2. NormalizadorTexto: remove diacríticos, minúsculas, mantém underscores
                String termoFinalNormalizado = NormalizadorTexto.normalizar(termoComUnderscores, false, false, true);

                termosNormalizadosSet.add(termoFinalNormalizado);
            }
        }

        return termosNormalizadosSet.stream().collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Método main para execução via linha de comando.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java NormalizadorCamposLexicais <diretorio_ou_arquivo1.txt> [arquivo2.txt ...] [" + FLAG_OUTPUT_FILE_SHORT + " <arquivo_saida.txt>]");
            System.exit(1);
        }

        List<String> argumentosLista = new ArrayList<>(Arrays.asList(args));
        String nomeArquivoSaida = null;

        // 1. Extração de Flags (Argument Parsing)
        int outputFlagIndex = argumentosLista.indexOf(FLAG_OUTPUT_FILE_SHORT);
        if (outputFlagIndex == -1) {
            outputFlagIndex = argumentosLista.indexOf(FLAG_OUTPUT_FILE_LONG);
        }

        if (outputFlagIndex != -1) {
            if (outputFlagIndex + 1 < argumentosLista.size()) {
                nomeArquivoSaida = argumentosLista.get(outputFlagIndex + 1);
                argumentosLista.remove(outputFlagIndex + 1); // Remove o valor
                argumentosLista.remove(outputFlagIndex);     // Remove a flag
            } else {
                System.err.println("Erro: A flag " + FLAG_OUTPUT_FILE_SHORT + " requer um nome de arquivo.");
                System.exit(1);
            }
        }

        if (argumentosLista.isEmpty()) {
            System.err.println("Nenhum arquivo ou diretório de entrada especificado.");
            System.exit(1);
        }

        // 2. Coleta de Arquivos usando o Helper Estático
        List<Path> arquivosParaProcessar = new ArrayList<>();
        try {
            for (String caminhoStr : argumentosLista) {
                Path caminho = Paths.get(caminhoStr);
                List<Path> encontrados = listarArquivosEntrada(caminho);

                if (encontrados.isEmpty()) {
                    System.err.println("Aviso: Nenhum arquivo .txt válido encontrado em: " + caminhoStr);
                }
                arquivosParaProcessar.addAll(encontrados);
            }
        } catch (IOException e) {
            System.err.println("Erro ao acessar sistema de arquivos: " + e.getMessage());
            System.exit(1);
        }

        if (arquivosParaProcessar.isEmpty()) {
            System.out.println("Nenhum arquivo .txt encontrado para processar. Encerrando.");
            System.exit(0);
        }

        // 3. Instanciação e Execução
        try {
            NormalizadorCamposLexicais normalizador = new NormalizadorCamposLexicais(arquivosParaProcessar, nomeArquivoSaida);
            normalizador.executarNormalizacao();
        } catch (Exception e) {
            System.err.println("Erro durante a execução: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}