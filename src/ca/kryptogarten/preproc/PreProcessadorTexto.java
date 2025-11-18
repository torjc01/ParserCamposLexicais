package ca.kryptogarten.preproc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Classe principal do programa PreProcessadorTexto.
 * Esta aplicação Java é projetada para pré-processar arquivos de texto,
 * aplicando normalizações como remoção de pontuação, remoção de diacríticos
 * e conversão para minúsculas. Suporta a leitura de arquivos de texto de
 * um diretório especificado ou de uma lista de arquivos individuais.
 * </p>
 *
 * <p>
 * O programa cria um arquivo de saída formatado com um timestamp,
 * onde o conteúdo normalizado é gravado. Linhas que começam com '#'
 * são tratadas como comentários e copiadas diretamente sem normalização.
 * </p>
 *
 * <p>
 * Novas funcionalidades incluem a capacidade de preservar colchetes '[', ']'
 * e chaves '{', '}' durante o processo de remoção de pontuação,
 * controlada por uma flag na linha de comando.
 * </p>
 *
 * <p>
 * Exemplo de uso via linha de comando:
 * <ul>
 *     <li><code>java PreProcessadorTexto /caminho/para/meu/diretorio_de_textos</code></li>
 *     <li><code>java PreProcessadorTexto /caminho/para/arquivo1.txt --preservar-colchetes-chaves /caminho/para/arquivo2.txt</code></li>
 *     <li><code>java PreProcessadorTexto -pcc /caminho/para/meu/diretorio_de_textos</code></li>
 * </ul>
 * </p>
 *
 * @author SeuNome
 * @version 1.1
 * @since 2023-10-27
 */
public class PreProcessadorTexto {

    private static final String COMMENT_PREFIX = "#";
    private static final int SEPARATOR_LINES = 10;
    private static final String FLAG_PRESERVAR_COLCHETES_CHAVES_LONG = "--preservar-colchetes-chaves";
    private static final String FLAG_PRESERVAR_COLCHETES_CHAVES_SHORT = "-pcc";
    private static final String FLAG_PRESERVAR_PONTUACAO_LONG = "--preservar-pontuacao";
    private static final String FLAG_PRESERVAR_PONTUACAO_SHORT = "-ppt";
    private static final String FLAG_PRESERVAR_UNDERSCORE_LONG = "--preservar-underscore";
    private static final String FLAG_PRESERVAR_UNDERSCORE_SHORT = "-pus";


    /**
     * O metodo principal que inicia a execução do programa.
     * Analisa os argumentos da linha de comando para determinar
     * os arquivos de entrada e coordena o processo de pré-processamento.
     *
     * @param args Array de Strings contendo os caminhos para os arquivos
     *             ou o diretório de entrada, e flags opcionais.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java PreProcessadorTexto [flags] <diretorio_ou_arquivo1.txt> [arquivo2.txt ...]");
            System.err.println("Flags disponíveis:");
            System.err.println("  " + FLAG_PRESERVAR_COLCHETES_CHAVES_LONG + " ou " + FLAG_PRESERVAR_COLCHETES_CHAVES_SHORT + ": Preserva colchetes [] e chaves {} na normalização.");
            System.err.println("  " + FLAG_PRESERVAR_PONTUACAO_LONG + " ou " + FLAG_PRESERVAR_PONTUACAO_SHORT + ": Preserva toda a pontuação na normalização.");
            System.err.println("  " + FLAG_PRESERVAR_UNDERSCORE_LONG + " ou " + FLAG_PRESERVAR_UNDERSCORE_SHORT + ": Preserva toda underscore na normalização.");
            System.exit(1);
        }

        List<String> argumentosLista = new ArrayList<>(Arrays.asList(args));
        boolean preservarColchetesEChaves = false;
        boolean preservarPontuacao = false;
        boolean preservarUnderscore = false;

        // Analisar flags
        if (argumentosLista.remove(FLAG_PRESERVAR_COLCHETES_CHAVES_LONG) ||
                argumentosLista.remove(FLAG_PRESERVAR_COLCHETES_CHAVES_SHORT)) {
            preservarColchetesEChaves = true;
            System.out.println("Flag detectada: Colchetes [] e Chaves {} serão preservados.");
        }

        if (argumentosLista.remove(FLAG_PRESERVAR_PONTUACAO_LONG) ||
                argumentosLista.remove(FLAG_PRESERVAR_PONTUACAO_SHORT)) {
            preservarPontuacao = true;
            System.out.println("Flag detectada: Pontuação será preservada.");
        }

        if(argumentosLista.remove(FLAG_PRESERVAR_UNDERSCORE_LONG) ||
                argumentosLista.remove(FLAG_PRESERVAR_UNDERSCORE_SHORT)) {
            preservarUnderscore = true;
            System.out.println("Flag detectada: underscore será preservado.");
        }

        if (argumentosLista.isEmpty()) {
            System.err.println("Nenhum arquivo ou diretório de entrada especificado após as flags.");
            System.err.println("Uso: java PreProcessadorTexto [flags] <diretorio_ou_arquivo1.txt> [arquivo2.txt ...]");
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

        GerenciadorSaida gerenciadorSaida = new GerenciadorSaida();
        List<String> nomesArquivosTratados = new ArrayList<>();
        int contadorArquivosTratados = 0;

        try {
            for (Path arquivoEntrada : arquivosParaProcessar) {
                System.out.println("Processando arquivo: " + arquivoEntrada.getFileName());
                nomesArquivosTratados.add(arquivoEntrada.getFileName().toString());
                contadorArquivosTratados++;

                List<String> linhas = Files.readAllLines(arquivoEntrada, StandardCharsets.UTF_8);

                for (String linha : linhas) {
                    if (linha.trim().startsWith(COMMENT_PREFIX)) {
                        // Linha de comentário, copiar ipsi litteris
                        gerenciadorSaida.escreverLinha(linha);
                    } else {
                        // Aplicar normalização, passando a flag para preservar colchetes/chaves
                        String linhaNormalizada = NormalizadorTexto.normalizar(linha, preservarColchetesEChaves, preservarPontuacao, preservarUnderscore);
                        gerenciadorSaida.escreverLinha(linhaNormalizada);
                    }
                }
                // Adicionar 10 linhas em branco como separador
                for (int i = 0; i < SEPARATOR_LINES; i++) {
                    gerenciadorSaida.escreverLinha("");
                }
            }

            // Mensagem final
            String outputFileName = gerenciadorSaida.getNomeArquivoSaida();

            // Formatar a lista de arquivos tratados para que cada um comece com "# "
            String listaArquivosComComentario = nomesArquivosTratados.stream()
                    .map(s -> COMMENT_PREFIX + "\t- " + s)
                    .collect(Collectors.joining("\n"));

            // Construir a mensagem final com cada linha começando com "# "
            String mensagemFinal = String.format(
                        COMMENT_PREFIX + " ===============================\n" +
                        COMMENT_PREFIX + " Fim do pré-processamento.\n" +
                        COMMENT_PREFIX + " ===============================\n" +
                        COMMENT_PREFIX + " Arquivos tratados nesta execução:\n" +
                        "%s\n" + // A lista já vem formatada
                        COMMENT_PREFIX + " Total de %d arquivos tratados\n" +
                        COMMENT_PREFIX + " Resultado gravado no arquivo de saída %s\n",
                    listaArquivosComComentario, contadorArquivosTratados, outputFileName
            );

            gerenciadorSaida.escreverLinha(mensagemFinal);
            System.out.println(mensagemFinal); // Exibir também no terminal

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
}