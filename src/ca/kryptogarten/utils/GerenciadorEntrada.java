package ca.kryptogarten.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GerenciadorEntrada {

    /**
     * Método utilitário estático para localizar arquivos de entrada.
     *
     * @param caminhoEntrada Caminho para um diretório ou arquivo único.
     * @return Lista de Paths contendo arquivos .txt encontrados.
     * @throws IOException Se houver erro ao acessar o sistema de arquivos.
     */
    public static List<Path> listarArquivosEntrada(Path caminhoEntrada) throws IOException {
        if (Files.isDirectory(caminhoEntrada)) {
            try (Stream<Path> stream = Files.walk(caminhoEntrada)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                        .collect(Collectors.toList());
            }
        } else if (Files.isRegularFile(caminhoEntrada)) {
            return Collections.singletonList(caminhoEntrada);
        } else {
            throw new IOException("O caminho de entrada informado não existe ou não é válido: " + caminhoEntrada);
        }
    }

}
