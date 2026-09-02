package com.mlisows.testgen.cli;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("profile")) {
            new ObservedProfileCommand().runProfile(args);
            return;
        }

        if (args.length > 0 && args[0].equals("prepare-profile")) {
            new ObservedProfileCommand().runPrepare(args);
            return;
        }

        new GenerateTestsCommand().run(args);
    }
}
