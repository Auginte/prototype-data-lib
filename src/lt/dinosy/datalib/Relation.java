package lt.dinosy.datalib;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static lt.dinosy.datalib.Controller.subElements;
import static lt.dinosy.datalib.Controller.getRealNodeName;

/**
 * Logical (not only graphical) relations between data
 *
 * @author Aurelijus Banelis
 * @todo Serialize all
 */
public abstract class Relation implements Serializable {
    private Data from;
    private Data to;
    private int fromId;
    private int toId;
    
    protected Relation(Element element) {
        fromId = Integer.valueOf(element.getAttribute("from"));
        toId = Integer.valueOf(element.getAttribute("to"));
    }
    
    public Relation(Data from, Data to) {
        this.from = from;
        this.to = to;
    }

    public Data getFrom() {
        return from;
    }

    public Data getTo() {
        return to;
    }
    
    void resolveData(Map<Integer, Data> data) {
        from = data.get(fromId);
        to = data.get(toId);
    }

    public final Element toNode(Document document, String nameSpace) {
        Element element = document.createElement(getNodeName(this.getClass()));
        element.setAttribute("from", String.valueOf(getFrom().getId()));
        element.setAttribute("to", String.valueOf(getTo().getId()));
        toNode(element, document, nameSpace);
        return element;
    }

    protected abstract void toNode(Element element, Document document, String nameSpace);
    
    
    /*
     * Relation types
     */

    public static class Generalization extends Relation {
        public Generalization(Element element) {
            super(element);
        }
        
        public Generalization(Data from, Data to) {
            super(from, to);
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) { }
        
    };

    public static class Association extends Relation {
        private String name;
        
        public Association(Element element) {
            super(element);
            for (Element subElement : subElements(element)) {
                if (getRealNodeName(subElement).equals("arrowTo")) {
                    name = subElement.getTextContent();
                }
            }
        }
        
        public Association(Data from, Data to, String name) {
            super(from, to);
            this.name = name;
        }

        public String getName() {
            return name;
        }
        
        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            Element arrowTo = document.createElementNS(nameSpace, "arrowTo");
            arrowTo.setTextContent(name);
            element.appendChild(arrowTo);
        }

        @Override
        public String toString() {
            return Association.class.getSimpleName() + ": " + getName() + " | " + getFrom() + " -> " + getTo();
        }
    }
    
    /*
     * Static elements
     */

    private static final Map<String, Constructor<?>> types = Controller.getClassMap(Relation.class);
    
    public static Relation getInstance(Element element, Map<Integer, Data> data) {
        Relation result = (Relation) Controller.getInstance(types, element);
        result.resolveData(data);
        return result;
    }
    
    public static String getNodeName(java.lang.Class<? extends Relation> classObject) {
        return classObject.getSimpleName().toLowerCase();
    }

}
