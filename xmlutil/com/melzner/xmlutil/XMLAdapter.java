package com.melzner.xmlutil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class XMLAdapter {

    private final MappingTree mappingTree = new MappingTree(null, null);
    private boolean initializedFields;

    private void initializeFields() {
        if (! initializedFields) {
            initializedFields = true;
            forClassUntilSuper(XMLAdapter.class, c -> analyseFields(c, this, mappingTree));
        }
    }

    @SuppressWarnings("unchecked")
    private static void analyseFields(Class<?> c, Object object, MappingTree mappingTree) {
        for (Field field : c.getDeclaredFields()) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == MapXML.class) {
                    String attrName = ((MapXML) annotation).value();
                    field.setAccessible(true);

                    XMLValue<?> value = (XMLValue<?>) getFieldValue(field, object);
                    Object def = value.get();
                    Class<?> valueClass = def.getClass();
                    XMLElement valueParent = value.getParent();
                    String[] path = valueParent != null ? valueParent.collectPath(attrName) : new String[]{attrName};
                    value.setName(attrName);
                    if (value.customTransformation != null) {
                        mappingTree.insert(path, value::applyCustomTransformation);
                    } else if (valueClass == Integer.class) {
                        mappingTree.insert(path, v -> ((XMLValue<Integer>) value).set(Integer.parseInt(v)));
                    } else if (valueClass == Double.class) {
                        mappingTree.insert(path, v -> ((XMLValue<Double>) value).set(Double.parseDouble(v)));
                    } else if (valueClass == Long.class) {
                        mappingTree.insert(path, v -> ((XMLValue<Long>) value).set(Long.parseLong(v)));
                    } else if (valueClass == String.class) {
                        mappingTree.insert(path, ((XMLValue<String>) value)::set);
                    }
                } else if (annotation.annotationType() == MapXMLContainer.class) {
                    Object fieldValue = getFieldValue(field, object);

                    if (fieldValue instanceof XMLList) {
                        XMLList<?> list = (XMLList<?>) fieldValue;
                        String[] path = list.getParent().collectPath(list.getName());
                        mappingTree.insertList(path, list);
                    } else {
                        analyseFields(field.getType(), fieldValue, mappingTree);
                    }
                }
            }
        }
    }

    private static Object getFieldValue(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends XMLAdapter> T parseXML(XMLInput xmlInput) throws IOException, SAXException {

        load(xmlInput.load(), xmlInput);
        return (T) this;
    }

    private void forClassUntilSuper(Class<?> superClass, Consumer<Class<?>> c) {
        Class<?> current = getClass();
        while (! Objects.equals(current, superClass)) {
            c.accept(current);
            current = current.getSuperclass();
        }
    }

    private void load(Document document, XMLInput xmlInput) throws IOException, SAXException {
        initializeFields();
        preParse(xmlInput, document);
        mappingTree.mapRoot(document.getDocumentElement());
    }

    protected void preParse(XMLInput xmlInput, Document document) throws IOException, SAXException {

    }

    private static DocumentBuilder createDocumentBuilder() {
        DocumentBuilder db;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return db;
    }


    private static class MappingTree {

        private final String name;
        private final MappingTree parent;
        private final Map<String, MappingTree> children = new HashMap<>();
        private final List<Node> defaultListNodes = new ArrayList<>();
        private Map<String, ListObject> namedListObjects;
        private List<ListObject> unnamedListObjects;
        private Function<String, XMLValue<?>> mapping;
        private XMLList<?> list;

        private MappingTree(String name, MappingTree parent) {
            this.name = name;
            this.parent = parent;
        }

        private void insert(String[] path, Function<String, XMLValue<?>> mapping) {
            insert(path, 0, mapping);
        }

        private void insertList(String[] path, XMLList<?> list) {
            insertList(path, 0, list);
        }

        private void insert(String[] path, int index, Function<String, XMLValue<?>> mapping) {
            if (index >= path.length) {
                this.mapping = mapping;
            } else {
                String name = path[index];
                children.computeIfAbsent(name, n -> new MappingTree(name, this))
                        .insert(path, index + 1, mapping);
            }
        }

        private void insertList(String[] path, int index, XMLList<?> list) {
            if (index >= path.length) {
                this.list = list;
                namedListObjects = new HashMap<>();
                unnamedListObjects = new ArrayList<>();
            } else {
                String name = path[index];
                children.computeIfAbsent(name, n -> new MappingTree(name, this))
                        .insertList(path, index + 1, list);
            }
        }

        public MappingTree mapRoot(Node node) {
            map(node, new NodeList() {
                @Override
                public Node item(int index) {
                    return node;
                }

                @Override
                public int getLength() {
                    return 1;
                }
            });
            return this;
        }

        private MappingTree map(Node node, NodeList childNodes) {
            if (list != null) {
                Node idAttr = node.getAttributes().getNamedItem("id");
                String id = idAttr == null ? null : idAttr.getNodeValue();
                if (Objects.equals(id, "{all}")) {
                    defaultListNodes.add(node);
                    for (ListObject listObject : namedListObjects.values()) {
                        listObject.mappingTree.map(node, node.getChildNodes());
                    }
                    for (ListObject listObject : unnamedListObjects) {
                        listObject.mappingTree.map(node, node.getChildNodes());
                    }
                } else {
                    ListObject listObject;
                    if (id != null) {
                        listObject = namedListObjects.computeIfAbsent(id, i -> {
                            ListObject lo = createNewListObject();
                            mapDefaultListNodes(lo);
                            return lo;
                        });
                    } else {
                        listObject = createNewListObject();
                        unnamedListObjects.add(listObject);
                        mapDefaultListNodes(listObject);
                    }
                    listObject.mappingTree.map(node, node.getChildNodes());
                }

                return this;
            }
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                MappingTree child = children.get(childNode.getNodeName());
                if (child != null) {
                    child.map(childNode, childNode.getChildNodes());
                } else if (! childNode.getNodeName().startsWith("#")) {
                    throw new RuntimeException("No XML Mapping defined for Attribute " + getPath() + ">" + childNode.getNodeName());
                }
            }
            if (mapping != null) {
                Node randomMin = node.getAttributes().getNamedItem("randomMin");
                Node randomMax = node.getAttributes().getNamedItem("randomMax");
                try {
                    XMLValue<?> value = mapping.apply(node.getTextContent());
                    if ((randomMin != null || randomMax != null) && value != null) {
                        double rMin = randomMin == null ? 1.0 : Double.parseDouble(randomMin.getNodeValue());
                        double rMax = randomMax == null ? 1.0 : Double.parseDouble(randomMax.getNodeValue());
                        value.applyRandom(rMin, rMax);
                    }

                } catch (RuntimeException e) {
                    throw new RuntimeException(getPath(), e);
                }
            }
            return this;
        }

        private void mapDefaultListNodes(ListObject listObject) {
            for (Node node : defaultListNodes) {
                listObject.mappingTree.map(node, node.getChildNodes());
            }
        }

        private ListObject createNewListObject() {
            ListObject listObject = new ListObject(list.addNewObject(), new MappingTree("[list]", this));
            analyseFields(list.getC(), listObject.object, listObject.mappingTree);
            return listObject;
        }

        private void printTree(String prefix) {
            System.out.println(prefix + name);
            for (MappingTree child : children.values()) {
                child.printTree(prefix + "    ");
            }
        }

        private String getPath() {
            return parent.parent == null ? name : parent.getPath() + ">" + name;
        }

        private static class ListObject {
            private final Object object;
            private final MappingTree mappingTree;

            ListObject(Object object, MappingTree mappingTree) {

                this.object = object;
                this.mappingTree = mappingTree;
            }

        }
    }

    public interface XMLInput {

        Document load() throws IOException, SAXException;

    }

    public static class XMLFileInput implements XMLInput {

        private final File file;

        public XMLFileInput(File file) {
            this.file = file;
        }

        @Override
        public Document load() throws IOException, SAXException {
            return createDocumentBuilder().parse(file);
        }

        public File getFile() {
            return file;
        }
    }

    public static class XMLClassResourceInput implements XMLInput {

        private final Class<?> c;
        private final String path;

        public XMLClassResourceInput(Class<?> c, String path) {
            this.c = c;
            this.path = path;
        }

        @Override
        public Document load() throws IOException, SAXException {
            return createDocumentBuilder().parse(c.getResourceAsStream(path));
        }

        public Class<?> getC() {
            return c;
        }

        public String getPath() {
            return path;
        }
    }

}
