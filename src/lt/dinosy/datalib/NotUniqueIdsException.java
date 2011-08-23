package lt.dinosy.datalib;

/**
 * Storing data only with unique id attributes.
 *
 * @author Aurelijus Banelis
 */
public class NotUniqueIdsException extends Exception {
    public Data data1;
    public Data data2;

    public NotUniqueIdsException(Data data1, Data data2) {
        this.data1 = data1;
        this.data2 = data2;
    }

    @Override
    public String getMessage() {
        return "Id " + data1.getId() + " is same against " + data1 + " and " + data2;
    }
}
