package ca.kryptogarten.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe responsável por gerenciar a escrita no arquivo de saída do programa.
 * Permite especificar o nome do arquivo de saída no momento da criação.
 */
public class GerenciadorSaida {
    private final BufferedWriter writer;
    private final String nomeArquivoSaida;

    /**
     * Construtor para GerenciadorSaida.
     * Cria um novo arquivo de saída com o nome especificado.
     * Se o nome do arquivo for {@code null} ou vazio, um nome padrão com timestamp
     * no formato `normalizacao-YYYY-MM-DD-HH:MM:SS.txt` será gerado.
     *
     * @param nomeArquivo O nome desejado para o arquivo de saída.
     */
    public GerenciadorSaida(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.trim().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");
            String timestamp = now.format(formatter);
            this.nomeArquivoSaida = "normalizacao-" + timestamp + ".txt"; // Nome padrão se não fornecido
            System.out.println("Nenhum nome de arquivo de saída fornecido. Gerando nome padrão: " + this.nomeArquivoSaida);
        } else {
            this.nomeArquivoSaida = nomeArquivo;
        }

        try {
            writer = Files.newBufferedWriter(Paths.get(this.nomeArquivoSaida), StandardCharsets.UTF_8);
            System.out.println("Arquivo de saída criado: " + this.nomeArquivoSaida);
        } catch (IOException e) {
            System.err.println("Erro ao inicializar o arquivo de saída " + this.nomeArquivoSaida + ": " + e.getMessage());
            throw new RuntimeException("Não foi possível criar o arquivo de saída.", e);
        }
    }

    /**
     * Sobrecarga do construtor para manter compatibilidade, gerando um nome de arquivo padrão.
     * @deprecated Use {@link #GerenciadorSaida(String)} e forneça um nome de arquivo explícito.
     */
    @Deprecated
    public GerenciadorSaida() {
        this(null); // Chama o construtor principal com null para gerar um nome padrão
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