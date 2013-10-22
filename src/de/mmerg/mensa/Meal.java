package de.mmerg.mensa;

import java.util.Date;

import android.provider.BaseColumns;

public class Meal {
    public Meal(String name, String typ, float priceStudent, float priceGuest,
            boolean bio, Date date) {
        this.name = name;
        this.typ = typ;
        this.priceStudent = priceStudent;
        this.priceGuest = priceGuest;
        this.bio = bio;
        this.date = date;
    }
    
    public Meal(Date date) {
        this.date = date;
    }

    private String name;
    private String typ;
    private float priceStudent;
    private float priceGuest;
    private boolean bio;
    private Date date;

    public static final String _ID = BaseColumns._ID;
    public static final String NAME = "name";
    public static final String TYP = "typ";
    public static final String PRICE_STUDENT = "priceStudent";
    public static final String PRICE_GUEST = "priceGuest";
    public static final String BIO = "bio";
    public static final String DATE = "date";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public float getPriceStudent() {
        return priceStudent;
    }

    public void setPriceStudent(float priceStudent) {
        this.priceStudent = priceStudent;
    }

    public float getPriceGuest() {
        return priceGuest;
    }

    public void setPriceGuest(float priceGuest) {
        this.priceGuest = priceGuest;
    }

    public boolean isBio() {
        return bio;
    }

    public void setBio(boolean bio) {
        this.bio = bio;
    }

    public Date getDate() {
        return date;
    }

}
