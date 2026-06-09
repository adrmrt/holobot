package dev.zawarudo.holo.modules.xkcd;

import com.google.gson.annotations.SerializedName;

public class XkcdComic implements Comparable<XkcdComic> {

    @SerializedName("day")
    private int day;
    @SerializedName("month")
    private int month;
    @SerializedName("year")
    private int year;
    @SerializedName("num")
    private int num;
    @SerializedName("title")
    private String title;
    @SerializedName("alt")
    private String alt;
    @SerializedName("img")
    private String img;

    /**
     * Returns the year at which this comic was published.
     */
    public int getDay() {
        return day;
    }

    /**
     * Returns the month at which this comic was published.
     */
    public int getMonth() {
        return month;
    }

    /**
     * Returns the year at which this comic was published.
     */
    public int getYear() {
        return year;
    }

    /**
     * Returns the date at which the comic was published.
     */
    public String getDate() {
        return day + "/" + month + "/" + year;
    }

    /**
     * Sets the date of the comic
     */
    public void setDate(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    /**
     * Returns the issue number of the comic
     */
    public int getIssueNr() {
        return num;
    }

    /**
     * Sets the issue number of the comic
     */
    public void setIssueNr(int num) {
        this.num = num;
    }

    /**
     * Returns the title of the comic
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the comic
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the alt text of the comic
     */
    public String getAlt() {
        return alt;
    }

    /**
     * Sets the alt text of the comic
     */
    public void setAlt(String alt) {
        this.alt = alt;
    }

    /**
     * Returns the image Url of the comic
     */
    public String getImg() {
        return img;
    }

    /**
     * Sets the image Url of the comic
     */
    public void setImg(String url) {
        this.img = url;
    }

    /**
     * Returns the permanent link of the comic
     */
    public String getUrl() {
        return "https://xkcd.com/" + num + "/";
    }

    /**
     * Returns the URL of the comic's explanation
     */
    public String getExplainedUrl() {
        return "https://www.explainxkcd.com/wiki/index.php/" + num;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof XkcdComic other)) return false;
        return this.getIssueNr() == other.getIssueNr();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(num);
    }

    @Override
    public String toString() {
        return "xkcd " + num + ": " + title;
    }

    @Override
    public int compareTo(XkcdComic other) {
        return Integer.compare(this.num, other.num);
    }
}
