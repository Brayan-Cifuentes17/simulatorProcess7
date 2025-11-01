package model;

public enum Status {
    BLOQUEADO("Bloqueado"), 
    NO_BLOQUEADO("No bloqueado");

    private String value;

    Status(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}