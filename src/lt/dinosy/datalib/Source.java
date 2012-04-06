package lt.dinosy.datalib;

import java.io.Serializable;
import java.util.SimpleTimeZone;
import java.util.Calendar;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Representing source of gathered data or conditions of data being generated.
 *
 * @author Aurelijus Banelis
 * @todo Serialize all
 */
public abstract class Source implements Serializable {
    private int id;
    private Date date;
    private int parentId = Controller.defaultParentId;
    private Source parent;
    private List<Source> childs = new LinkedList<Source>();

    /**
     * @throws  NullPointerException when date is null
     */
    private Source(Date date, Source parent) {
        this.parent = parent;
        if (parent != null) {
            parentId = parent.getId();
        }
        this.date = date;
        if (date == null) {
            throw new NullPointerException("Date canot be null");
        }
        id = Controller.getNewSourceId();
    }

    private Source(Element element) {
        id = Integer.valueOf(element.getAttribute("id"));
        parentId = Controller.defaultParentId;
        if (element.getAttribute("parentId").length() > 0) {
            parentId = Integer.valueOf(element.getAttribute("parentId"));
        }
        date = parseDate(element.getAttribute("date"));
    }

    public Date getDate() {
        return date;
    }
    
    public String getDateSting() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public int getId() {
        return id;
    }

    void translateId(int translate) {
        id += translate;
    }

    public Source getParent() {
        return parent;
    }

    int getParentId() {
        if (parent == null) {
            return parentId;
        } else {
            return parent.getId();
        }
    }

    void setParent(Source parent) {
        this.parent = parent;
    }

    public void resolveInheritance(Map<Integer, Source> sources) {
        for (Source instance : sources.values()) {
            if (instance.getParentId() == id) {
                childs.add(instance);
                instance.setParent(this);
            }
        }
    }

    public final Element toNode(Document document, String nameSpace) {
        Element element = document.createElement(getNodeName(this.getClass()));
        element.setAttribute("id", String.valueOf(getId()));
        if (getParent() != null) {
            element.setAttribute("parentId", String.valueOf(getParent().getId()));
        }
        element.setAttribute("date", parseDate(date));
        toNode(element, document, nameSpace);
        return element;
    }

    public static String getNodeName(java.lang.Class<? extends Source> classObject) {
        return classObject.getSimpleName().toLowerCase();
    }

