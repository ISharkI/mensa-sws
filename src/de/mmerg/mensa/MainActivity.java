package de.mmerg.mensa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.mgbc.mensa.R;
import de.mmerg.mensa.FlingViewGroup.ViewSwitchListener;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivtiy";

    private static final String WEEK_NUMBER = "week-number";
    private static final String[] WEEKDAYS_GERMAN = {"Mo", "Di", "Mi", "Do", "Fr"}; 

    public static final int UPDATE = 0;
    public static final int ERROR = 1;
    public static final int NO_CONNECTION = 2;

    private final String BASE_URL = "http://www.studentenwerk-stuttgart.de/" +
    		"gastronomie/speiseangebot/";

    private Handler mHandler;
    private DatabaseManager mDatabase;
    private LinearLayout mOverlay;
    private Calendar mCalendar;

    private Button[] mButtons;
    private ListView[] mLists;
    private List<Meal> mMeals;

    private FlingViewGroup mViewGroup;
    private ViewSwitchListener mSwitchListener;
    
    private Thread mDownloadThread;
    private boolean mIsDestroyed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /** load current week number in the default preferences */
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        final Editor editor = prefs.edit();
        final int weekInDb = prefs.getInt(WEEK_NUMBER, -1);

        /** Initialize variables */
        mCalendar = Calendar.getInstance();
        int weekday = mCalendar.get(Calendar.DAY_OF_WEEK);
        boolean nextWeek = false;
        if (weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY) {
            mCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            nextWeek = true;
        }
        mCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        
        final int year = mCalendar.get(Calendar.YEAR);
        final int week = mCalendar.get(Calendar.WEEK_OF_YEAR);
        
        mDatabase = new DatabaseManager(this);
        mButtons = new Button[5];
        mLists = new ListView[5];
        mMeals = new ArrayList<Meal>();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case UPDATE:
                    updateLists();
                    mDownloadThread = null;
                    // Store week number in default preferences.
                    editor.putInt(WEEK_NUMBER, week);
                    editor.commit();
                    break;
                case ERROR:
                    // TODO Get string from xml.
                    Toast.makeText(MainActivity.this,
                            "Fehler beim Laden des Speiseplans.",
                            Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case NO_CONNECTION:
                    // TODO Get string from xml.
                    Toast.makeText(MainActivity.this,
                            "Keine Internetverbindung.",
                            Toast.LENGTH_LONG).show();
                    finish();
                    break;
                }
            }
        };

        mSwitchListener = new ViewSwitchListener() {

            @Override
            public void onSwitching(float progress) {
            }

            @Override
            public void onSwitched(int position) {
                switchTo(position, false);
            }
        };

        /** Initialize views */
        mViewGroup = (FlingViewGroup) findViewById(R.id.fling_view_group);
        mViewGroup.setOnViewSwitchedListener(mSwitchListener);

        // Get buttons from layout
        mButtons[0] = (Button) findViewById(R.id.mon_button);
        mButtons[1] = (Button) findViewById(R.id.tue_button);
        mButtons[2] = (Button) findViewById(R.id.wed_button);
        mButtons[3] = (Button) findViewById(R.id.thu_button);
        mButtons[4] = (Button) findViewById(R.id.fri_button);

        // Add title and listener to the buttons
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
        String[] weekdays = dateFormatSymbols.getWeekdays();

        for (int i = 0; i < mButtons.length; i++) {
            final int id = i;
            // FIXME java.lang.StringIndexOutOfBoundsException ???
            if (weekdays[i + 2].length() >= 2)
                mButtons[id].setText(weekdays[i + 2].substring(0, 2));
            else 
                mButtons[id].setText(WEEKDAYS_GERMAN[i]);
            mButtons[id].setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    switchTo(id, true);
                }
            });
        }

        // Get lists from layout.
        mLists[0] = (ListView) findViewById(R.id.mon_list);
        mLists[1] = (ListView) findViewById(R.id.tue_list);
        mLists[2] = (ListView) findViewById(R.id.wed_list);
        mLists[3] = (ListView) findViewById(R.id.thu_list);
        mLists[4] = (ListView) findViewById(R.id.fri_list);

        // Disable selector for list items.
        mLists[0].setSelector(android.R.color.transparent);
        mLists[1].setSelector(android.R.color.transparent);
        mLists[2].setSelector(android.R.color.transparent);
        mLists[3].setSelector(android.R.color.transparent);
        mLists[4].setSelector(android.R.color.transparent);


        // Set current day (select button and list)
        if (nextWeek)
            switchTo(0, true);
        else 
            switchTo(weekday - 2, true);

        // Show and/or download data
        if (week == weekInDb && (mMeals = mDatabase.getMeals()).size() > 0) {
            updateLists();
        } else {

            /** Initialize overlay */
            mOverlay = (LinearLayout) findViewById(R.id.overlay);

            // Set loading animation to the overlay
            ImageView img = (ImageView) mOverlay.findViewById(R.id.loading);
            img.setBackgroundResource(R.anim.loading);
            final AnimationDrawable frameAnimation = 
                    (AnimationDrawable) img.getBackground();
            img.post(new Runnable() {

                @Override
                public void run() {
                    frameAnimation.start();
                }
            });
            // TODO Get string from xml.
            setInfoText("Speiseangebot wird geladen...");

            // Show Overlay and download the data
            showOverlay();
            
            // check Connection
            if (isOnline()) {
                asyncDownload(year, week);
            } else {
                mHandler.sendEmptyMessage(NO_CONNECTION);
            }

        }
    }
        
    private void asyncDownload(final int year, final int week) {
        mDownloadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                download(year, week);
            }
        });
        mDownloadThread.start();
    }
    
    private void switchTo(int id, boolean switchView) {
        for (int i = 0; i < mButtons.length; i++)
            mButtons[i].setSelected(i == id);
        if (switchView) {
            mViewGroup.setPosition(id);
        }
    }

    private void updateLists() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy");
        View header;
        TextView date;

        Calendar calendar = (Calendar) mCalendar.clone();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Calendar calendarToday = Calendar.getInstance();
        
        for (int i = 0; i < mButtons.length; i++) {
            List<Meal> meals = new ArrayList<Meal>();
            calendar.set(Calendar.DAY_OF_WEEK, i + 2);

            // Add meals to separate lists.
            for (int j = 0; j < mMeals.size(); j++) {
                Meal meal = mMeals.get(j);
                Calendar mealCalendar = Calendar.getInstance();
                mealCalendar.setTime(meal.getDate());
                if (calendar.get(Calendar.YEAR) == mealCalendar.get(Calendar.YEAR)
                        && calendar.get(Calendar.DAY_OF_YEAR) == mealCalendar
                                .get(Calendar.DAY_OF_YEAR))
                    meals.add(meal);
            }

            // Add header
            header = View.inflate(this, R.layout.list_header, null);
            date = (TextView) header.findViewById(R.id.date);
            if (calendar.get(Calendar.YEAR) == calendarToday.get(Calendar.YEAR)
                    && calendar.get(Calendar.DAY_OF_YEAR) == calendarToday
                            .get(Calendar.DAY_OF_YEAR)) {
                date.setText(getString(R.string.today));
            } else {
                date.setText(sdf.format(calendar.getTime()));
            }
            mLists[i].addHeaderView(header);

            // Set adapter
            mLists[i].setAdapter(new MealAdapter(this, R.layout.list_item,
                    meals));
        }

        hideOverlay();
    }

    private void download(int year, int week) {
        mDatabase.removeMeals();

        URL url;
        BufferedReader reader;
        ArrayList<String> tables = new ArrayList<String>();
        SAXParser parser;
        XMLReader xmlReader;
        TableHandler handler;

        try {
            url = new URL(BASE_URL + year + "-W" + week + "/allweek");
        } catch (MalformedURLException e) {
            Log.e(TAG, "Unable to create URL.");
            mHandler.sendEmptyMessage(NO_CONNECTION);
            return;
        }

        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream()));

            String inputLine;
            String html = "";

            // Read HTML from stream
            while ((inputLine = reader.readLine()) != null) {
                html += inputLine.trim();
            }
            
            // extract <table> parts
            while (html.indexOf("<table>")  != -1) {
                int indexStart = html.indexOf("<table>");
                int indexEnd = html.indexOf("</table>");
                String tmp = html.substring(indexStart, indexEnd + 8);
                
                // fix XML errors
                tmp = tmp.replaceAll("\\s+", " ").replaceAll(" +", " ");
                tmp = tmp.replaceAll("</tr>\\s*</tr>", "</tr>");
                tmp = tmp.replace("<span class=\"next\">", "");

                // Remove special characters
                tmp = tmp.replace("&nbsp;", "").replace("&quot;", "\"");
                tmp = tmp.replace("<br/>", "");
                tmp = tmp.replace("&auml;", "ä");
                
                tables.add(tmp);
                html = html.substring(indexEnd + 8);
            }
                
            reader.close();

        } catch (IOException e) {
            Log.e(TAG, "Unable to download website.");
            mHandler.sendEmptyMessage(NO_CONNECTION);
            return;
        }

        if (tables.size() == 0) {
            Log.e(TAG, "0");
            mHandler.sendEmptyMessage(ERROR);
            return;
        }

        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            xmlReader = parser.getXMLReader();
            handler = new TableHandler();
            xmlReader.setContentHandler(handler);

            for (int i = 0; i < tables.size(); i++) {
                Calendar calendar = (Calendar)mCalendar.clone();
                calendar.add(Calendar.DAY_OF_WEEK, i);
                handler.setDate(calendar.getTime());
                xmlReader.parse(
                        new InputSource(new StringReader(tables.get(i))));
            }

        } catch (ParserConfigurationException e) {
            Log.e(TAG, "1");
            mHandler.sendEmptyMessage(ERROR);
            return;
        } catch (SAXException e) {
            Log.e(TAG, "2");
            e.printStackTrace();
            mHandler.sendEmptyMessage(ERROR);
            return;
        } catch (FactoryConfigurationError e) {
            Log.e(TAG, "3");
            mHandler.sendEmptyMessage(ERROR);
            return;
        } catch (IOException e) {
            Log.e(TAG, "4");
            mHandler.sendEmptyMessage(ERROR);
            return;
        }
        if (!mIsDestroyed)
            mHandler.sendEmptyMessage(UPDATE);
    }

    /**
     * Hide the overlay
     */
    private void hideOverlay() {
        if (mOverlay != null)
            mOverlay.setVisibility(LinearLayout.GONE);
    }

    /**
     * Show the overlay
     */
    private void showOverlay() {
        mOverlay.setVisibility(LinearLayout.VISIBLE);
    }

    /**
     * Set a text to the Info TextView on the Overlay.
     * 
     * @param text
     * @param show
     */
    private void setInfoText(String text) {
        TextView info = (TextView) mOverlay.findViewById(R.id.info);
        info.setText(text);
    }
    
    @Override
    protected void onResume() {
        mIsDestroyed = false;
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mIsDestroyed = true;
        mDatabase.close();
        super.onDestroy();
    }
    
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null)
            return ni.isConnectedOrConnecting();
        else
            return true;
    }

    private class TableHandler extends DefaultHandler {
        
        private Date mDate;
        private boolean mIsTyp;
        private boolean mIsMeal;
        private boolean mIsPrice;
        private Meal mMeal;
        private List<String> mTyps = new ArrayList<String>();
        private int mBodyCount = -1;
        
        public void setDate(Date date) {
            mDate = date;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            
            if (mIsDestroyed)
                return;
            
            if (localName.equals("td")
                    && attributes.getValue(0).equals("speiseangebotbody")) {
                mBodyCount++;
            } else if (localName.equals("td")
                    && attributes.getValue(0).equals("speiseangebottitel")) {
                mIsTyp = true;
            } else if (localName.equals("span")
                    && attributes.getValue(0).equals("name")) {
                if (mMeal != null && mDatabase != null && !mIsDestroyed) {
                    mDatabase.addMeal(mMeal);
                    mMeals.add(mMeal);
                }
                mMeal = new Meal(mDate);
                mMeal.setTyp(mTyps.get(mBodyCount));
                if (mTyps.get(mBodyCount).equals("Bio"))
                    mMeal.setBio(true);
                mIsMeal = true;
            }

            super.startElement(uri, localName, qName, attributes);
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            
            if (mIsDestroyed)
                return;
            
            String text = new String(ch);
            text = text.substring(start, start + length).trim();

            if (mIsTyp) {
                mTyps.add(text);
                mIsTyp = false;
            } else if (mIsMeal) {
                mMeal.setName(text);
                mIsPrice = true;
                mIsMeal = false;
            } else if (mIsPrice) {
                String[] prices = text.replace(",", ".").split("/");
                if (prices.length >= 1 && !prices[0].equalsIgnoreCase(""))
                    mMeal.setPriceStudent(Float.parseFloat(prices[0]));
                if (prices.length >= 2 && !prices[1].equalsIgnoreCase(""))
                    mMeal.setPriceGuest(Float.parseFloat(prices[1]));
                mIsPrice = false;
            }

            super.characters(ch, start, length);
        }
    }

}
