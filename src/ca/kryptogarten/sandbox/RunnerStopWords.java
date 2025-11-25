package ca.kryptogarten.sandbox;

import ca.kryptogarten.preproc.StopWords;

public class RunnerStopWords {

    public static void main(String[] args) {


        String frase = "Consultez la messagerie de Planitou pour le découvrir dès maintenant! Me connecter à Planitou. Pour toute question, communiquez avec votre milieu de garde.";

        System.out.println(StopWords.processarTexto(frase).toString());
    }
}
