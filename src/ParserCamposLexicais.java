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
 * Classe responsável por analisar campos lexicais em arquivos de texto.
 * <p>
 * Utiliza o algoritmo <b>Aho-Corasick</b> para realizar a busca eficiente de múltiplos
 * padrões (palavras ou expressões) simultaneamente em uma única passagem pelo texto.
 * </p>
 */
public class ParserCamposLexicais {

    /**
     * Registro (Record) para encapsular o resultado do carregamento do arquivo de termos.
     *
     * @param mapaVariacaoBase Mapa onde a chave é a variação (ex: sinônimo) e o valor é o termo canônico.
     * @param contagemTermosLogicos O número de conceitos/termos lógicos distintos lidos do arquivo.
     */
    public record ResultadoCarregamentoTermos(Map<String, String> mapaVariacaoBase, int contagemTermosLogicos) {}

    /**
     * Registro (Record) para encapsular as estatísticas finais da busca no texto.
     *
     * @param ocorrencias Mapa contendo o termo base e a quantidade de vezes que foi encontrado.
     * @param contagemTotalPalavras O número total de palavras processadas no arquivo de texto.
     */
    public record ResultadoBusca(Map<String, Integer> ocorrencias, long contagemTotalPalavras) {}

    /**
     * Classe interna que representa um Nó na árvore (Trie) do algoritmo Aho-Corasick.
     */
    private static class No {
        // Map para os nós filhos, onde a chave é o caractere de transição
        private final Map<Character, No> filhos = new HashMap<>();

        // Link de falha: aponta para o sufixo mais longo possível que também é um prefixo de outro padrão
        private No linkFalha = null;

        // Lista de saídas: contém os termos completos encontrados ao chegar neste nó
        private final List<String> saida = new ArrayList<>();
    }

    private final No raiz;
    private final Map<String, String> mapaVariacaoBase;

    /**
     * Construtor do Parser. Inicializa a estrutura de dados e constrói o autômato.
     *
     * @param mapaVariacaoBase Mapa contendo as variações de termos apontando para seus termos base.
     * @throws IllegalArgumentException Se o mapa fornecido for nulo ou vazio.
     */
    public ParserCamposLexicais(Map<String, String> mapaVariacaoBase) {
        if (mapaVariacaoBase == null || mapaVariacaoBase.isEmpty()) {
            throw new IllegalArgumentException("O mapa de variações para termos base não pode ser nulo ou vazio.");
        }
        this.raiz = new No();
        this.mapaVariacaoBase = mapaVariacaoBase;

        // Fases de construção do algoritmo Aho-Corasick
        construirTrie();
        construirLinksDeFalha();
    }

    /**
     * Constrói a estrutura básica de árvore (Trie) com todos os termos de busca.
     */
    private void construirTrie() {
        for (String variacao : this.mapaVariacaoBase.keySet()) {
            No noAtual = this.raiz;
            String variacaoMinuscula = variacao.toLowerCase();

            // Navega ou cria nós para cada caractere do termo
            for (char c : variacaoMinuscula.toCharArray()) {
                noAtual = noAtual.filhos.computeIfAbsent(c, k -> new No());
            }
            // Ao final da palavra, adiciona o termo base correspondente à lista de saída deste nó
            noAtual.saida.add(this.mapaVariacaoBase.get(variacao));
        }
    }

    /**
     * Constrói os links de falha (failure links) usando busca em largura (BFS).
     * Isso permite que o algoritmo transite entre correspondências parciais sem retroceder no texto.
     */
    private void construirLinksDeFalha() {
        Queue<No> fila = new LinkedList<>();

        // A falha da raiz é ela mesma
        raiz.linkFalha = raiz;

        // Inicializa a fila com os filhos diretos da raiz e define seus links de falha para a raiz
        for (No filho : raiz.filhos.values()) {
            filho.linkFalha = raiz;
            fila.add(filho);
        }

        // Processamento BFS
        while (!fila.isEmpty()) {
            No noAtual = fila.poll();

            for (Map.Entry<Character, No> entrada : noAtual.filhos.entrySet()) {
                char caractere = entrada.getKey();
                No noFilho = entrada.getValue();

                fila.add(noFilho);

                // Encontra o link de falha para o nó filho
                No tempLinkFalha = noAtual.linkFalha;

                // Retrocede pelos links de falha até encontrar um nó que tenha uma transição para 'caractere'
                while (tempLinkFalha.filhos.get(caractere) == null && tempLinkFalha != raiz) {
                    tempLinkFalha = tempLinkFalha.linkFalha;
                }

                // Define o link de falha do filho
                if (tempLinkFalha.filhos.containsKey(caractere)) {
                    noFilho.linkFalha = tempLinkFalha.filhos.get(caractere);
                } else {
                    noFilho.linkFalha = raiz;
                }

                // Propaga as saídas encontradas no nó de falha para o nó atual (união de resultados)
                noFilho.saida.addAll(noFilho.linkFalha.saida);
            }
        }
    }

