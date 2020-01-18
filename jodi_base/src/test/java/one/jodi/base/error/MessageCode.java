package one.jodi.base.error;

import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;

import java.util.List;

/**
 * Class defining the error warning messages storage structure, MessageCode.
 */
public class MessageCode {
    private final int id;
    private final String body;
    private MESSAGE_TYPE type;
    private List<String> classLocations;

    public MessageCode(int id, String body, List<String> classLocations) {
        super();
        this.id = id;
        this.setType(MESSAGE_TYPE.UNUSED);
        this.body = body;
        this.classLocations = classLocations;
    }

    public List<String> getClassLocations() {
        return classLocations;
    }

    public void setClassLocations(List<String> classLocations) {
        this.classLocations = classLocations;
    }

    public int getId() {
        return id;
    }

    public String getBody() {
        return body;
    }

    public void printMessageCodeAndBody() {
        System.out.println(this.id + ": " + this.body.trim());
    }

    public void printMessageCodeAndTypeAndBody() {
        System.out.println(this.id + ", " + this.type + ", " + this.body.trim());
    }


    public void printMessageCodeAndBodyAndLocations() {
        System.out.println("\n" + this.id + ": " + this.body.trim());
        int counter = 1;
        for (String locations : this.classLocations) {
            System.out.println(counter + ") " + locations);
            counter++;
        }
    }

    public MESSAGE_TYPE getType() {
        return type;
    }

    public void setType(MESSAGE_TYPE type) {
        this.type = type;
    }
}
