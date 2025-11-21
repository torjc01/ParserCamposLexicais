import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Classe para buscar eficientemente uma lista de palavras/expressões em um arquivo de texto.
 * Utiliza o algoritmo Aho-Corasick para realizar a busca em uma única passagem pelo texto.
 */
public class ParserCamposLexicais {

    /**
     * Encapsula o resultado do carregamento do arquivo de termos.
     * @param variationToBaseMap O mapa de variações para termos base.
     * @param logicalTermCount O número de termos lógicos (linhas) lidos do arquivo.
     */
    public record TermLoadResult(Map<String, String> variationToBaseMap, int logicalTermCount) {}
    
    /**
     * Encapsula o resultado da busca no arquivo de texto.
     * @param occurrences O mapa de termos base para sua contagem de ocorrências.
     * @param totalWordCount O número total de palavras encontradas no texto.
     */
    public record SearchResult(Map<String, Integer> occurrences, long totalWordCount) {}

    // --- Aho-Corasick Node ---
    private static class Node {
        private final Map<Character, Node> children = new HashMap<>();
        private Node failureLink = null;
        private final List<String> output = new ArrayList<>();
    }

    private final Node root;
    private final Map<String, String> variationToBaseMap;

    public ParserCamposLexicais(Map<String, String> variationToBaseMap) {
        if (variationToBaseMap == null || variationToBaseMap.isEmpty()) {
            throw new IllegalArgumentException("O mapa de variações para termos base não pode ser nulo ou vazio.");
        }
        this.root = new Node();
        this.variationToBaseMap = variationToBaseMap;
        buildTrie();
        buildFailureLinks();
    }

    private void buildTrie() {
        for (String variation : this.variationToBaseMap.keySet()) {
            Node currentNode = this.root;
            String lowerCaseVariation = variation.toLowerCase();
            for (char ch : lowerCaseVariation.toCharArray()) {
                currentNode = currentNode.children.computeIfAbsent(ch, k -> new Node());
            }
            currentNode.output.add(this.variationToBaseMap.get(variation));
        }
    }

    private void buildFailureLinks() {
        Queue<Node> queue = new LinkedList<>();
        root.failureLink = root;
        for (Node child : root.children.values()) {
            child.failureLink = root;
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            for (Map.Entry<Character, Node> entry : currentNode.children.entrySet()) {
                char ch = entry.getKey();
                Node childNode = entry.getValue();
                queue.add(childNode);
                Node tempFailureLink = currentNode.failureLink;
                while (tempFailureLink.children.get(ch) == null && tempFailureLink != root) {
                    tempFailureLink = tempFailureLink.failureLink;
                }
                if (tempFailureLink.children.containsKey(ch)) {
                    childNode.failureLink = tempFailureLink.children.get(ch);
                } else {
                    childNode.failureLink = root;
                }
                childNode.output.addAll(childNode.failureLink.output);
            }
        }
    }
    
    /**
     * Realiza a busca no arquivo de texto e retorna um objeto com os resultados e estatísticas.
     *
     * @param filePath O caminho para o arquivo de texto.
     * @return Um objeto SearchResult contendo o mapa de ocorrências e a contagem total de palavras.
     * @throws IOException Se ocorrer um erro ao ler o arquivo.
     */
    public SearchResult searchInFile(String filePath) throws IOException {
        Map<String, Integer> results = new HashMap<>();
        Set<String> uniqueBaseTerms = new HashSet<>(this.variationToBaseMap.values());
        for (String baseTerm : uniqueBaseTerms) {
            results.put(baseTerm, 0);
        }

        Node currentNode = this.root;
        long totalWordCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
                    continue;
                }

                // Contagem de palavras na linha
                String[] words = trimmedLine.split("\\s+");
                totalWordCount += words.length;

                // Processamento Aho-Corasick da linha
                for (char c : line.toCharArray()) {
                    char character = Character.toLowerCase(c);
                    currentNode = findNextState(currentNode, character);
                    if (!currentNode.output.isEmpty()) {
                        for (String baseTerm : currentNode.output) {
                            results.compute(baseTerm, (k, v) -> v + 1);
                        }
                    }
                }
                
