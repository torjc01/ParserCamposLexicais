package ca.kryptogarten.preproc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe responsável por gerenciar a escrita no arquivo de saída do programa.
 * Gera um nome de arquivo único com base em um timestamp e garante que o
 * arquivo seja aberto e fechado corretamente.
 */
class GerenciadorSaida {
    private final BufferedWriter writer;
    private final String nomeArquivoSaida;

    /**
     * Construtor para GerenciadorSaida.
     * Cria um novo arquivo de saída com um nome único no formato
     * `normalizacao-YYYY-MM-DD-HH:MM:SS.txt`.
     *
     */
    public GerenciadorSaida() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");
        String timestamp = now.format(formatter);
        nomeArquivoSaida = "documentos/preproc/normalizacao-" + timestamp + ".txt";

        try {
            // Usa Files.newBufferedWriter para criar um BufferedWriter com UTF-8
            // e garante que o arquivo é criado ou sobrescrito se já existir.
            writer = Files.newBufferedWriter(Paths.get(nomeArquivoSaida), StandardCharsets.UTF_8);
            System.out.println("Arquivo de saída criado: " + nomeArquivoSaida);
        } catch (IOException e) {
            System.err.println("Erro ao inicializar o arquivo de saída " + nomeArquivoSaida + ": " + e.getMessage());
            throw new RuntimeException("Não foi possível criar o arquivo de saída.", e);
        }
    }

    /**
     * Escreve uma linha de texto no arquivo de saída, seguida por uma quebra de linha.
     *
     * @param linha A string a ser escrita.
     * @throws IOException Se ocorrer um erro de E/S ao escrever no arquivo.
     */
    public void escreverLinha(String linha) throws IOException {
        writer.write(linha);
        writer.newLine(); // Adiciona uma quebra de linha após cada linha
    }

    /**
     * Fecha o BufferedWriter, liberando os recursos associados ao arquivo de saída.
     * É crucial chamar este metodo ao final da execução do programa para garantir
     * que todos os dados sejam gravados e os recursos liberados.
     *
     * @throws IOException Se ocorrer um erro de E/S ao fechar o arquivo.
     */
    public void fechar() throws IOException {
        if (writer != null) {
            writer.close();
            System.out.println("Arquivo de saída " + nomeArquivoSaida + " fechado.");
        }
    }

    /**
     * Retorna o nome do arquivo de saída que foi criado por esta instância do gerenciador.
     *
     * @return O nome do arquivo de saída.
     */
    public String getNomeArquivoSaida() {
        return nomeArquivoSaida;
    }
}