package com.melzner.xmlutil;

import java.util.ArrayList;
import java.util.function.Supplier;

public class XMLList<T> extends ArrayList<T> {

    private final Class<T> c;
    private final Supplier<T> constructor;
    private final String name;
    private final XMLElement parent;

    public XMLList(Class<T> c, Supplier<T> constructor, String name, XMLElement parent) {
        this.c = c;
        this.constructor = constructor;
        this.name = name;
        this.parent = parent;
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    Class<T> getC() {
        return c;
    }

    XMLElement getParent() {
        return parent;
    }

    String getName() {
        return name;
    }

    T addNewObject() {
        T t = constructor.get();
        super.add(t);
        return t;
    }
}
