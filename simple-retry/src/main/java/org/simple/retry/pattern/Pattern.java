package org.simple.retry.pattern;

public interface Pattern {

    Boolean meetState();

    void readFormHeadersContent(String content);

    int escapeTime();

}
