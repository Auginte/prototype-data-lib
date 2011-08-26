package lt.dinosy.datalib;

import java.util.Map;
import java.lang.reflect.Constructor;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import static lt.dinosy.datalib.Controller.subElements;
import static lt.dinosy.datalib.Controller.getRealNodeName;

/**
 * Representation of data.
 * There can be meny represenations for one data,
 * or represenation can group data
 *
 * @author Aurelijus Banelis
 */
public abstract class Representation {
    private int dataId;
    private Object assigned;

    private Representation(int dataId) {
        this.dataId = dataId;
    }

    private Representation(org.w3c.dom.Element element) {
        this.dataId = Integer.parseInt(element.getAttribute("dataId"));
    }

    public int getDataId() {
        return dataId;
    }

    void translateDataId(int length) {
        dataId += length;
    }

    public final void setAssigned(Object assigned) {
        this.assigned = assigned;
    }

    public Object getAssigned() {
        return assigned;
    }

    public final org.w3c.dom.Element toNode(org.w3c.dom.Document document, String nameSpace) {
        org.w3c.dom.Element element = document.createElementNS(nameSpace, getNodeName(this.getClass()));
        element.setAttribute("dataId", String.valueOf(getDataId()));
        toNode(element, document, nameSpace);
        return element;
    }

    public static String getNodeName(java.lang.Class<? extends Representation> classObject) {
        return classObject.getSimpleName().toLowerCase();
    }

    protected abstract void toNode(org.w3c.dom.Element element, org.w3c.dom.Document document, String nameSpace);

    /*
     * Types of representation
     */

    public static class Element extends Representation {
        private Point2D position;
        private Dimension2D size;
        private double z;
        private boolean mainIdea = false;

        public Element(org.w3c.dom.Element element) {
            super(element);
            for (org.w3c.dom.Element sub : subElements(element)) {
                if (getRealNodeName(sub).equals("position")) {
                    String[] positionData = sub.getTextContent().split(" ");
                    position = new Point2D.Double(Double.valueOf(positionData[0]), Double.valueOf(positionData[1]));
                    z = Double.valueOf(positionData[2]);
                } else if (getRealNodeName(sub).equals("size")) {
                    String[] sizeData = sub.getTextContent().split(" ");
                    size = new DoubleDimention(Double.valueOf(sizeData[0]), Double.valueOf(sizeData[1]));
                }
            }
            mainIdea = Boolean.parseBoolean(element.getAttribute("mainIdea"));
        }

        public Element(Data data, Point2D position, double z, Dimension2D size, boolean mainIdea, Object assigned) {
            super(data.getId());
            this.position = position;
            this.z = z;
            this.size = size;
            this.mainIdea = mainIdea;
            setAssigned(assigned);
        }

        protected org.w3c.dom.Element positionToNode(org.w3c.dom.Document document, String nameSpace) {
            org.w3c.dom.Element element = document.createElementNS(nameSpace, "position");
            element.setTextContent(getPosition().getX() + " " + getPosition().getY() + " " + getZ());
            return element;
        }

        protected org.w3c.dom.Element sizeToNode(org.w3c.dom.Document document, String nameSpace) {
            org.w3c.dom.Element element = document.createElementNS(nameSpace, "size");
            element.setTextContent(getSize().getWidth() + " " + getSize().getHeight());
            return element;
        }

        public void set(Point2D position, double z, Dimension2D size) {
            this.position = position;
            this.z = z;
            this.size = size;
        }

        public Point2D getPosition() {
            return position;
        }

        public Dimension2D getSize() {
            return size;
        }

        public double getZ() {
            return z;
        }

        @Override
        protected void toNode(org.w3c.dom.Element element, Document document, String nameSpace) {
            element.appendChild(positionToNode(document, nameSpace));
            element.appendChild(sizeToNode(document, nameSpace));
        }

    }

    public static class PlaceHolder extends Representation {
        public PlaceHolder(org.w3c.dom.Element element) {
            super(element);
        }

        public PlaceHolder(int dataId) {
            super(dataId);
        }

        @Override
        protected void toNode(org.w3c.dom.Element element, Document document, String nameSpace) { }
        
    }

    /*
     * Static elemetns
     */

    private static final Map<String, Constructor<?>> types = Controller.getClassMap(Representation.class);

    public static Representation getInstance(org.w3c.dom.Element element) {
        return (Representation) Controller.getInstance(types, element);
    }

    private static class DoubleDimention extends Dimension2D {
        private double width;
        private double height;
        
        public DoubleDimention(double width, double height) {
            setSize(width, height);
        }

        @Override
        public double getWidth() {
            return width;
        }

        @Override
        public double getHeight() {
            return height;
        }

        @Override
        public final void setSize(double width, double height) {
            this.width = width;
            this.height = height;
        }

    }

    private static Node getElement(String name, Node parent) {
        for (int i= 0; i < parent.getChildNodes().getLength(); i++) {
            Node item = parent.getChildNodes().item(i);
            if (item instanceof org.w3c.dom.Element) {
                if (Controller.getRealNodeName(item).equals(name)) {
                    return item;
                }
            }
        }
        return null;
    }
}
