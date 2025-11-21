package ca.kryptogarten.sandbox;

import ca.kryptogarten.preproc.MarkupExpressao;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RunnerMarkupExpressao {
    public void executarEtapaMarkup() {
        try {
            Path diretorioLexicalBruto = Paths.get("documentos/camposLexicais/FR-CL-1884-HG.txt");

            // 1. Usa o helper estático
            List<Path> inputs = MarkupExpressao.listarArquivosEntrada(diretorioLexicalBruto);

            // 2. Instancia
            MarkupExpressao markup = new MarkupExpressao(inputs, "norm-FR-CL-1884-HG.txt");

            // 3. Executa
            markup.executar();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        RunnerMarkupExpressao runner = new RunnerMarkupExpressao();
        runner.executarEtapaMarkup();
    }
}
