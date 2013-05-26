package lt.dinosy.datalib;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static lt.dinosy.datalib.Controller.subElements;
import static lt.dinosy.datalib.Controller.getRealNodeName;

/**
 *
 *
 * @author Aurelijus Banelis
 * @todo Serialize all
 * @todo cloning for images
 */
public abstract class Data implements Serializable, Cloneable {

    private int id;
    private int parentId;
    private Data parent;
    private List<Data> childs = new LinkedList<Data>();
    private List<Relation> relations = new LinkedList<Relation>();
    private List<Representation> representations = new LinkedList<Representation>();
    private String originalDataFile = null;
    private int sourceId;
    private Source source;

    /*
     * Generic data element
     */
    private Data(Element element) {
        id = Integer.valueOf(element.getAttribute("id"));
        parentId = Controller.defaultParentId;
        if (element.getAttribute("parentId").length() > 0) {
            parentId = Integer.valueOf(element.getAttribute("parentId"));
        }
        sourceId = Integer.parseInt(element.getAttribute("sourceId"));
    }

    private Data(Source source) {
        this.source = source;
        this.sourceId = source.getId();
        this.id = Controller.getNewDataId();
        this.parentId = Controller.defaultParentId;
    }

    public void resolveInheritance(Map<Integer, Data> map) {
        for (Data instance : map.values()) {
            if (instance.getParentId() == id) {
                childs.add(instance);
                instance.setParent(this);
            }
        }
    }

    void setParent(Data data) {
        this.parent = data;
    }

    void translateId(int length) {
        id += length;
        for (Data data : childs) {
            data.translateId(length);
        }
        for (Representation representation : representations) {
            representation.translateDataId(length);
        }
    }

    protected void setSource(Source source) {
        if (sourceId == source.getId()) {
            this.source = source;
        }
    }

    public void resolveSources(Map<Integer, Source> map) {
        source = map.get(sourceId);
    }

    public List<Data> getChilds() {
        return childs;
    }

    public int getId() {
        return id;
    }

    private int getParentId() {
        return parentId;
    }

    public Data getParent() {
        return parent;
    }

    public Source getSource() {
        return source;
    }

    private int getSourceId() {
        if (getSource() != null) {
            return getSource().getId();
        } else {
            return sourceId;
        }
    }

    public void addRelation(Relation relation) {
        relations.add(relation);
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public void addRepresentation(Representation representation) {
        representations.add(representation);
    }

    public List<Representation> getRepresentations() {
        return representations;
    }

    public boolean removeRepresentation(Representation representation) {
        return representations.remove(representation);
    }

    /**
     * Exports data attributes to XML Element
     */
    public final Element toNode(Document document, String nameSpace) {
        Element element = document.createElementNS(nameSpace, getNodeName(this.getClass()));
        element.setAttribute("id", String.valueOf(getId()));
        if (getParent() != null) {
            element.setAttribute("parentId", String.valueOf(getParent().getId()));
        }
        element.setAttribute("sourceId", String.valueOf(getSourceId()));
        toNode(element, document, nameSpace);
        return element;
    }

    public static String getNodeName(java.lang.Class<? extends Data> classObject) {
        return classObject.getSimpleName().toLowerCase();
    }

    public String getDataFile() {
        if (new File(getData()).exists() || getData().startsWith("zip://")) {
            return getData();
        } else {
            return null;
        }
    }

    public abstract String getData();

    public abstract void setDataFile(String file);

    public abstract void restoreDataFile();

    protected abstract void toNode(Element element, Document document, String nameSpace);

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + getData();
    }

    /**
     * Shallow clone!
     */
    protected Data shallowClone() throws CloneNotSupportedException {
        return (Data) super.clone();
    }

    /*
     * Types of data
     */
    public static class Plain extends Data {

        private String data;
        private String oldDataFile;

        public Plain(Element element) {
            super(element);
            data = element.getTextContent();
        }

        public Plain(String data, Source source) {
            super(source);
            this.data = data;
        }

        @Override
        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            element.appendChild(document.createTextNode(getData()));
        }

