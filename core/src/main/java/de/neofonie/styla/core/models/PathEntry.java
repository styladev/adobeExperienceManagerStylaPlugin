package de.neofonie.styla.core.models;

import java.io.Serializable;

/**
 * PathEntry class is a representation for the response
 * of https://paths.styla.com/v1/delta/ci-oxid-nle
 *
 * @author Sebastian Sachtleben
 */
public class PathEntry implements Serializable {

    private String name;
    private String path;
    private PathEntryType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public PathEntryType getType() {
        return type;
    }

    public void setType(final PathEntryType type) {
        this.type = type;
    }
}
