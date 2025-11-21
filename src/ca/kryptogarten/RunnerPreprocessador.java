package ca.kryptogarten;

import ca.kryptogarten.preproc.MarkupExpressao;
import ca.kryptogarten.preproc.NormalizadorCamposLexicais;
import ca.kryptogarten.preproc.NormalizadorExpressao;
import ca.kryptogarten.preproc.PreProcessadorTexto;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Classe Runner responsável por orquestrar o pipeline de pré-processamento
 * na ordem específica de dependência dos dados.
 * Ordem de Execução:
 * 1. MarkupExpressao: Prepara o léxico (insere underscores).
 * 2. NormalizadorExpressao: Aplica o léxico ao Corpus Bruto (protege termos compostos).
 * 3. NormalizadorCamposLexicais: Padroniza o arquivo léxico gerado na etapa 1.
 * 4. PreProcessadorTexto: Limpa o Corpus gerado na etapa 2 (preservando os underscores).
 */
public class RunnerPreprocessador {

    public static void main(String[] args) {
        // Definição de caminhos (Exemplo)
        // Entradas
        // "documentos/camposLexicais/FR-CL-1884-HG.txt";
        // "documentos/corpusEclesiastico/FR-1884-HG.txt"
        String dirLexicoBruto = "documentos/camposLexicais/FR-CL-2025-Antimaconnisme.txt"; // Diretório com arquivos .txt de termos dos campos lexicais
        String dirCorpusBruto = "documentos/corpusMidiatico/FR-1884-05-28-LeCourrierCanada.txt"; // Diretório com textos originais dos corpora

        // Arquivos Intermediários (Saídas de uma etapa, entradas da próxima)
        // "./dados/temp/lexico_markup.txt";
        // "./dados/temp/corpus_exp.txt"
        String arquivoLexicoMarkup = "documentos/preproc/camposLexicais/lexico_markup.txt";       // Saída da Etapa 1 -> Entrada da Etapa 2 e 3
        String arquivoCorpusComExpressoes = "documentos/preproc/corpus/eclesiastico/corpus_exp.txt";   // Saída da Etapa 2 -> Entrada da Etapa 4

        // Saídas Finais
        String arquivoLexicoFinal = "documentos/preproc/camposLexicais/final-lexico.txt";   // Saída da Etapa 3
        String arquivoCorpusFinal = "documentos/preproc/corpus/eclesiastico/final-corpus_preproc.txt";    // Saída da Etapa 4

        RunnerPreprocessador runner = new RunnerPreprocessador();
        runner.executarPipeline(
                dirLexicoBruto,
                arquivoLexicoMarkup,
                dirCorpusBruto,
                arquivoCorpusComExpressoes,
                arquivoLexicoFinal,
                arquivoCorpusFinal
        );
    }

    public void executarPipeline(String inputLexicoDir,
                                 String intermLexicoMarkup,
                                 String inputCorpusDir,
                                 String intermCorpusExpressoes,
                                 String outputLexicoFinal,
                                 String outputCorpusFinal) {

        System.out.println(">>> INICIANDO PIPELINE DE PRÉ-PROCESSAMENTO <<<");

        try {
            // =================================================================
            // ETAPA 1: MarkupExpressao
            // Objetivo: Ler arquivos de léxico bruto e gerar um arquivo unificado
            // com termos compostos unidos por underscore.
            // =================================================================
            System.out.println("\n--- [1/4] Markup de Expressões (Léxico) ---");
            List<Path> inputsMarkup = MarkupExpressao.listarArquivosEntrada(Paths.get(inputLexicoDir));

            if (inputsMarkup.isEmpty()) throw new IOException("Nenhum arquivo léxico encontrado em: " + inputLexicoDir);

            MarkupExpressao markup = new MarkupExpressao(inputsMarkup, intermLexicoMarkup);
            markup.executar();


            // =================================================================
            // ETAPA 2: NormalizadorExpressao
            // Objetivo: Ler o Corpus BRUTO e o Léxico Markup (Etapa 1).
            // Substitui no corpus as ocorrências dos termos por sua versão com underscore.
            // =================================================================
            System.out.println("\n--- [2/4] Normalização de Expressões no Corpus ---");
            List<Path> inputsCorpus = NormalizadorExpressao.listarArquivosDoCorpus(Paths.get(inputCorpusDir));
            Path pathLexicoMarkup = Paths.get(intermLexicoMarkup);

            if (inputsCorpus.isEmpty()) throw new IOException("Nenhum arquivo de corpus encontrado em: " + inputCorpusDir);

            // O output desta etapa é o corpus intermediário
            NormalizadorExpressao normExpressao = new NormalizadorExpressao(inputsCorpus, pathLexicoMarkup, intermCorpusExpressoes);
            normExpressao.executar();


            // =================================================================
            // ETAPA 3: NormalizadorCamposLexicais
            // Objetivo: Pegar o Léxico Markup (Etapa 1) e aplicar normalização de texto nele
            // (minúsculas, sem acentos) para gerar o dicionário final limpo.
            // =================================================================
            System.out.println("\n--- [3/4] Normalização Final dos Campos Lexicais ---");
            // A entrada é o arquivo gerado na etapa 1
            List<Path> inputLexicoParaNorm = NormalizadorCamposLexicais.listarArquivosEntrada(Paths.get(intermLexicoMarkup));

            NormalizadorCamposLexicais normLexico = new NormalizadorCamposLexicais(inputLexicoParaNorm, outputLexicoFinal);
            normLexico.executarNormalizacao();


            // =================================================================
            // ETAPA 4: PreProcessadorTexto
            // Objetivo: Pegar o Corpus Intermediário (Etapa 2) — que já tem os underscores —
            // e aplicar a limpeza final (minúsculas, acentos, pontuação).
            // CRÍTICO: Deve preservar underscores para não quebrar o trabalho da Etapa 2.
            // =================================================================
            System.out.println("\n--- [4/4] Pré-processamento Final do Corpus ---");
            // A entrada é o arquivo único gerado na etapa 2
            List<Path> inputCorpusParaFinal = PreProcessadorTexto.listarArquivosEntrada(Paths.get(intermCorpusExpressoes));

            // Configuração ESSENCIAL:
            // preservarColchetesEChaves = false (limpa tudo)
            // preservarPontuacao = false (remove pontos, vírgulas gerais)
            // preservarUnderscore = TRUE (mantém 'franco_maçonaria')
            PreProcessadorTexto.Configuracao config = new PreProcessadorTexto.Configuracao(false, false, true);

            PreProcessadorTexto preProc = new PreProcessadorTexto(inputCorpusParaFinal, config, outputCorpusFinal);
            preProc.executar();

            System.out.println("\n>>> SUCESSO: Pipeline concluído. <<<");
            System.out.println("Léxico Final: " + outputLexicoFinal);
            System.out.println("Corpus Final: " + outputCorpusFinal);

        } catch (Exception e) {
            System.err.println("\n!!! ERRO FATAL NO PIPELINE !!!");
            System.err.println("Causa: " + e.getMessage());
            e.printStackTrace();
        }
    }
}