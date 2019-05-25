package app.assets;

public class Time {
    public String prefix;
    public String start;
    public String end;

    public String getStartYear() {
        return this.start.substring(0,3);
    }

    public String getStartMonth() {
        return this.start.substring(5,6);
    }

    public String getStartDay() {
        return this.start.substring(8,9);
    }

    public String getDate(String s) {
        return this.getStartYear() + "-" + this.getStartMonth() + "-" + this.getStartDay();
    }

    public String getStartHour() {
        return this.start.substring(11,12);
    }

    public String getStartMinute() {
        return this.start.substring(14,15);
    }

    public String getStartSeconds() {
        return this.start.substring(16,17);
    }

    public String getStartTime() {
        return this.getStartYear() + "-" + this.getStartMonth() + "-" + this.getStartDay();
    }
}