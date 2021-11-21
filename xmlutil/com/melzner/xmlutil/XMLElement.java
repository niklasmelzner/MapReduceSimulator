package com.melzner.xmlutil;

public class XMLElement {

    private final String name;
    private final XMLElement parent;
    private final int depth;

    public XMLElement(String name) {
        this(name, null);
    }

    public XMLElement(String name, XMLElement parent) {
        this.name = name;
        this.parent = parent;
        depth = parent == null ? 0 : parent.depth + 1;
    }

    public String[] collectPath(String... additionalArgs) {
        String[] path = new String[depth + 1 + additionalArgs.length];
        collectPathInternal(path);
        System.arraycopy(additionalArgs, 0, path, depth + 1, additionalArgs.length);
        return path;
    }

    private void collectPathInternal(String[] path) {
        path[depth] = name;
        if (parent != null) {
            parent.collectPathInternal(path);
        }
    }
}
