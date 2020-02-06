package de.neofonie.styla.core.models;

public enum PathEntryType {
    PAGE("PAGE"),
    MODULE("MODULE");

    private String value;

    PathEntryType(final String value) {
        this.value = value;
    }

    public String geValue() {
        return value;
    }
}
