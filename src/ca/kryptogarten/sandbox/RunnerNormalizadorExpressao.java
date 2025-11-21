package ca.kryptogarten.sandbox;

import ca.kryptogarten.preproc.NormalizadorExpressao;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RunnerNormalizadorExpressao {
    public void rodarProcesso() {
        try {
            // 1. Prepara os caminhos
            Path pathCorpus = Paths.get("documentos/corpusEclesiastico/FR-1884-HG.txt");
            Path pathLexico = Paths.get("norm-FR-CL-1884-HG.txt");

            // 2. Obtém a lista de arquivos usando o helper da própria classe
            List<Path> listaArquivos = NormalizadorExpressao.listarArquivosDoCorpus(pathCorpus);

            // 3. Instancia via new()
            NormalizadorExpressao norm = new NormalizadorExpressao(listaArquivos, pathLexico, "norm-FR-1884-HG.txt");

            // 4. Executa
            norm.executar();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        RunnerNormalizadorExpressao runner = new RunnerNormalizadorExpressao();
        runner.rodarProcesso();
    }
}