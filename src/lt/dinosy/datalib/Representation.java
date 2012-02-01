package lt.dinosy.datalib;

import java.awt.Color;
import java.util.Map;
import java.lang.reflect.Constructor;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import org.w3c.dom.Document;
import static lt.dinosy.datalib.Controller.subElements;
import static lt.dinosy.datalib.Controller.getRealNodeName;

/**
 * Representation of data.
 * There can be many representations for one data,
 * or representation can group data
 *
 * @author Aurelijus Banelis
 */
public abstract class Representation {
    private int dataId;
    private Data data;
    private Object assigned;
    
    private Representation(int dataId) {
        this.dataId = dataId;
    }

    private Representation(org.w3c.dom.Element element) {
        this.dataId = Integer.parseInt(element.getAttribute("dataId"));
    }

    int getDataId() {
        if (data == null) {
            return dataId;
        } else {
            return data.getId();
        }
    }

    public Data getData() {
        return data;
    }
    
    public void resovleData(Map<Integer, Data> map) {
        data = map.get(dataId);
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
        private int zIndex = 0;
        private Color background;
        private Color foreground;
        
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
            zIndex = Integer.parseInt(element.getAttribute("zIndex"));
            foreground = getColor(element.getAttribute("foreground"));
            background = getColor(element.getAttribute("background"));
        }
        
        public Element(Data data, Point2D position, double z, Dimension2D size, int zIndex, Color foreground, Color background, boolean mainIdea, Object assigned) {
            super(data.getId());
            this.position = position;
            this.z = z;
            this.size = size;
            this.mainIdea = mainIdea;
            this.foreground = foreground;
            this.background = background;
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
            if (mainIdea) {
                element.setAttribute("mainIdea", Boolean.toString(mainIdea));
            }
            element.setAttribute("zIndex", Integer.toString(zIndex));
            colorToNode(element, "foreground", foreground);
            colorToNode(element, "background", background);
        }

        
        
        private void colorToNode(org.w3c.dom.Element element, String key, Color color) {
            if (color != null) {
                String text = "#" + toHex(color.getRed()) + toHex(color.getGreen()) + toHex(color.getBlue()) + toHex(color.getAlpha());
                element.setAttribute(key, text);
            }
        }
        
        private String toHex(int number) {
            if (number < 16) {
                return "0" + Integer.toHexString(number);
            } else {
                return Integer.toHexString(number);
            }
        }

        private Color getColor(String text) {
            if (text != null && text.length() == 9) {
                int r = Integer.parseInt(text.substring(1,3), 16);
                int g = Integer.parseInt(text.substring(3,5), 16);
                int b = Integer.parseInt(text.substring(5,7), 16);
                int a = Integer.parseInt(text.substring(7,9), 16);
                return new Color(r, g, b, a);
            } else {
                return null;
            }
        }
        
        public int getZIndex() {
            return zIndex;
        }

        public void setZIndex(int zIndex) {
            this.zIndex = zIndex;
        }

        public Color getBackground() {
            return background;
        }

        public Color getForeground() {
            return foreground;
        }

        public void setBackground(Color background, boolean opaque) {
            if (!opaque) {
                background = null;
            }
            this.background = background;
        }

        public void setForeground(Color foreground, Color defaultForeground) {
            if (foreground == defaultForeground) {
                foreground = null;
            }
            this.foreground = foreground;
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
}