        @Override
        public void setDataFile(String file) {
            oldDataFile = data;
            data = file;
        }

        @Override
        public void restoreDataFile() {
            data = oldDataFile;
        }
    };

    public static class Class extends Data {

        private String name;
        private List<String> extending;
        private List<String> implementing;
        private List<String> attributes;
        private List<String> methods;

        public Class(Element element) {
            super(element);
            this.extending = new LinkedList<String>();
            this.implementing = new LinkedList<String>();
            this.attributes = new LinkedList<String>();
            this.methods = new LinkedList<String>();
            for (Element subElement : subElements(element)) {
                if (getRealNodeName(subElement).equals("name")) {
                    this.name = subElement.getTextContent();
                } else if (getRealNodeName(subElement).equals("extend")) {
                    this.extending.add(subElement.getTextContent());
                } else if (getRealNodeName(subElement).equals("implement")) {
                    this.implementing.add(subElement.getTextContent());
                } else if (getRealNodeName(subElement).equals("attribute")) {
                    this.attributes.add(subElement.getTextContent());
                } else if (getRealNodeName(subElement).equals("method")) {
                    this.methods.add(subElement.getTextContent());
                }
            }
        }

        public Class(Source source, String name, List<String> extending, List<String> implementing, List<String> attributes, List<String> methods) {
            super(source);
            this.name = name;
            this.extending = extending;
            this.implementing = implementing;
            this.attributes = attributes;
            this.methods = methods;
        }

        public List<String> getExtending() {
            return extending;
        }

        public List<String> getImplementing() {
            return implementing;
        }

        public List<String> getAttributes() {
            return attributes;
        }

        public List<String> getMethods() {
            return methods;
        }

        @Override
        public String getData() {
            return name;
        }

        @Override
        public void setDataFile(String file) {
        }

        @Override
        public void restoreDataFile() {
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            Element nameNode = document.createElementNS(nameSpace, "name");
            nameNode.setTextContent(name);
            element.appendChild(nameNode);
            for (String extend : extending) {
                Element extendNode = document.createElementNS(nameSpace, "extend");
                extendNode.setTextContent(extend);
                element.appendChild(extendNode);
            }
            for (String implement : implementing) {
                Element implementNode = document.createElementNS(nameSpace, "implement");
                implementNode.setTextContent(implement);
                element.appendChild(implementNode);
            }
            for (String attribute : attributes) {
                Element attributeNode = document.createElementNS(nameSpace, "attribute");
                attributeNode.setTextContent(attribute);
                element.appendChild(attributeNode);
            }
            for (String method : methods) {
                Element methodNode = document.createElementNS(nameSpace, "method");
                methodNode.setTextContent(method);
                element.appendChild(methodNode);
            }
        }
    };

    public static class Image extends Plain {

        private String cached;
        private String originalCached;

        public Image(Element element) {
            super(element);
            if (!element.getAttribute("cached").equals("")) {
                cached = element.getAttribute("cached");
            }
        }

        public Image(String file, String cached, Source source) {
            super(file, source);
            this.cached = cached;
        }

        public Image(String file, Source source) {
            this(file, null, source);
            cached = file;
        }

        public String getCached() {
            return cached;
        }

        @Override
        public void setDataFile(String file) {
            super.setDataFile(file);
            originalCached = cached;
            cached = file;
        }

        @Override
        public void restoreDataFile() {
            super.restoreDataFile();
            cached = originalCached;
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            super.toNode(element, document, nameSpace);
            if (cached != null) {
                element.setAttribute("cached", cached);
            }
        }
    };

    public static class Link extends Plain {

        private String url;

        public Link(Element element) {
            super(element);
            url = element.getAttribute("url");
        }

        public Link(String name, String url, Source source) {
            super(name, source);
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    };

    /*
     * Static elemetns
     */
    private static final Map<String, Constructor<?>> types = Controller.getClassMap(Data.class);

    public static Data getInstance(Element element) {
        return (Data) Controller.getInstance(types, element);
    }
}
