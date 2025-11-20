package ca.kryptogarten.preproc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MarkupExpressao {

    private static final String COMMENT_PREFIX = "#";
    private static final String DEFAULT_OUTPUT_FILE = "norm_corpus_expressao.txt";


    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("Uso: MarkupExpressao <campo_lexical_original.txt>");
            System.exit(1);
        }

        String caminhoCampoLexical = args[0];

        System.out.println("Caminho do lexical original:" + caminhoCampoLexical);

        //Lista de arquivos de campos lexicos
        List<Path> arquivosCampoLexical = new ArrayList<>();
        Path entradaCampoLexical = Paths.get(caminhoCampoLexical);

        if(Files.isDirectory(entradaCampoLexical)) {
            try (Stream<Path> stream = Files.walk(entradaCampoLexical)) {
                arquivosCampoLexical = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                System.err.println("Erro ao listar campos lexicais: " + e.getMessage());
                System.exit(1);
            }
        }else {
            if (Files.isRegularFile(entradaCampoLexical)) {
                arquivosCampoLexical.add(entradaCampoLexical);
            } else {
                System.err.println("Arquivo de campos lexicais inválido: " + caminhoCampoLexical);
                System.exit(1);
            }
        }

        if(arquivosCampoLexical.isEmpty()) {
            System.err.println("Nenhum arquivo de texto no caminho dos campos lexicais.");
            System.exit(1);
        }
    }
}
