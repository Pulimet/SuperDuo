package it.jaschke.alexandria;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Alexey on 06.10.2015.
 */
public class ShPref {
    private static final String DATA = "data001";


    public static void setFileUri(Context ctx, String fileUri) {
        SharedPreferences settings = ctx.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("file_uri", fileUri);
        editor.apply();
    }

    public static String getFileUri(Context ctx) {
        SharedPreferences settings = ctx.getSharedPreferences(DATA, Context.MODE_PRIVATE);
        return settings.getString("file_uri", "");
    }
}
