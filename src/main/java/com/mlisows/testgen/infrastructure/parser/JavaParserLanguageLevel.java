package com.mlisows.testgen.infrastructure.parser;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;

public final class JavaParserLanguageLevel {
    private JavaParserLanguageLevel() {
    }

    public static void configure() {
        StaticJavaParser.setConfiguration(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }
}
