package org.simple.example.sign;

public interface SignAlgorithm {

    String sign(String content);

    boolean verify(String content, String sign);
}