    public abstract String getSource();
    protected abstract void toNode(Element element, Document document, String nameSpace);

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + getSource();
    }


    /*
     * Types of data
     */

    public static class Event extends Source {
        private String place;
        private String name;

        public Event(Date date, String name, String place, Source parent) {
            super(date, parent);
            this.name = name;
            this.place = place;
        }

        public Event(Date date, String name, String place) {
            this(date, name, place, null);
        }

        public Event() {
            this(new Date(), "", "");
        }

        public Event(Element element) {
            super(element);
            place = element.getAttribute("place");
            name = element.getTextContent();
        }

        public String getPlace() {
            return place;
        }

        @Override
        public String getSource() {
            return name;
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            if (place != null && place.length() > 0) {
                element.setAttribute("place", place);
            } else {
                element.setAttribute("place", "");
            }
            element.appendChild(document.createTextNode(name));
        }

        @Override
        public String toString() {
            if (name.length() < 1) {
                return "Event: " + getDateSting();
            } else {
                return super.toString();
            }
        }
    };

    public static class Project extends Source {
        private String name;
        private String address;
        private String owner;

        public Project(Date date, String name, String address, String owner, Source parent) {
            super(date, parent);
            this.name = name;
            this.address = address;
            this.owner = owner;
        }

        public Project(Date date, String name) {
            this(date, name, "", "", null);
        }

        public Project(Element element) {
            super(element);
            this.name = element.getTextContent();
            this.address = element.getAttribute("address");
            this.owner = element.getAttribute("owner");
        }

        public String getAddress() {
            return address;
        }

        public String getOwner() {
            return owner;
        }

        @Override
        public String getSource() {
            return name;
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            if (address != null && address.length() > 0) {
                element.setAttribute("address", address);
            }
            if (owner != null && owner.length() > 0) {
                element.setAttribute("owner", owner);
            }
            element.appendChild(document.createTextNode(name));
        }
    };

    public static class Model extends Source {
        private Language language;
        private String file;

        public Model(Date date, Language language, String file, Source parent) {
            super(date, parent);
            this.language = language;
            this.file = file;
        }

        public Model(Element element) {
            super(element);
            language = Language.valueOf(element.getAttribute("language"));
            file = element.getTextContent();
        }

        public Language getLanguage() {
            return language;
        }

        @Override
        public String getSource() {
            return file;
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            element.setAttribute("language", language.getValue());
            element.appendChild(document.createTextNode(file));
        }

        public enum Language {
            php("php"),
            java("java"),
            cpp("c++");

            private String value;
            private Language(String value) {
                this.value = value;
            }
            public String getValue() {
                return value;
            }
            public Language getName(String value) {
                for (Language language : this.values()) {
                    if (language.getValue().equals(value)) {
                        return language;
                    }
                }
                return null;
            }
        };
    };

    public static class Book extends Source {
        private String name;
        private int page;
        private String isbn = null;

        public Book(Date date, String name, int page, String isbn, Source source) {
            super(date, source);
            this.name = name;
            this.page = page;
            this.isbn = isbn;
        }

        public Book(String name, int page) {
            super(new Date(), null);
            this.name = name;
            this.page = page;
        }

        public Book(Element element) {
            super(element);
            page = Integer.valueOf(element.getAttribute("page"));
            isbn = element.getAttribute("isbn");
            name = element.getTextContent();
        }

        @Override
        public String getSource() {
            return name;
        }

        public String getIsbn() {
            return isbn;
        }

        public int getPage() {
            return page;
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            element.setAttribute("page", String.valueOf(page));
            if (isbn != null && isbn.length() > 0) {
                element.setAttribute("isbn", isbn);
            }
            element.appendChild(document.createTextNode(name));
        }
    }

    public static class Okular extends Book {
        private Boundary boundary;
        private String cachedImage;
        
        public Okular(Date date, String file, int page, Boundary boundary, String isbn, String cachedImage, Source source) {
            super(date, file, page, isbn, source);
            this.boundary = boundary;
            this.cachedImage = cachedImage;
        }

        public Okular(Date date, String file, int page, Boundary boundary, String cachedImage) {
            super(date, file, page, null, null);            
            this.boundary = boundary;
            this.cachedImage = cachedImage;
        }

        public Okular(Element element) {
            super(element);
            String position = element.getAttribute("position");
            if (position != null) {
                boundary = new Boundary(position);
            }
            cachedImage = element.getAttribute("cachedImage");
        }

        public Boundary getPosition() {
            return boundary;
        }

        public String getCachedImage() {
            if (cachedImage != null && cachedImage.length() == 0) {
                return null;
            } else {
                return cachedImage;
            }
        }
        
        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            super.toNode(element, document, nameSpace);
            if (boundary != null) {
                element.setAttribute("position", boundary.toString());
            }
            if (cachedImage != null && cachedImage.length() > 0) {
                element.setAttribute("cachedImage", cachedImage);
            }
        }

        @Override
        public String toString() {
            int slash = getSource().lastIndexOf(System.getProperty("file.separator"));
            if (slash < 0) {
                slash = 0;
            }
            String fileName = getSource().substring(slash).trim();
            return fileName + ": " + getPage();
        }
        
        public static class Boundary implements Serializable {
            public float l, r, t, b;

            public Boundary(String list) {
                String[] parts = list.split(" ", 4);
                l = Float.valueOf(parts[0]);
                r = Float.valueOf(parts[0]);
                t = Float.valueOf(parts[0]);
                b = Float.valueOf(parts[0]);
            }

            public Boundary(float l, float r, float t, float b) {
                this.l = l;
                this.r = r;
                this.t = t;
                this.b = b;
            }

            @Override
            public String toString() {
                return l + " " + r + " " + t + " " + b;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {  return false; }
                if (getClass() != obj.getClass()) { return false; }
                final Boundary other = (Boundary) obj;
                if (Float.floatToIntBits(this.l) != Float.floatToIntBits(other.l)) { return false; }
                if (Float.floatToIntBits(this.r) != Float.floatToIntBits(other.r)) { return false; }
                if (Float.floatToIntBits(this.t) != Float.floatToIntBits(other.t)) { return false; }
                if (Float.floatToIntBits(this.b) != Float.floatToIntBits(other.b)) { return false; }
                return true;
            }

            @Override
            public int hashCode() {
                int hash = 7;
                hash = 83 * hash + Float.floatToIntBits(this.l);
                hash = 83 * hash + Float.floatToIntBits(this.r);
                hash = 83 * hash + Float.floatToIntBits(this.t);
                hash = 83 * hash + Float.floatToIntBits(this.b);
                return hash;
            }
            
            
        };
    }
    
    public static class Internet extends Source {
        private String url;
        private String xpaht = null;
        private String title = null;
        private String saved = null;

        public Internet(Date date, String url, String xpaht, String title, String saved, Source source) {
            super(date, source);
            this.url = url;
            this.xpaht = xpaht;
            this.title = title;
            this.saved = saved;
        }

        public Internet(String url) {
            super(new Date(), null);
            this.url = url;
        }

        public Internet(Element element) {
            super(element);
            this.xpaht = element.getAttribute("xpath");
            this.url = element.getTextContent();
        }

        @Override
        public String getSource() {
            return url;
        }

        public String getXpaht() {
            return xpaht;
        }

        public String getTitle() {
            return title;
        }

        public String getSaved() {
            return saved;
        }
        
                
        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            if (xpaht != null) {
                element.setAttribute("xpath", xpaht);
            }
            if (saved != null) {
                element.setAttribute("saved", saved);
            }
            element.appendChild(document.createTextNode(url));
        }
    }

    public static class Dinosy extends Source {
        private String url;
        private Type type;

        public Dinosy(Date date, String url, Type type, Source source) {
            super(date, source);
            this.url = url;
            this.type = type;
        }

        public Dinosy(Element element) {
            super(element);
            this.type = Type.valueOf(element.getAttribute("type"));
            this.url = element.getTextContent();
        }

        @Override
        public String getSource() {
            return url;
        }

        public Type getType() {
            return type;
        }

        @Override
        protected void toNode(Element element, Document document, String nameSpace) {
            element.setAttribute("type", type.name());
            element.appendChild(document.createTextNode(url));
        }

        public enum Type {
            include,
            link
        }
    }

    /*
     * Static elemetns
     */

    private static final Map<String, Constructor<?>> types = Controller.getClassMap(Source.class);

    public static Source getInstance(Element element) {
        return (Source) Controller.getInstance(types, element);
    }
    
    public static Date parseDate(String date) {
        java.util.Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setCalendar(cal);
        try {
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Logger.getLogger(Source.class.getName()).log(Level.SEVERE, "Error parsing date: " + date, ex);
            return null;
        }
    }
    
    public static String parseDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }
}
