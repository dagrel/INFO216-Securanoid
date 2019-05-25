package app.assets;

import java.util.ArrayList;

public class Event {
    public String prefix;
    public String name;
    public String measurement;
    public String measurementUnit;
    public String measurementType;
    public ArrayList<Time> timeList = new ArrayList<>();

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}