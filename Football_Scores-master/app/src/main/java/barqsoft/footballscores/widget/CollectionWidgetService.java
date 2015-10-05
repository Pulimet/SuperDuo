package barqsoft.footballscores.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.ScoresAdapter;
import barqsoft.footballscores.Utilies;


public class CollectionWidgetService extends RemoteViewsService {
    public static final String EXTRA_SHARE_DATA = "share_data";
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CollectionRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class CollectionRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {


    private static int mCount = 0;
    private List<String> mWidgetItems = new ArrayList<String>();
    private Context mContext;
    private Cursor mCursor;
    private int mAppWidgetId;

    public CollectionRemoteViewsFactory(Context context, Intent intent) {
        Date fragmentdate = new Date(System.currentTimeMillis());
        SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
        String today[] = {mformat.format(fragmentdate)};
        mCursor = context.getContentResolver().query(DatabaseContract.scores_table.buildScoreWithDate(), null, null, today, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            mCount = mCursor.getCount();
        }

        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        for (int i = 0; i < mCount; i++) {
            mWidgetItems.add("Testing " + i);
        }
    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {
        mCursor.close();
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Log.d("ZAQ", "getViewAt position: " + position);
        // Construct a remote views item based on the app widget item XML file,
        // and set the text based on the position.
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.item_collection_widget);
        if (mCursor != null) {
            mCursor.moveToPosition(position);
            views.setTextViewText(R.id.home_name, mCursor.getString(ScoresAdapter.COL_HOME));
            views.setTextViewText(R.id.away_name, mCursor.getString(ScoresAdapter.COL_AWAY));
            views.setTextViewText(R.id.score_textview, Utilies.getScores(mCursor.getInt(ScoresAdapter.COL_HOME_GOALS), mCursor.getInt(ScoresAdapter.COL_AWAY_GOALS)));
            views.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName(mCursor.getString(ScoresAdapter.COL_HOME), mContext));
            views.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName(mCursor.getString(ScoresAdapter.COL_AWAY), mContext));
            views.setTextViewText(R.id.data_textview, mCursor.getString(ScoresAdapter.COL_MATCHTIME));

            String shareData =
                    mCursor.getString(ScoresAdapter.COL_HOME) + " "
                            + Utilies.getScores(mCursor.getInt(ScoresAdapter.COL_HOME_GOALS), mCursor.getInt(ScoresAdapter.COL_AWAY_GOALS)) + " "
                            + mCursor.getString(ScoresAdapter.COL_AWAY);
            Intent fillInIntent = new Intent();
            Bundle extras = new Bundle();
            extras.putString(CollectionWidgetService.EXTRA_SHARE_DATA, shareData);
            fillInIntent.putExtras(extras);
            views.setOnClickFillInIntent(R.id.linearLayout, fillInIntent);
        }

        // Return the remote views object.
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

}