                currentNode = findNextState(currentNode, Character.toLowerCase('\n'));
                if (!currentNode.output.isEmpty()) {
                    for (String baseTerm : currentNode.output) {
                        results.compute(baseTerm, (k, v) -> v + 1);
                    }
                }
            }
        }
        return new SearchResult(results, totalWordCount);
    }

    private Node findNextState(Node currentState, char character) {
        Node nextState = currentState;
        while (nextState.children.get(character) == null && nextState != root) {
            nextState = nextState.failureLink;
        }
        return nextState.children.getOrDefault(character, root);
    }

    /**
     * Carrega os termos de busca de um arquivo e retorna um objeto com os dados carregados.
     *
     * @param termsFilePath O caminho para o arquivo de termos.
     * @return Um objeto TermLoadResult contendo o mapa de termos e a contagem de termos lógicos.
     * @throws IOException Se houver um erro de leitura.
     */
    public static TermLoadResult loadSearchTermsFromFile(String termsFilePath) throws IOException {
        Map<String, String> variationToBaseMap = new LinkedHashMap<>();
        int logicalTermCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(termsFilePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                logicalTermCount++; // Conta uma linha válida como um termo lógico

                if (line.startsWith("[") && line.endsWith("]")) {
                    String content = line.substring(1, line.length() - 1).trim();
                    if (content.isEmpty()) continue;
                    String[] parts = content.split(",");
                    if (parts.length == 0) continue;
                    String baseTerm = parts[0].trim();
                    if (baseTerm.isEmpty()) {
                        System.err.println("Aviso: Grupo malformado na linha: " + line);
                        continue;
                    }
                    for (String part : parts) {
                        String variation = part.trim();
                        if (!variation.isEmpty()) {
                            variationToBaseMap.put(variation, baseTerm);
                        }
                    }
                } else {
                    variationToBaseMap.put(line, line);
                }
            }
        }
        return new TermLoadResult(variationToBaseMap, logicalTermCount);
    }

    public static void main(String[] args) {

        // --- Validação dos argumentos ---
        if (args.length != 2) {
            System.err.println("Uso: java TextSearcher <caminho_arquivo_termos> <caminho_arquivo_texto>");
            return;
        }

        if( args[0].equalsIgnoreCase(args[1]) ) {
            System.err.println("Erro: O arquivo de termos e o arquivo de texto não podem ser o mesmo.");
            return;
        }

        if (args[0].trim().isEmpty() || args[1].trim().isEmpty()) {
            System.err.println("Erro: Os caminhos dos arquivos não podem ser vazios.");
            return;
        }

        if (args[0].contains("*") || args[0].contains("?") || args[1].contains("*") || args[1].contains("?")) {
            System.err.println("Erro: Os caminhos dos arquivos não podem conter curingas (* ou ?).");
            return;
        }

        String termsFilePath = args[0];
        String textFilePath  = args[1];

        // --- Execução do algoritmo ---
        try {
            // Carrega os termos e obtém a contagem
            TermLoadResult termData = loadSearchTermsFromFile(termsFilePath);
            Map<String, String> variationToBaseMap = termData.variationToBaseMap();
            int termCount = termData.logicalTermCount();

            // Instancia o buscador
            ParserCamposLexicais searcher = new ParserCamposLexicais(variationToBaseMap);

            // Executa a busca e obtém resultados e contagem de palavras
            SearchResult searchResult = searcher.searchInFile(textFilePath);
            Map<String, Integer> occurrences = searchResult.occurrences();
            long wordCount = searchResult.totalWordCount();

            // Calcula o total de ocorrências encontradas
            long totalOccurrences = 0;
            for (int count : occurrences.values()) {
                totalOccurrences += count;
            }
            // Alternativa com Stream:
            // long totalOccurrences = occurrences.values().stream().mapToLong(Integer::longValue).sum();

            // Calcula o número de termos únicos com ocorrências > 0
            long uniqueTermsFound = occurrences.values().stream().filter(count -> count > 0).count();

            // Calcula os índices de densidade e cobertura lexical
            float idl = 0.0f;
            float icl = 0.0f;

            idl = (totalOccurrences / (float)wordCount) * 1000;
            icl = (uniqueTermsFound / (float)termCount) * 100;

            // --- Apresentação dos resultados ---
            System.out.println("============================================================================");
            System.out.println("                 Resumo da Análise");
            System.out.println("============================================================================");
            System.out.printf("Arquivo de Termos.................: %s%n", termsFilePath);
            System.out.printf("Arquivo de Texto..................: %s%n", textFilePath);
            System.out.println("============================================================================");
            System.out.printf("Termos de busca carregados........: %d%n", termCount);
            System.out.printf("Termos únicos encontrados.........: %d%n", uniqueTermsFound);
            System.out.printf("Total de palavras no texto........: %d%n", wordCount);
            System.out.printf("Total de ocorrências encontrados..: %d%n", totalOccurrences);
            System.out.println("============================================================================");
            System.out.printf("Indice de densidade lexical.......: %f por mil palavras %n", idl);
            System.out.printf("Indice de cobertura lexical.......: %f por cento %n", icl);
            System.out.println("============================================================================");

            System.out.println("--- Detalhamento de Ocorrências (Ordenado) ---");
            
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(occurrences.entrySet());
            Comparator<Map.Entry<String, Integer>> resultComparator = 
                Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey());
            sortedEntries.sort(resultComparator);
            
            for (Map.Entry<String, Integer> entry : sortedEntries) {
                 System.out.printf("'%s': %d ocorrência(s)%n", entry.getKey(), entry.getValue());
            }

        } catch (IOException e) {
            System.err.println("Ocorreu um erro durante a busca: " + e.getMessage());
        } finally {
        }
    }
}