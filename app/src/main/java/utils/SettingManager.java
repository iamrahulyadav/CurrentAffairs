package utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by bunny on 16/10/17.
 */

public class SettingManager {



    public static void setTextSize(Context mContext , int size) {
        SharedPreferences prefs = mContext.getSharedPreferences("settings", 0);


        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter

        editor.putInt("textSize", size);



        editor.apply();
    }

    public static int getTextSize(Context mContext ) {
        SharedPreferences prefs = mContext.getSharedPreferences("settings", 0);

        return prefs.getInt("textSize", 14);

    }

    public static void setLastUpdatedTime(Context mContext, long timeInMillis){
        SharedPreferences prefs = mContext.getSharedPreferences("settings", 0);


        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter

        editor.putLong("lastUpdated", timeInMillis);



        editor.apply();
    }

    public static long getLastUpdatedTime(Context mContext){
        SharedPreferences prefs = mContext.getSharedPreferences("settings", 0);

        return prefs.getLong("lastUpdated", System.currentTimeMillis());

    }

    public static void setNightMode(Context mContext , boolean nightMode) {
        SharedPreferences prefs = mContext.getSharedPreferences("settings", 0);


        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter

        editor.putBoolean("nightmode", nightMode);



        editor.apply();
    }

    public static boolean getNightMode(Context mContext ) {
        SharedPreferences prefs = mContext.getSharedPreferences("settings", 0);

        return prefs.getBoolean("nightmode", false);

    }


}
