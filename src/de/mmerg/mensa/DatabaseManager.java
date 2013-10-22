package de.mmerg.mensa;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Manage off-line data in a local database
 * 
 * @author michaelmerg
 *
 */
public class DatabaseManager {
    public static final String  TAG                 = "DatabaseManager";

    private static final int    DATABASE_VERSION    = 3;
    private static final String DATABASE_NAME       = "mensa_db";
    private static final String TABLE_NAME          = "meals";

    private SQLiteDatabase      mDB;
    
    
    public DatabaseManager(Context context) {
        DatabaseHelper helper = new DatabaseHelper(context);
        this.mDB = helper.getWritableDatabase();
    }

    private class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME + " ( " + Meal._ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Meal.NAME
                    + " TEXT, " + Meal.TYP + " TEXT, " + Meal.PRICE_STUDENT
                    + " FLOAT, " + Meal.PRICE_GUEST + " FLOAT, " + Meal.BIO
                    + " BIT, " + Meal.DATE + " LONG " + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVers, int newVers) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    public List<Meal> getMeals() {
        List<Meal> meals = new ArrayList<Meal>();
        Cursor c = mDB.query(TABLE_NAME, new String[] { Meal.NAME, Meal.TYP,
                Meal.PRICE_STUDENT, Meal.PRICE_GUEST, Meal.BIO, Meal.DATE },
                null, null, null, null, null);
        c.moveToFirst();
        do {
            String name = c.getString(c.getColumnIndex(Meal.NAME));
            String typ = c.getString(c.getColumnIndex(Meal.TYP));
            float priceStudent = c.getFloat(c
                    .getColumnIndex(Meal.PRICE_STUDENT));
            float priceGuest = c.getFloat(c.getColumnIndex(Meal.PRICE_GUEST));
            boolean bio = (c.getInt(c.getColumnIndex(Meal.BIO)) == 1);
            Date date = new Date(c.getLong(c.getColumnIndex(Meal.DATE)));
            Meal meal = new Meal(name, typ, priceStudent, priceGuest, bio, date);
            meals.add(meal);
        } while (c.moveToNext());
        
        return meals;
    }

    public void addMeal(Meal meal) {
        ContentValues values = new ContentValues();
        values.put(Meal.NAME, meal.getName());
        values.put(Meal.TYP, meal.getTyp());
        values.put(Meal.PRICE_STUDENT, meal.getPriceStudent());
        values.put(Meal.PRICE_GUEST, meal.getPriceGuest());
        values.put(Meal.BIO, meal.isBio());
        values.put(Meal.DATE, meal.getDate().getTime());
        mDB.insertOrThrow(TABLE_NAME, null, values);
    }

    public void removeMeals() {
        mDB.delete(TABLE_NAME, null, null);
    }

    public void close() {
        mDB.close();
        mDB = null;
    }
}
