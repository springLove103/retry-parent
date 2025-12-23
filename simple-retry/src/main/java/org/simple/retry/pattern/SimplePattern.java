package org.simple.retry.pattern;


import org.simple.retry.RetryUtil;

public class SimplePattern implements Pattern {
    private static String context;
    private SimplePattern anotherPattern;

    public SimplePattern(String context) {
        SimplePattern.context = context;
    }

    public String getContext() {
        return context;
    }

    @Override
    public Boolean meetState() {
        return anotherPattern != null && context.equals(anotherPattern.getContext());
    }

    @Override
    public int escapeTime() {
        return RetryUtil.DEFAULT_ESCAPE_TIME;
    }

    public void readFormHeadersContent(String content) {
        this.anotherPattern = new SimplePattern(content);
    }
}