    /**
     * Realiza a busca no arquivo de texto especificado e coleta estatísticas.
     *
     * @param caminhoArquivo O caminho do arquivo de texto a ser analisado.
     * @return Um objeto {@link ResultadoBusca} contendo as ocorrências e contagem de palavras.
     * @throws IOException Se ocorrer erro de I/O ao ler o arquivo.
     */
    public ResultadoBusca buscarNoArquivo(String caminhoArquivo) throws IOException {
        Map<String, Integer> resultados = new HashMap<>();

        // Inicializa o mapa de resultados com 0 para todos os termos base únicos
        Set<String> termosBaseUnicos = new HashSet<>(this.mapaVariacaoBase.values());
        for (String termoBase : termosBaseUnicos) {
            resultados.put(termoBase, 0);
        }

        No noAtual = this.raiz;
        long contagemTotalPalavras = 0;

        try (BufferedReader leitor = Files.newBufferedReader(Paths.get(caminhoArquivo), StandardCharsets.UTF_8)) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                String linhaTrim = linha.trim();

                // Ignora comentários e linhas vazias
                if (linhaTrim.startsWith("#") || linhaTrim.isEmpty()) {
                    continue;
                }

                // Contagem simples de palavras baseada em espaços em branco
                String[] palavras = linhaTrim.split("\\s+");
                contagemTotalPalavras += palavras.length;

                // Processamento caractere por caractere (Aho-Corasick)
                for (char c : linha.toCharArray()) {
                    char caractere = Character.toLowerCase(c);
                    noAtual = encontrarProximoEstado(noAtual, caractere);

                    // Se houver saídas neste estado, incrementa os contadores
                    if (!noAtual.saida.isEmpty()) {
                        for (String termoBase : noAtual.saida) {
                            resultados.compute(termoBase, (k, v) -> v + 1);
                        }
                    }
                }

                // Processa uma quebra de linha explícita para garantir que termos no fim da linha sejam capturados
                // caso dependam de um delimitador invisível
                noAtual = encontrarProximoEstado(noAtual, Character.toLowerCase('\n'));
                if (!noAtual.saida.isEmpty()) {
                    for (String termoBase : noAtual.saida) {
                        resultados.compute(termoBase, (k, v) -> v + 1);
                    }
                }
            }
        }
        return new ResultadoBusca(resultados, contagemTotalPalavras);
    }

    /**
     * Determina o próximo estado no autômato dado o estado atual e um caractere de entrada.
     * Segue os links de falha se a transição direta não existir.
     *
     * @param estadoAtual O nó atual na travessia.
     * @param caractere O caractere sendo lido.
     * @return O próximo nó (estado).
     */
    private No encontrarProximoEstado(No estadoAtual, char caractere) {
        No proximoEstado = estadoAtual;
        while (proximoEstado.filhos.get(caractere) == null && proximoEstado != raiz) {
            proximoEstado = proximoEstado.linkFalha;
        }
        return proximoEstado.filhos.getOrDefault(caractere, raiz);
    }

    /**
     * Carrega e analisa o arquivo de termos de busca.
     * Suporta agrupamento de sinônimos entre colchetes, ex: [casa, lar, moradia].
     *
     * @param caminhoArquivoTermos O caminho para o arquivo de definições.
     * @return Um objeto {@link ResultadoCarregamentoTermos} com o mapa processado e contagem.
     * @throws IOException Se houver erro na leitura do arquivo.
     */
    public static ResultadoCarregamentoTermos carregarTermosDoArquivo(String caminhoArquivoTermos) throws IOException {
        Map<String, String> mapaVariacaoBase = new LinkedHashMap<>();
        int contagemTermosLogicos = 0;

        try (BufferedReader leitor = Files.newBufferedReader(Paths.get(caminhoArquivoTermos), StandardCharsets.UTF_8)) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                linha = linha.trim();

                // Ignora linhas vazias ou comentários
                if (linha.isEmpty() || linha.startsWith("#")) {
                    continue;
                }

                contagemTermosLogicos++; // Cada linha válida conta como um conceito lógico

                // Verifica se é um grupo de sinônimos: [termoBase, variacao1, variacao2]
                if (linha.startsWith("[") && linha.endsWith("]")) {
                    String conteudo = linha.substring(1, linha.length() - 1).trim();
                    if (conteudo.isEmpty()) continue;

                    String[] partes = conteudo.split(",");
                    if (partes.length == 0) continue;

                    // O primeiro item é considerado o termo canônico (base)
                    String termoBase = partes[0].trim();
                    if (termoBase.isEmpty()) {
                        System.err.println("Aviso: Grupo malformado na linha: " + linha);
                        continue;
                    }

                    // Mapeia todas as variações (incluindo a base) para o termo base
                    for (String parte : partes) {
                        String variacao = parte.trim();
                        if (!variacao.isEmpty()) {
                            mapaVariacaoBase.put(variacao, termoBase);
                        }
                    }
                } else {
                    // Termo simples (mapeia para si mesmo)
                    mapaVariacaoBase.put(linha, linha);
                }
            }
        }
        return new ResultadoCarregamentoTermos(mapaVariacaoBase, contagemTermosLogicos);
    }

    /**
     * Método principal para execução via linha de comando.
     *
     * @param args Argumentos: [0] arquivo de termos, [1] arquivo de texto.
     */
    public static void main(String[] args) {

        // --- Validação dos argumentos ---
        if (args.length != 2) {
            System.err.println("Uso: java ParserCamposLexicais <caminho_arquivo_termos> <caminho_arquivo_texto>");
            return;
        }

        if(args[0].equalsIgnoreCase(args[1])) {
            System.err.println("Erro: O arquivo de termos e o arquivo de texto não podem ser o mesmo.");
            return;
        }

        if (args[0].trim().isEmpty() || args[1].trim().isEmpty()) {
            System.err.println("Erro: Os caminhos dos arquivos não podem ser vazios.");
            return;
        }

        // Verificação simples de caracteres curinga (comum em erros de shell script)
        if (args[0].contains("*") || args[0].contains("?") || args[1].contains("*") || args[1].contains("?")) {
            System.err.println("Erro: Os caminhos dos arquivos não podem conter caracteres curinga (* ou ?).");
            return;
        }

        String caminhoArquivoTermos = args[0];
        String caminhoArquivoTexto  = args[1];

        // --- Execução do algoritmo ---
        try {
            // 1. Carrega os termos e obtém a contagem lógica
            ResultadoCarregamentoTermos dadosTermos = carregarTermosDoArquivo(caminhoArquivoTermos);
            Map<String, String> mapaVariacaoBase = dadosTermos.mapaVariacaoBase();
            int qtdTermosLogicos = dadosTermos.contagemTermosLogicos();

            // 2. Instancia o buscador com o mapa carregado
            ParserCamposLexicais buscador = new ParserCamposLexicais(mapaVariacaoBase);

            // 3. Executa a busca e obtém estatísticas brutas
            ResultadoBusca resultadoBusca = buscador.buscarNoArquivo(caminhoArquivoTexto);
            Map<String, Integer> ocorrencias = resultadoBusca.ocorrencias();
            long totalPalavrasTexto = resultadoBusca.contagemTotalPalavras();

            // 4. Processamento dos resultados
            long totalOcorrenciasEncontradas = 0;
            for (int qtd : ocorrencias.values()) {
                totalOcorrenciasEncontradas += qtd;
            }

            // Calcula quantos termos únicos (conceitos) apareceram pelo menos uma vez
            long termosUnicosEncontrados = ocorrencias.values().stream().filter(qtd -> qtd > 0).count();

            // --- Cálculo de Índices ---
            float idl = 0.0f; // Índice de Densidade Lexical
            float icl = 0.0f; // Índice de Cobertura Lexical

            // IDL: Frequência dos termos buscados a cada 1000 palavras do texto
            if (totalPalavrasTexto > 0) {
                idl = (totalOcorrenciasEncontradas / (float)totalPalavrasTexto) * 1000;
            }

            // ICL: Porcentagem dos termos do dicionário que foram encontrados no texto
            if (qtdTermosLogicos > 0) {
                icl = (termosUnicosEncontrados / (float)qtdTermosLogicos) * 100;
            }

            // --- Apresentação dos resultados ---
            System.out.println("============================================================================");
            System.out.println("                 Resumo da Análise Lexical");
            System.out.println("============================================================================");
            System.out.printf("Arquivo de Termos.................: %s%n", caminhoArquivoTermos);
            System.out.printf("Arquivo de Texto..................: %s%n", caminhoArquivoTexto);
            System.out.println("============================================================================");
            System.out.printf("Termos de busca carregados........: %d%n", qtdTermosLogicos);
            System.out.printf("Termos únicos encontrados.........: %d%n", termosUnicosEncontrados);
            System.out.printf("Total de palavras no texto........: %d%n", totalPalavrasTexto);
            System.out.printf("Total de ocorrências encontrados..: %d%n", totalOcorrenciasEncontradas);
            System.out.println("============================================================================");
            System.out.printf("Índice de Densidade Lexical (IDL).: %f por mil palavras %n", idl);
            System.out.printf("Índice de Cobertura Lexical (ICL).: %f porcento (%%) %n", icl);
            System.out.println("============================================================================");

            System.out.println("--- Detalhamento de Ocorrências (Ordenado por Frequência) ---");

            // Ordenação da lista para exibição: maior frequência primeiro, depois ordem alfabética
            List<Map.Entry<String, Integer>> entradasOrdenadas = new ArrayList<>(ocorrencias.entrySet());
            Comparator<Map.Entry<String, Integer>> comparadorResultado =
                    Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey());
            entradasOrdenadas.sort(comparadorResultado);

            for (Map.Entry<String, Integer> entrada : entradasOrdenadas) {
                System.out.printf("'%s': %d ocorrência(s)%n", entrada.getKey(), entrada.getValue());
            }

        } catch (IOException e) {
            System.err.println("Ocorreu um erro de I/O durante o processo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ocorreu um erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}