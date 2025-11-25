package ca.kryptogarten.preproc;

import java.util.*;
import java.util.stream.Collectors;

public class StopWords {

    // Usamos HashSet para garantir busca rápida (O(1))
    private static final Set<String> STOP_WORDS_SET;

    static {
        String[] words = {
                // --- 1. Artigos e Preposições Básicas ---
                "le", "la", "les", "l", "un", "une", "des", "du", "de", "d",
                "au", "aux", "à", "a", "en", "y", "par", "pour", "avec", "sans",
                "sur", "sous", "dans", "vers", "chez", "entre",

                // --- 2. Pronomes (Incluso Plural Majestático 'Nous' e tratamento 'Vous') ---
                "je", "me", "m", "moi",
                "tu", "te", "t", "toi",
                "il", "elle", "on", "le", "la", "se", "s", "soi", "lui",
                "nous", "nôtre", "nôtres", // Muito frequente em Bulas (o Papa falando)
                "vous", "vôtre", "vôtres", // Destinatários
                "ils", "elles", "eux", "leur", "leurs",
                "ce", "cet", "cette", "ces", "celui", "celle", "ceux", "celles",
                "celui-ci", "celle-ci", "ceci", "cela", "ça",

                // --- 3. Conjunções e Relativos ---
                "et", "ou", "ni", "car", "donc", "or", "mais",
                "que", "qui", "quoi", "dont", "où", "qu", "quand", "comment",
                "comme", "si", "puisque", "lorsque", "quoique",

                // --- 4. Termos Jurídicos/Eclesiásticos de Ligação (Referenciais) ---
                "ledit", "ladite", "lesdits", "lesdites", "dudit", "desdits",
                "susdit", "susdite", "susdits", "susdites", // "Supracitado"
                "audit", "auxdits",
                "icelui", "icelle", "iceux", "icelles", // Arcaísmo para "este/esta"
                "lequel", "laquelle", "lesquels", "lesquelles", "duquel", "desquels",
                "ci-dessus", "ci-après", "ici", "là", "item",

                // --- 5. VERBOS EXPANDIDOS (Auxiliares e de Estado) ---

                // Être (Ser/Estar) - Todas as formas comuns
                "être", "étant", "été",
                "suis", "es", "est", "sommes", "êtes", "sont", // Presente
                "étais", "était", "étions", "étiez", "étaient", // Imperfeito
                "fus", "fut", "fûmes", "fûtes", "furent",       // Passé Simple (Muito comum em história)
                "serai", "sera", "serons", "serez", "seront",   // Futuro
                "sois", "soit", "soyons", "soyez", "soient",    // Subjuntivo (Comum em decretos)
                "serais", "serait", "serions", "seriez", "seraient", // Condicional

                // Avoir (Ter/Haver) - Auxiliar universal
                "avoir", "ayant", "eu", "eue", "eus", "eues",
                "ai", "as", "a", "avons", "avez", "ont",        // Presente
                "avais", "avait", "avions", "aviez", "avaient", // Imperfeito
                "eut", "eûmes", "eûtes", "eurent",              // Passé Simple
                "aurai", "aura", "aurons", "aurez", "auront",   // Futuro
                "aie", "ait", "ayons", "ayez", "aient",         // Subjuntivo
                "aurais", "aurait", "aurions", "auriez", "auraient", // Condicional

                // Faire (Fazer) - Comum em "Faire savoir", "Faire part"
                "faire", "faisant", "fait", "faite", "faits", "faites",
                "fais", "fait", "faisons", "faites", "font",
                "faisais", "faisait", "faisions", "faisiez", "faisaient",
                "fis", "fit", "fîmes", "fîtes", "firent",       // Passé Simple
                "ferai", "fera", "ferons", "ferez", "feront",
                "fasse", "fasses", "fassions", "fassiez", "fassent",

                // Devoir (Dever) - Obrigação canônica
                "devoir", "devant", "dû", "due", "dus", "dues",
                "dois", "doit", "devons", "devez", "doivent",
                "devais", "devait", "devions", "deviez", "devaient",
                "dut", "dûmes", "dûtes", "durent",              // Passé Simple
                "devrai", "devra", "devrons", "devrez", "devront",
                "doive", "doives", "doivions", "doiviez", "doivent",

                // Pouvoir (Poder) - Autoridade/Possibilidade
                "pouvoir", "pouvant", "pu",
                "peux", "puis", "peut", "pouvons", "pouvez", "peuvent",
                "pouvais", "pouvait", "pouvions", "pouviez", "pouvaient",
                "pus", "put", "pûmes", "pûtes", "purent",       // Passé Simple
                "pourrai", "pourra", "pourrons", "pourrez", "pourront",
                "puisse", "puisses", "puissions", "puissiez", "puissent",

                // Vouloir (Querer) - "Nous voulons" (Decretamos/Queremos)
                "vouloir", "voulant", "voulu",
                "veux", "veut", "voulons", "voulez", "veulent",
                "voulais", "voulait", "voulions", "vouliez", "voulaient",
                "voulut", "voulûmes", "voulûtes", "voulurent",  // Passé Simple
                "voudrai", "voudra", "voudrons", "voudrez", "voudront",
                "veuille", "veuilles", "veuilions", "veuiliez", "veuillent",

                // Aller (Ir) - Auxiliar de futuro próximo (menos comum, mas possível)
                "aller", "allant", "allé", "allée", "allés", "allées",
                "vais", "vas", "va", "allons", "allez", "vont",
                "allais", "allait", "allions", "alliez", "allaient",
                "allai", "allas", "alla", "allâmes", "allâtes", "allèrent", // Passé Simple
                "irai", "iras", "ira", "irons", "irez", "iront",
                "aille", "ailles", "aillent"
        };

        // Inicializa o Set convertendo tudo para minúsculas para padronização
        STOP_WORDS_SET = Arrays.stream(words)
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(HashSet::new));
    }

    // Partículas de elisão francesas
    private static final Set<String> PARTICULAS_ELISAO = new HashSet<>(Arrays.asList(
            "l", "d", "j", "m", "n", "s", "t", "c", "qu", "puisqu", "lorsqu"
    ));

    /**
     * Verifica se uma palavra é uma stop word.
     * @param word A palavra a ser verificada (será normalizada para lowercase).
     * @return true se for stop word, false caso contrário.
     */
    public static boolean isStopWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return true; // Considera vazio como stop word/ignóravel
        }
        // Remove pontuação simples se a palavra vier "suja" (ex: "l'église")
        // Em uma pipeline real, a tokenização deve ocorrer ANTES de chamar este método.
        return STOP_WORDS_SET.contains(word.toLowerCase().trim());
    }

    /**
     * Retorna o conjunto completo caso precise adicionar mais palavras dinamicamente.
     */
    public static Set<String> getStopWordsSet() {
        return new HashSet<>(STOP_WORDS_SET); // Retorna uma cópia para proteção
    }

    /**
     * Processa uma linha de texto e a retorna sem as palavras que sejam match par stop words. Faz a tokenização de
     * forma inteligente com regex, e substitui o apóstrofo tipográfico pelo comum.
     * @param textoBruto linha de texto que será processada
     * @return Lista com as palavras uteis
     */
    public static List<String> processarTexto(String textoBruto) {
        List<String> tokensUteis = new ArrayList<>();

        // 1. NORMALIZAÇÃO
        // Substitui o apóstrofo tipográfico (’) pelo padrão (') para evitar confusão
        // Converte para minúsculas
        String textoNormalizado = textoBruto.toLowerCase().replace('’', '\'');

        // 2. TOKENIZAÇÃO INTELIGENTE (REGEX)
        // Explicação da Regex "[^\\p{L}]+" :
        // Quebra o texto sempre que encontrar algo que NÃO seja uma letra (\p{L}).
        // Isso inclui espaços, pontuação (.,;:) e, crucialmente, o apóstrofo (').
        // Assim, "l'église" vira dois tokens: "l" e "église".
        //String[] tokens = textoNormalizado.split("[^\\p{L}]+");
        String[] tokens = textoNormalizado.split(" ");

        for (String token : tokens) {
            // Ignora tokens vazios criados por múltiplos espaços
            if (token.isEmpty()) continue;

            // 3. FILTRAGEM
            // Verifica se é stop word normal (ex: "le") OU partícula de elisão (ex: "l")
            if (StopWords.isStopWord(token) || PARTICULAS_ELISAO.contains(token)) {
                continue; // É lixo, ignora
            }

            // Se chegou aqui, é uma palavra útil (ex: "église", "pape", "dieu")
            tokensUteis.add(token);
        }
        return tokensUteis;
    }




    // Exemplo de uso
    public static void main(String[] args) {
        String textoExemplo = "Nous, Pape François, ordonnons que ledit document soit lu.";

        // Tokenização simples por espaço (para demonstração)
        String[] tokens = textoExemplo.split("\\s+");

        System.out.println("Texto Original: " + textoExemplo);
        System.out.print("Texto Filtrado: ");

        for (String token : tokens) {
            // Remove pontuação básica para checagem (vírgula, ponto)
            String limpo = token.replaceAll("[^a-zA-Zà-üÀ-Ü]", "");

            if (!isStopWord(limpo)) {
                System.out.print(limpo + " ");
            }
        }
        // Saída esperada: Pape François ordonnons document lu
        // ("Nous", "que", "ledit", "soit" foram removidos)
    }
}