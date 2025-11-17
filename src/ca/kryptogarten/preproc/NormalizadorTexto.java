package ca.kryptogarten.preproc;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Classe utilitária responsável por aplicar as operações de normalização
 * em strings de texto. Isso inclui remoção de pontuação, remoção de diacríticos
 * e conversão para minúsculas.
 */

class NormalizadorTexto {

    // Padrão de regex para remover TODA a pontuação
    private static final Pattern PADRAO_PONTUACAO_TOTAL = Pattern.compile("\\p{Punct}+");
    // Padrão de regex para remover pontuação, EXCLUINDO colchetes [] e chaves {}
    // Isso significa: "qualquer caractere de pontuação que NÃO seja [ ou ] ou { ou }"
    private static final Pattern PADRAO_PONTUACAO_EXCETO_COLCHETES_CHAVES = Pattern.compile("[\\p{Punct}&&[^\\[\\]{}]]+");


    /**
     * Normaliza uma string de texto aplicando as seguintes transformações:
     * <ol>
     *     <li>Remove sinais de pontuação (com opção de preservar colchetes e chaves).</li>
     *     <li>Remove todos os sinais diacríticos das letras (ex: 'á' -> 'a').</li>
     *     <li>Converte todas as letras para minúsculas.</li>
     * </ol>
     *
     * @param texto                      A string de texto a ser normalizada.
     * @param preservarColchetesEChaves Se true, colchetes '[' ']' e chaves '{' '}'
     *                                   não serão removidos da pontuação.
     * @return A string de texto normalizada.
     */
    public static String normalizar(String texto, boolean preservarColchetesEChaves, boolean preservarPontuacao) {
        String textoPontuacao;
        if (preservarColchetesEChaves) {
            // Usa o padrão que exclui colchetes e chaves
            textoPontuacao = PADRAO_PONTUACAO_EXCETO_COLCHETES_CHAVES.matcher(texto).replaceAll(" ");
        } else if (preservarPontuacao) {
            textoPontuacao = texto;
        } else {
            // Remove toda a pontuação
            textoPontuacao = PADRAO_PONTUACAO_TOTAL.matcher(texto).replaceAll(" ");
        }

        // 2. Remover todos os sinais diacríticos
        // Normaliza a string para a forma NFD (Normalization Form D), que separa o caractere base do seu diacrítico.
        // Em seguida, remove todos os caracteres que são marcas de combinação (diacríticos) usando \p{M}.
        String textoDiacriticos = Normalizer.normalize(textoPontuacao, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // 3. Converter todas as letras em minúscula
        // Usa Locale.ROOT para garantir que a conversão para minúsculas seja consistente
        // em diferentes ambientes, independentemente do locale padrão do sistema.

        return textoDiacriticos.toLowerCase(Locale.ROOT);
    }

    /**
     * Sobrecarga do metodo normalizar para compatibilidade com o comportamento original.
     * Remove toda a pontuação por padrão.
     *
     * @param texto A string de texto a ser normalizada.
     * @return A string de texto normalizada.
     * @deprecated Use {@link #normalizar(String, boolean)} para controlar a preservação de colchetes e chaves.
     */
    @Deprecated
    public static String normalizar(String texto) {
        return normalizar(texto, false, false); // Comportamento padrão: não preservar colchetes/chaves
    }
}
