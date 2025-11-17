package ca.kryptogarten.sandbox;

import ca.kryptogarten.utils.FileUtils;

public class Testador {

    public static void main(String[] args) {
        String file1 = args[0];

        System.out.println(FileUtils.readFileToString(file1));
    }
}
