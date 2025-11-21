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
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Classe responsável por pré-processar arquivos de texto bruto.
 * Aplica normalizações (pontuação, diacríticos, minúsculas) linha a linha.
 * </p>
 *
 * @author Julio Cesar Torres dos Santos
 * @version 1.2
 * @since 2025-11-08
 */
public class PreProcessadorTexto {

    private static final String COMMENT_PREFIX = "#";
    private static final int SEPARATOR_LINES = 10;

    // Constantes de Flags para uso no main e validação
    public static final String FLAG_PCC_LONG = "--preservar-colchetes-chaves";
    public static final String FLAG_PCC_SHORT = "-pcc";
    public static final String FLAG_PPT_LONG = "--preservar-pontuacao";
    public static final String FLAG_PPT_SHORT = "-ppt";
    public static final String FLAG_PUS_LONG = "--preservar-underscore";
    public static final String FLAG_PUS_SHORT = "-pus";

    private final List<Path> arquivosEntrada;
    private final Configuracao config;
    private final String nomeArquivoSaida; // Opcional, se null usa padrão do GerenciadorSaida

    /**
     * Classe interna para encapsular as opções de normalização.
     */
    public static class Configuracao {
        boolean preservarColchetesEChaves = false;
        boolean preservarPontuacao = false;
        boolean preservarUnderscore = false;

        public Configuracao(boolean pcc, boolean ppt, boolean pus) {
            this.preservarColchetesEChaves = pcc;
            this.preservarPontuacao = ppt;
            this.preservarUnderscore = pus;
        }
    }

    /**
     * Construtor principal para uso programático (Runner).
     *
     * @param arquivosEntrada Lista de arquivos a processar.
     * @param config Objeto de configuração com as flags booleanas.
     * @param nomeArquivoSaida Nome do arquivo de saída (pode ser null).
     */
    public PreProcessadorTexto(List<Path> arquivosEntrada, Configuracao config, String nomeArquivoSaida) {
        if (arquivosEntrada == null || arquivosEntrada.isEmpty()) {
            throw new IllegalArgumentException("A lista de arquivos de entrada não pode ser vazia.");
        }
        this.arquivosEntrada = arquivosEntrada;
        this.config = (config != null) ? config : new Configuracao(false, false, false);
        this.nomeArquivoSaida = nomeArquivoSaida;
    }

    /**
     * Método utilitário estático para localizar arquivos de entrada.
     *
     * @param caminhoEntrada Caminho para um diretório ou arquivo único.
     * @return Lista de Paths contendo arquivos .txt encontrados.
     * @throws IOException Se houver erro ao acessar o sistema de arquivos.
     */
    public static List<Path> listarArquivosEntrada(Path caminhoEntrada) throws IOException {
           return GerenciadorEntrada.listarArquivosEntrada(caminhoEntrada);
    }

