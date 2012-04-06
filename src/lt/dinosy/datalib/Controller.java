package lt.dinosy.datalib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Parse data file
 *
 * @author Aurelijus Banelis
 */
public class Controller {
    private DocumentBuilderFactory factory;
    private Document document;
    private Element xmlSources;
    private Element xmlData;
    private Element xmlRepresentations;
    private Element xmlRelations;
    private static final String dinosyNS = "http://aurelijus.banelis.lt/dinosy";
    private Map<Integer, Data> data;
    private Map<Integer, Source> sources;
    private static int lastSourceId = 0;
    private static int lastDataId = 0;
    private boolean valid = true;
    private SAXException lastException;

    private Collection<Data> dataList;
    private Collection<Representation> representations;
    private Set<Source> sourcesSet;
    private Transformer transformer;
    public final static int defaultParentId = -1;
    
    public boolean openFile(String fileAddress) throws URISyntaxException, ParserConfigurationException, SAXException, IOException, BadVersionException {
        File file = new File(fileAddress);
        
        document = validate(file, Controller.class.getResourceAsStream("dinosy.xsd"));
        if (valid) {
            clear();
            checkVersions();
            parseSource();
            parseData();
            parseRelations();
            parseRepresentation();
        } else if (lastException != null) {
            debugXmlSchema();
            throw lastException;
        }
        return valid;
    }

    //FIXME: gui based error validation
    private void debugXmlSchema() {
        try {
            System.out.println("=== XML schema used ===");
            InputStream resourceAsStream = Controller.class.getResourceAsStream("dinosy.xsd");
            int b = 0;
            do {
                b = resourceAsStream.read();
                if (b > -1) {
                    System.out.print((char) b);
                }
            } while (b > -1);
            System.out.println("=== End of XML schema ===");
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, "Error dumping XML schema", ex);
        }
    }
    
    private void clear() {
        data = new HashMap<Integer, Data>();
        sources = new HashMap<Integer, Source>();
    }

    /*
     * Format
     */

    private Document validate(File dataFile, InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        /* Validator */
        if (factory == null) {
            initDocumentBuilderFactory();
        }

        /* Schema */
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        factory.setSchema(schemaFactory.newSchema(new javax.xml.transform.Source[]{new StreamSource(stream)}));
        
        /* Validating */
        valid = true;
        lastException = null;
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
                valid = false;
                lastException = exception;
            }
            public void error(SAXParseException exception) throws SAXException {
                //TODO: do without workaround
                if (!exception.getMessage().endsWith("Document is invalid: no grammar found.") && !exception.getMessage().endsWith(", must match DOCTYPE root \"null\".")) {
                    valid = false;
                    lastException = exception;
                }
            }
            public void fatalError(SAXParseException exception) throws SAXException {
                valid = false;
                lastException = exception;
            }
        });
        return builder.parse(dataFile);
    }

    private void initDocumentBuilderFactory() {
        factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
    }

    /*
     * Versions and sections
     */

    private void checkVersions() throws BadVersionException {
        NodeList nodes = document.getFirstChild().getChildNodes();
        for (int i= 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element && nodes.item(i).getNamespaceURI().equals(dinosyNS)) {
                String componentVersion = nodes.item(i).getAttributes().getNamedItem("since").getNodeValue();
                String needed = "1.1.1";
                if (checkVersion(componentVersion, needed) < 0) {
                    throw new  BadVersionException(needed, componentVersion, nodes.item(i).getNodeName());
                }
                if (getRealNodeName(nodes.item(i)).equals("sources")) {
                    xmlSources = (Element) nodes.item(i);
                } else if (getRealNodeName(nodes.item(i)).equals("data")) {
                    xmlData = (Element) nodes.item(i);
                } else if (getRealNodeName(nodes.item(i)).equals("representations")) {
                    xmlRepresentations = (Element) nodes.item(i);
                } else if (getRealNodeName(nodes.item(i)).equals("relations")) {
                    xmlRelations = (Element) nodes.item(i);
                }
            }
        }
    }

    /**
     * Return qualified name of element
     */
    public static String getRealNodeName(Node node) {
        if (node.getPrefix() != null) {
            return node.getNodeName().substring(node.getPrefix().length() + 1);
        } else {
            return node.getNodeName();
        }
    }

    private int checkVersion(String version1, String version2) {
        String[] string1 = version1.split("\\.");
        String[] string2 = version2.split("\\.");
        int[] int1 = new int[3];
        int[] int2 = new int[3];
        for (int i= 0; i < 3; i++) {
            int1[i] = (int) Integer.valueOf(string1[i]);
            int2[i] = (int) Integer.valueOf(string2[i]);
            if (int1[i] != int2[i]) {
                return int1[i] - int2[i];
            }
        }
        return 0;
    }

    public class BadVersionException extends Exception {
        public String needed;
        public String given;
        public String component;

        public BadVersionException(String needed, String given, String component) {
            this.needed = needed;
            this.given = given;
            this.component = component;
        }

        @Override
        public String getMessage() {
            return "Component " + component + " version " + given + " < " + needed;
        }
    }


    /*
     * Parsing sections
     */

    private void parseSource() {
        for (int i= 0; i < xmlSources.getChildNodes().getLength(); i++) {
            Node item = xmlSources.getChildNodes().item(i);
            if (item instanceof Element && item.getNamespaceURI().equals(dinosyNS)) {
                Source instance = Source.getInstance((Element) item);
                lastSourceId = Math.max(lastSourceId, instance.getId());
                sources.put(instance.getId(), instance);
            }
        }
        for (Source instance : sources.values()) {
            instance.resolveInheritance(sources);
        }
    }

    private void parseData() {
        for (int i= 0; i < xmlData.getChildNodes().getLength(); i++) {
            Node item = xmlData.getChildNodes().item(i);
            if (item instanceof Element && item.getNamespaceURI().equals(dinosyNS)) {
                Data instance = Data.getInstance((Element) item);
                lastDataId = Math.max(lastDataId, instance.getId());
                data.put(instance.getId(), instance);
            }
        }
        for (Data instance : data.values()) {
            instance.resolveInheritance(data);
            instance.resolveSources(sources);
        }
    }

    private void parseRelations() {
        if (xmlRelations != null) {
            for (Element element : subElements(xmlRelations)) {
                if (element.getNamespaceURI().equals(dinosyNS)) {
                    Relation relation = Relation.getInstance(element, data);
                    data.get(relation.getFrom().getId()).addRelation(relation);
                    data.get(relation.getTo().getId()).addRelation(relation);
                }
            }
        }
    }

    private void parseRepresentation() {
        for (Element element : subElements(xmlRepresentations)) {
            if (element.getNamespaceURI().equals(dinosyNS)) {
                Representation representation = Representation.getInstance(element);
                representation.resovleData(data);
                data.get(representation.getDataId()).addRepresentation(representation);
            }
        }
    }

    public Map<Integer, Data> getData() {
        return data;
    }

    public Map<Integer, Source> getSources() {
        return sources;
    }
    
