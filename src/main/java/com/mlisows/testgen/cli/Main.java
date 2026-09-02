package com.mlisows.testgen.cli;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("prepare-profile")) {
            new PrepareProfileCommand().run(args);
            return;
        }

        new GenerateTestsCommand().run(args);
    }
}
