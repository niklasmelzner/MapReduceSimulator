package com.melzner.mapreduce.scenario;

import com.melzner.mapreduce.cluster.Cluster;
import com.melzner.xmlutil.MapXMLContainer;
import com.melzner.xmlutil.XMLAdapter;
import com.melzner.xmlutil.XMLElement;
import com.melzner.xmlutil.XMLList;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ScenarioConfig extends XMLAdapter {

    public static final XMLElement SIMULATION = new XMLElement("simulation");
    public static final XMLElement CLUSTER = new XMLElement("cluster", SIMULATION);
    public static final XMLElement DFS = new XMLElement("dfs", CLUSTER);
    public static final XMLElement COMPUTATION = new XMLElement("computation", SIMULATION);
    public static final XMLElement MASTER = new XMLElement("master", COMPUTATION);

    @MapXMLContainer
    public final ClusterConfig clusterConfig = new ClusterConfig();
    @MapXMLContainer
    public final List<SimpleComputationConfig> simpleComputations = new XMLList<>(SimpleComputationConfig.class,
            () -> new SimpleComputationConfig(clusterConfig),
            "simpleComputation", SIMULATION);

    private ScenarioConfig() {

    }

    public static Long parseDataSize(String value) {
        value = value.toLowerCase(Locale.ROOT);
        int length = value.length();
        if (length >= 2 && value.charAt(length - 1) == 'b') {
            long factor = 0;
            switch (value.charAt(length - 2)) {
                case 't':
                    factor = 1024L * 1024 * 1024 * 1024;
                    break;
                case 'g':
                    factor = 1024 * 1024 * 1024;
                    break;
                case 'm':
                    factor = 1024 * 1024;
                    break;
                case 'k':
                    factor = 1024;
                    break;
            }
            if (factor == 0) {
                return Long.parseLong(value.substring(0, value.length() - 1));
            } else {
                return factor * Long.parseLong(value.substring(0, value.length() - 2));
            }
        }
        return Long.parseLong(value);
    }

    public static Long parseTime(String value) {
        value = value.toLowerCase(Locale.ROOT);
        int length = value.length();
        TimeUnit timeUnit = null;
        if (length >= 2 && value.charAt(length - 1) == 's') {
            switch (value.charAt(length - 2)) {
                case 'm':
                    timeUnit = TimeUnit.MILLISECONDS;
                    break;
                case 'µ':
                    timeUnit = TimeUnit.MICROSECONDS;
                    break;
                case 'n':
                    timeUnit = TimeUnit.NANOSECONDS;
                    break;
            }
            if (timeUnit == null) {
                return TimeUnit.SECONDS.toNanos(Long.parseLong(value.substring(0, value.length() - 1)));
            } else {
                return timeUnit.toNanos(Long.parseLong(value.substring(0, value.length() - 2)));
            }
        }
        return Long.parseLong(value);
    }

    public static ScenarioConfig load(File file) throws IOException, SAXException {
        return new ScenarioConfig().parseXML(new XMLFileInput(file));
    }

    public static ScenarioConfig load(Class<?> c, String path) throws IOException, SAXException {
        return new ScenarioConfig().parseXML(new XMLClassResourceInput(c, path));
    }

    @Override
    protected void preParse(XMLInput xmlInput, Document document) throws IOException, SAXException {
        String extendsAttr = document.getDocumentElement().getAttribute("extends");
        for (String extendsName : extendsAttr.split(";")) {
            if (! extendsName.isEmpty()) {
                if (xmlInput instanceof XMLFileInput) {
                    File file = ((XMLFileInput) xmlInput).getFile();
                    File f = new File(file.getParentFile(), extendsName + ".xml");
                    if (! f.exists()) {
                        throw new RuntimeException("extends file '" + f + "' doesn't exist");
                    }
                    parseXML(new XMLFileInput(f));
                } else if (xmlInput instanceof XMLClassResourceInput) {
                    String newPath = new File(new File(((XMLClassResourceInput) xmlInput).getPath()).getParentFile(), extendsName).getPath();
                    newPath = newPath.replace(File.separatorChar, '/');
                    parseXML(new XMLClassResourceInput(((XMLClassResourceInput) xmlInput).getC(), newPath + ".xml"));
                }
            }
        }
    }

    public Cluster createCluster() {
        return new Cluster(clusterConfig);
    }

}
