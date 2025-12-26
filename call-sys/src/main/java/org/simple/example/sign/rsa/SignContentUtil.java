package org.simple.example.sign.rsa;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SignContentUtil {

    public static String build(Map<String, String> params) {
        return new TreeMap<>(params).entrySet()
                .stream()
                .filter(e ->
                        !"sign".equals(e.getKey())
                                && !"sign_type".equals(e.getKey())
                                && e.getValue() != null
                                && !e.getValue().isEmpty()
                )
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }
}
