package lt.dinosy.datalib;

import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Logical (not graphical) relations between data
 *
 * @author Aurelijus Banelis
 */
public abstract class Relation {
    private Data from;
    private Data to;

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

    protected void addCommonAttributes(Element element) {
        element.setAttribute("from", String.valueOf(getFrom().getId()));
        element.setAttribute("to", String.valueOf(getTo().getId()));
    }

    public abstract Element toNode(Document document, String nameSpace);

    
    /*
     * Relation types
     */

    public static class Generalization extends Relation {
        public Generalization(Data from, Data to) {
            super(from, to);
        }

        @Override
        public Element toNode(Document document, String nameSpace) {
            Element element = document.createElementNS(nameSpace, "element");
            addCommonAttributes(element);
            element.setAttribute("type", "generalization");
            return element;
        }
    };


    /*
     * Static elements
     */

    public static Relation getInstance(Node node, Map<Integer, Data> data) {
        Integer fromId = Integer.valueOf(node.getAttributes().getNamedItem("from").getNodeValue());
        Integer toId = Integer.valueOf(node.getAttributes().getNamedItem("to").getNodeValue());
        return new Generalization(data.get(fromId), data.get(toId));
    }
}
