package barqsoft.footballscores.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.ScoresAdapter;
import barqsoft.footballscores.Utilies;

public class SimpleWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        final int num = appWidgetIds.length;
        for (int i = 0; i < num; i++) {
            int appWidgetId = appWidgetIds[i];

            Date fragmentdate = new Date(System.currentTimeMillis());
            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
            String today[] = {mformat.format(fragmentdate)};
            Cursor cursor = context.getContentResolver().query(DatabaseContract.scores_table.buildScoreWithDate(), null, null,
                    today, null);


            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simple);
            views.setOnClickPendingIntent(R.id.linLayout, pendingIntent);

            if (cursor != null) {
                cursor.moveToFirst();
                views.setTextViewText(R.id.home_name, cursor.getString(ScoresAdapter.COL_HOME));
                views.setTextViewText(R.id.away_name, cursor.getString(ScoresAdapter.COL_AWAY));
                views.setTextViewText(R.id.score_textview, Utilies.getScores(cursor.getInt(ScoresAdapter.COL_HOME_GOALS), cursor.getInt(ScoresAdapter.COL_AWAY_GOALS)));
                views.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName(cursor.getString(ScoresAdapter.COL_HOME), context));
                views.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName(cursor.getString(ScoresAdapter.COL_AWAY), context));
                views.setTextViewText(R.id.data_textview, cursor.getString(ScoresAdapter.COL_MATCHTIME));
            }


            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);

            if (cursor != null) cursor.close();
        }
    }


}