//##############################################################################
    
    
    /*
     * Saving
     */

    //TODO: save all, not just part, keeping all data and all sources
    public void save(Collection<Data> data, Collection<Representation> representations, String file) throws NotUniqueIdsException, ParserConfigurationException, TransformerConfigurationException, TransformerException {
        clear();
        this.dataList = data;
        this.representations = representations;
        leaveUniqueData();
        checkIdUniquity();
        createEmptyDocument();
        prepareSources(true);
        prepareData();
        prepareRelations();
        prepareRepresentations(representations);
        transformToXML(file);
    }


    /*
     * Preparing data
     */

    private void leaveUniqueData() {
        Set<Data> dataSet = new HashSet<Data>(dataList.size());
        dataSet.addAll(dataList);

        /* Remove not selected represenations */
        for (Data d : dataSet) {
            List<Representation> iteratable = new LinkedList<Representation>(d.getRepresentations());
            for (Representation representation : iteratable) {
                if (!representations.contains(representation)) {
                    d.getRepresentations().remove(representation);
                }
            }
        }

        /* Remove not selected relations */
        for (Data d : dataSet) {
            List<Relation> toDelete = new LinkedList<Relation>();
            for (Relation relation : d.getRelations()) {
                if (relation.getFrom() == d && !dataSet.contains(relation.getTo()) ||
                    relation.getTo() == d && !dataSet.contains(relation.getFrom())) {
                    toDelete.add(relation);
                }
            }
            d.getRelations().removeAll(toDelete);
        }
        dataList = new LinkedList<Data>(dataSet);
    }

    private void checkIdUniquity() throws NotUniqueIdsException {
        List<Integer> usedIds = new ArrayList<Integer>(dataList.size());
        for (Data element : dataList) {
            if (usedIds.contains(element.getId())) {
                throw new NotUniqueIdsException(getElementFromList(element.getId()), element);
            }
        }
    }

    private Data getElementFromList(int id) {
        for (Data element : dataList) {
            if (element.getId() == id) {
                return element;
            }
        }
        return null;
    }


    /*
     * Converting to XML
     */

    private void createEmptyDocument() throws ParserConfigurationException {
        if (factory == null) {
            initDocumentBuilderFactory();
        }
        /* Root element */
        document = factory.newDocumentBuilder().newDocument();
        Element dinosy = document.createElementNS(dinosyNS, "dinosy");
        //FIXME: replace to URL
        String schemaNS = "http://www.w3.org/2001/XMLSchema-instance";
        dinosy.setAttributeNS(schemaNS, "schemaLocation", dinosyNS + " file:/home/aurelijus/Documents/DiNoSy/DataLib/src/lt/dinosy/datalib/dinosy.xsd");
        document.appendChild(dinosy);
        
//        /* Stilesheet (XSL Transformation) */
//        ProcessingInstruction pi = document.createProcessingInstruction("xml-stylesheet", "href=\"http://aurelijus.banelis.lt/dinosy/transf.xsl\" type=\"text/xsl\"");
//        dinosy.getParentNode().insertBefore(pi, dinosy);
        
        /* Sources */
        xmlSources = document.createElementNS(dinosyNS, "sources");
        xmlSources.setAttribute("since", "1.1.1");

        /* Data */
        xmlData = document.createElementNS(dinosyNS, "data");
        xmlData.setAttribute("since", "1.1.1");

        /* Relations */
        xmlRelations = document.createElementNS(dinosyNS, "relations");
        xmlRelations.setAttribute("since", "1.1.1");

        /* Representations */
        xmlRepresentations = document.createElementNS(dinosyNS, "representations");
        xmlRepresentations.setAttribute("since", "1.1.1");
    }

    private void prepareSources(boolean extractFromDataList) {
        if (extractFromDataList) {
            sourcesSet = new HashSet<Source>();
            for (Data element : dataList) {
                if (element.getSource() != null) {
                    sourcesSet.add(element.getSource());
                    if (element.getSource().getParent() != null) {
                        sourcesSet.add(element.getSource().getParent());
                    }
                }
            }
        }
        for (Source source : sourcesSet) {
            if (source instanceof Source.Project) {
                xmlSources.appendChild(source.toNode(document, dinosyNS));
            }
        }
        for (Source source : sourcesSet) {
            if (!(source instanceof Source.Project)) {
                xmlSources.appendChild(source.toNode(document, dinosyNS));
            }
        }
    }

    private void prepareData() {
        for (Data element : dataList) {
            xmlData.appendChild(element.toNode(document, dinosyNS));
        }
    }
    
    private void prepareRelations() {
        Set<Relation> relations = new HashSet<Relation>();
        for (Data element : dataList) {
            for (Relation relation : element.getRelations()) {
                relations.add(relation);
            }
        }
        for (Relation relation : relations) {
            xmlRelations.appendChild(relation.toNode(document, dinosyNS));
        }
    }
    
    private void prepareRepresentations(Collection<Representation> representations) {
        for (Representation representation : representations) {
            xmlRepresentations.appendChild(representation.toNode(document, dinosyNS));
        }
    }

    private void transformToXML(String file) throws TransformerConfigurationException, TransformerException {
        document.getDocumentElement().appendChild(xmlSources);
        document.getDocumentElement().appendChild(xmlData);
        document.getDocumentElement().appendChild(xmlRelations);
        document.getDocumentElement().appendChild(xmlRepresentations);
        if (transformer == null) {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        }
        StreamResult result = new StreamResult(new File(file));
        transformer.transform(new DOMSource(document), result);
    }
    
    public static int getNewSourceId() {
        lastSourceId++;
        return lastSourceId;
    }

    public static int getNewDataId() {
        lastDataId++;
        return lastDataId;
    }


    /*
     * Appending data
     */

    public void append(List<Data> data, String file) throws URISyntaxException, ParserConfigurationException, SAXException, IOException, BadVersionException, TransformerConfigurationException, TransformerException {
        if ((new File(file)).exists()) {
            openFile(file);
        } else {
            clear();
            createEmptyDocument();
        }
        dataList = data;
        translateIds();
        prepareSources(false);
        prepareData();
        prepareRepresentations(createPlaceHolders());
        transformToXML(file);
    }

    private void translateIds() {
        sourcesSet = new HashSet<Source>();
        int maxId = lastDataId;
        for (Data dataElement : dataList) {
            dataElement.translateId(lastDataId);
            sourcesSet.add(dataElement.getSource());
            if (dataElement.getSource().getParent() != null) {
                sourcesSet.add(dataElement.getSource().getParent());
            }
            maxId = Math.max(maxId, dataElement.getId());
        }
        lastDataId = maxId;
        maxId = lastSourceId;
        for (Source source : sourcesSet) {
            source.translateId(lastSourceId);
            maxId = Math.max(maxId, source.getId());
        }
        lastSourceId = maxId;
    }

    private List<Representation> createPlaceHolders() {
        List<Representation> representationsList = new LinkedList<Representation>();
        for (Data dataElement : dataList) {
            representationsList.add(new Representation.PlaceHolder(dataElement.getId()));
        }
        return representationsList;
    }

    /*
     * Utilietis
     */

    /*
     * Return iterator of elements.
     * Used if setIgnoringElementContentWhitespace is set to true
     */
    static Iterable<Element> subElements(final Element element) {
        int i;
        for (i = element.getChildNodes().getLength() - 1; i >= 0; i--) {
            if (element.getChildNodes().item(i) instanceof Element) {
                break;
            }
        }
        final int lastI = i;
        return new Iterable<Element>() {
            public Iterator<Element> iterator() {
                return new Iterator<Element>() {
                    private int i = 0;
                    public boolean hasNext() {
                        if (element == null || element.getChildNodes() == null) {
                            return false;
                        } else {
                            return i <= lastI;
                        }
                    }
                    public Element next() {
                        Node node = element.getChildNodes().item(i++);
                        while (!(node instanceof Element)) {
                            node = element.getChildNodes().item(i++);
                        }
                        return (Element) node;
                    }
                    public void remove() {}
                };
            }
        };
    }


    /*
     * Common section implementation
     */

    static boolean hasSuper(java.lang.Class<?> who, java.lang.Class<?> what) {
        java.lang.Class<?> parent = who.getSuperclass();
        while (parent != what && parent != Object.class) {
            parent = parent.getSuperclass();
        }
        if (parent == what) {
            return true;
        } else {
            return false;
        }
    }

    public static Map<String, Constructor<?>> getClassMap(Class<?> container) {
        HashMap<String, Constructor<?>> map = new HashMap<String, Constructor<?>>();
        for (java.lang.Class<?> class1 : container.getClasses()) {
            if (hasSuper(class1, container)) {
                try {
                    Constructor cons = class1.getConstructor(Element.class);
                    Method getTagname = class1.getMethod("getNodeName", java.lang.Class.class);
                    String tagName = (String) getTagname.invoke(null, class1);
                    map.put(tagName, cons);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(Data.class.getName()).log(Level.SEVERE, "Posibly constructor is private: " + class1.getSimpleName(), ex);
                } catch (Exception ex) {
                    Logger.getLogger(Data.class.getName()).log(Level.SEVERE, "Precompiling data types error", ex);
                }
            }
        }
        return map;
    }

    public static Object getInstance(Map<String, Constructor<?>> types, Element element) {
        Object result = null;
        try {
            Constructor constructor = types.get(getRealNodeName(element));
            if (constructor != null) {
                result = constructor.newInstance(element);
            } else {
                Logger.getLogger(Data.class.getName()).log(Level.SEVERE, "No class for type " + element.getTagName() + " found", new Exception());
            }
        } catch (Exception ex) {
            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, "Iniciating data types error", ex);
        }
        return result;
    }
}