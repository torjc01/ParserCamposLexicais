import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TextComparator {

    /**
     * Etapa 1: Pré-processa o texto.
     */
    public static List<String> preprocessText(String text) {
        String cleanedText = text.toLowerCase().replaceAll("[^a-záàâãéèêíïóôõöúçñ\\s]", "");
        return Arrays.stream(cleanedText.split("\\s+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Etapa 2: Calcula a Frequência do Termo (TF) para um único documento.
     */
    public static Map<String, Double> calculateTf(List<String> docTokens) {
        Map<String, Double> tfScores = new HashMap<>();
        int totalTerms = docTokens.size();
        Map<String, Integer> termCounts = new HashMap<>();
        for (String term : docTokens) {
            termCounts.put(term, termCounts.getOrDefault(term, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            tfScores.put(entry.getKey(), (double) entry.getValue() / totalTerms);
        }
        return tfScores;
    }

    /**
     * Etapa 3: Calcula a Frequência Inversa do Documento (IDF) para todos os termos do corpus.
     */
    public static Map<String, Double> calculateIdf(List<List<String>> corpus) {
        Map<String, Double> idfScores = new HashMap<>();
        int totalDocuments = corpus.size();
        Set<String> vocabulary = new HashSet<>();
        for (List<String> doc : corpus) {
            vocabulary.addAll(doc);
        }
        for (String term : vocabulary) {
            int docCount = 0;
            for (List<String> doc : corpus) {
                if (doc.contains(term)) {
                    docCount++;
                }
            }
            idfScores.put(term, Math.log((double) totalDocuments / docCount));
        }
        return idfScores;
    }

    /**
     * Etapa 4: Cria o vetor TF-IDF para um documento.
     */
    public static Map<String, Double> createTfIdfVector(Map<String, Double> tfScores, Map<String, Double> idfScores) {
        Map<String, Double> tfIdfVector = new HashMap<>();
        for (Map.Entry<String, Double> entry : tfScores.entrySet()) {
            String term = entry.getKey();
            double tf = entry.getValue();
            double idf = idfScores.getOrDefault(term, 0.0);
            tfIdfVector.put(term, tf * idf);
        }
        return tfIdfVector;
    }

    /**
     * Etapa 5: Calcula a Similaridade de Cosseno entre dois vetores TF-IDF.
     */
    public static double calculateCosineSimilarity(Map<String, Double> vectorA, Map<String, Double> vectorB) {
        double dotProduct = 0.0;
        Set<String> allTerms = new HashSet<>(vectorA.keySet());
        allTerms.addAll(vectorB.keySet());
        for (String term : allTerms) {
            dotProduct += vectorA.getOrDefault(term, 0.0) * vectorB.getOrDefault(term, 0.0);
        }
        double magnitudeA = 0.0;
        for (double value : vectorA.values()) {
            magnitudeA += Math.pow(value, 2);
        }
        magnitudeA = Math.sqrt(magnitudeA);
        double magnitudeB = 0.0;
        for (double value : vectorB.values()) {
            magnitudeB += Math.pow(value, 2);
        }
        magnitudeB = Math.sqrt(magnitudeB);
        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            return 0.0;
        } else {
            return dotProduct / (magnitudeA * magnitudeB);
        }
    }

    // --- NOVAS FUNÇÕES E LÓGICA PRINCIPAL ---

    /**
     * NOVO: Lê o conteúdo de um arquivo e o retorna como uma String.
     * @param filePath O caminho para o arquivo.
     * @return O conteúdo do arquivo ou null em caso de erro.
     */
    public static String readFileToString(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + filePath);
            return null;
        }
    }

    /**
     * MÉTODO PRINCIPAL MODIFICADO
     */
    public static void main(String[] args) {
        // Valida se os argumentos de linha de comando foram passados
        if (args.length < 2) {
            System.out.println("Uso: java TextComparator <path_arquivo_1> <path_arquivo_2> ... <path_arquivo_n>");
            System.out.println("Por favor, forneça pelo menos dois arquivos de texto para comparar.");
            return;
        }

        // Carrega os textos a partir dos arquivos fornecidos como argumentos
        List<String> corpusRaw = new ArrayList<>();
        System.out.println("Lendo arquivos...");
        for (String filePath : args) {
            String content = readFileToString(filePath);
            if (content != null && !content.isEmpty()) {
                corpusRaw.add(content);
                System.out.println(" - " + filePath + " carregado com sucesso.");
            } else {
                System.err.println("Arquivo '" + filePath + "' está vazio ou não pôde ser lido. Ignorando.");
            }
        }

        if (corpusRaw.size() < 2) {
            System.err.println("Não foi possível carregar pelo menos dois arquivos válidos para comparação.");
            return;
        }

        // O resto do processamento é o mesmo
        List<List<String>> corpusProcessed = new ArrayList<>();
        for (String text : corpusRaw) {
            corpusProcessed.add(preprocessText(text));
        }

        Map<String, Double> idfScores = calculateIdf(corpusProcessed);

        List<Map<String, Double>> tfIdfVectors = new ArrayList<>();
        for (List<String> docTokens : corpusProcessed) {
            Map<String, Double> tfScores = calculateTf(docTokens);
            Map<String, Double> tfIdfVector = createTfIdfVector(tfScores, idfScores);
            tfIdfVectors.add(tfIdfVector);
        }

        System.out.println("\n### Matriz de Similaridade de Cosseno ###\n");

        // Compara cada par de documentos e exibe o resultado
        for (int i = 0; i < tfIdfVectors.size(); i++) {
            for (int j = i + 1; j < tfIdfVectors.size(); j++) {
                double similarity = calculateCosineSimilarity(tfIdfVectors.get(i), tfIdfVectors.get(j));
                String fileNameA = Paths.get(args[i]).getFileName().toString();
                String fileNameB = Paths.get(args[j]).getFileName().toString();
                System.out.printf("Similaridade entre '%s' e '%s': %.4f%n", fileNameA, fileNameB, similarity);
            }
        }
    }
}