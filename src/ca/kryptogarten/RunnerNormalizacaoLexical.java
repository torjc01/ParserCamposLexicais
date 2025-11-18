package ca.kryptogarten;
import ca.kryptogarten.preproc.NormalizadorCamposLexicais;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunnerNormalizacaoLexical {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Uso: java RunnerNormalizacaoLexical <diretorio_ou_arquivo1.txt> [arquivo2.txt ...] [" + NormalizadorCamposLexicais.FLAG_OUTPUT_FILE_SHORT + " <arquivo_saida.txt>]");
            System.err.println("Flags disponíveis:");
            System.err.println("  " + NormalizadorCamposLexicais.FLAG_OUTPUT_FILE_SHORT + " ou " + NormalizadorCamposLexicais.FLAG_OUTPUT_FILE_LONG + ": Especifica o nome do arquivo de saída.");
            System.exit(1);
        }

        List<String> argumentosLista = new ArrayList<>(Arrays.asList(args));
        String nomeArquivoSaida = null;

        // Analisar flag de arquivo de saída
        int outputFlagIndex = argumentosLista.indexOf(NormalizadorCamposLexicais.FLAG_OUTPUT_FILE_SHORT);
        if (outputFlagIndex == -1) {
            outputFlagIndex = argumentosLista.indexOf(NormalizadorCamposLexicais.FLAG_OUTPUT_FILE_LONG);
        }

        if (outputFlagIndex != -1) {
            if (outputFlagIndex + 1 < argumentosLista.size()) {
                nomeArquivoSaida = argumentosLista.get(outputFlagIndex + 1);
                argumentosLista.remove(outputFlagIndex + 1); // Remove o nome do arquivo
                argumentosLista.remove(outputFlagIndex);     // Remove a flag
            } else {
                System.err.println("Erro: A flag " + NormalizadorCamposLexicais.FLAG_OUTPUT_FILE_SHORT + " ou " + NormalizadorCamposLexicais.FLAG_OUTPUT_FILE_LONG + " requer um nome de arquivo.");
                System.exit(1);
            }
        }

        if (argumentosLista.isEmpty()) {
            System.err.println("Nenhum arquivo ou diretório de entrada especificado.");
            System.err.println("Uso: java RunnerNormalizacaoLexical <diretorio_ou_arquivo1.txt> [arquivo2.txt ...] [" + NormalizadorCamposLexicais.FLAG_OUTPUT_FILE_SHORT + " <arquivo_saida.txt>]");
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