    /**
     * Executa o processo de pré-processamento.
     *
     * @throws IOException Se ocorrer erro de IO.
     */
    public void executar() throws IOException {
        // Se nomeArquivoSaida for null, o construtor vazio do GerenciadorSaida usa o padrão com timestamp
        GerenciadorSaida gerenciadorSaida = (nomeArquivoSaida != null)
                ? new GerenciadorSaida(nomeArquivoSaida)
                : new GerenciadorSaida();

        List<String> nomesArquivosTratados = new ArrayList<>();
        int contadorArquivosTratados = 0;

        try {
            for (Path arquivoEntrada : arquivosEntrada) {
                System.out.println("Processando texto bruto: " + arquivoEntrada.getFileName());
                nomesArquivosTratados.add(arquivoEntrada.getFileName().toString());
                contadorArquivosTratados++;

                List<String> linhas = Files.readAllLines(arquivoEntrada, StandardCharsets.UTF_8);

                for (String linha : linhas) {
                    if (linha.trim().startsWith(COMMENT_PREFIX)) {
                        gerenciadorSaida.escreverLinha(linha);
                    } else {
                        String linhaNormalizada = NormalizadorTexto.normalizar(
                                linha,
                                config.preservarColchetesEChaves,
                                config.preservarPontuacao,
                                config.preservarUnderscore
                        );
                        gerenciadorSaida.escreverLinha(linhaNormalizada);
                    }
                }

                // Adicionar linhas separadoras entre arquivos
                for (int i = 0; i < SEPARATOR_LINES; i++) {
                    gerenciadorSaida.escreverLinha("");
                }
            }

            String outputFileName = gerenciadorSaida.getNomeArquivoSaida();
            String listaArquivosComComentario = nomesArquivosTratados.stream()
                    .map(s -> COMMENT_PREFIX + "\t- " + s)
                    .collect(Collectors.joining("\n"));

            String mensagemFinal = String.format(
                    COMMENT_PREFIX + " ===============================\n" +
                            COMMENT_PREFIX + " Fim do pré-processamento básico.\n" +
                            COMMENT_PREFIX + " ===============================\n" +
                            COMMENT_PREFIX + " Arquivos tratados:\n" +
                            "%s\n" +
                            COMMENT_PREFIX + " Total de %d arquivos tratados\n" +
                            COMMENT_PREFIX + " Resultado gravado em %s\n",
                    listaArquivosComComentario, contadorArquivosTratados, outputFileName
            );

            gerenciadorSaida.escreverLinha(mensagemFinal);
            System.out.println(mensagemFinal);

        } finally {
            gerenciadorSaida.fechar();
        }
    }

    // --------------------------------------------------------------------------------
    // Método MAIN (Interface CLI)
    // --------------------------------------------------------------------------------
    public static void main(String[] args) {
        if (args.length == 0) {
            imprimirUso();
            System.exit(1);
        }

        List<String> argumentosLista = new ArrayList<>(Arrays.asList(args));
        boolean pcc = false;
        boolean ppt = false;
        boolean pus = false;

        // Parsing de flags
        if (argumentosLista.remove(FLAG_PCC_LONG) || argumentosLista.remove(FLAG_PCC_SHORT)) {
            pcc = true;
            System.out.println("Flag detectada: Preservar Colchetes e Chaves.");
        }
        if (argumentosLista.remove(FLAG_PPT_LONG) || argumentosLista.remove(FLAG_PPT_SHORT)) {
            ppt = true;
            System.out.println("Flag detectada: Preservar Pontuação.");
        }
        if (argumentosLista.remove(FLAG_PUS_LONG) || argumentosLista.remove(FLAG_PUS_SHORT)) {
            pus = true;
            System.out.println("Flag detectada: Preservar Underscore.");
        }

        if (argumentosLista.isEmpty()) {
            System.err.println("Nenhum arquivo ou diretório especificado.");
            imprimirUso();
            System.exit(1);
        }

        Configuracao config = new Configuracao(pcc, ppt, pus);
        List<Path> arquivosProcessar = new ArrayList<>();

        try {
            for (String arg : argumentosLista) {
                List<Path> encontrados = listarArquivosEntrada(Paths.get(arg));
                if (encontrados.isEmpty()) {
                    System.err.println("Aviso: Nenhum arquivo .txt encontrado em: " + arg);
                }
                arquivosProcessar.addAll(encontrados);
            }

            if (arquivosProcessar.isEmpty()) {
                System.out.println("Nenhum arquivo válido para processar. Encerrando.");
                System.exit(0);
            }

            // Instancia e Executa (Saída null = nome padrão gerado)
            PreProcessadorTexto preProc = new PreProcessadorTexto(arquivosProcessar, config, null);
            preProc.executar();

        } catch (IOException e) {
            System.err.println("Erro de E/S: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void imprimirUso() {
        System.err.println("Uso: java PreProcessadorTexto [flags] <diretorio_ou_arquivo1> ...");
        System.err.println("Flags:");
        System.err.println("  " + FLAG_PCC_SHORT + " : Preservar colchetes [] e chaves {}");
        System.err.println("  " + FLAG_PPT_SHORT + " : Preservar pontuação");
        System.err.println("  " + FLAG_PUS_SHORT + " : Preservar underscore");
    }
}