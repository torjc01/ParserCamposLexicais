package ca.kryptogarten.utils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileUtils {

    /**
     * Encapsula o resultado do carregamento do arquivo de termos.
     * @param variationToBaseMap O mapa de variações para termos base.
     * @param logicalTermCount O número de termos lógicos (linhas) lidos do arquivo.
     */
    public record TermLoadResult(Map<String, String> variationToBaseMap, int logicalTermCount) {}

    public static TermLoadResult loadSearchTermsFromFile(String termsFilePath) throws IOException {
        Map <String, String> variationToBaseMap = new LinkedHashMap<>();
        int logicalTermCount = 0;

        return new TermLoadResult(variationToBaseMap, logicalTermCount);
    }

    /**
     * Lê o conteúdo de um arquivo e o retorna como uma String.
     * @param filePath O caminho para o arquivo.
     * @return O conteúdo do arquivo ou null em caso de erro.
     */
    public static String readFileToString(String filePath) {
        try {
            System.out.println("Leitura do arquivo :" + filePath);
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + filePath);
            return null;
        }finally{
            System.out.println("Arquivo lido com sucesso");
        }
    }

    /**
     * Quebra string
     * @param content conteúdo de texto a analisar.
     */
    public static void showContent(String content) {
        String[] lines = content.split("\\r?\\n|\\r");

        // The regex "\\r?\\n|\\r" handles both \n, \r\n, and standalone \r
        // Each element in 'lines' will contain a line of text without the line break character.
        int i =0;
        for (String line : lines) {
            System.out.println(i + " " + line);
            i++;
        }
    }


}