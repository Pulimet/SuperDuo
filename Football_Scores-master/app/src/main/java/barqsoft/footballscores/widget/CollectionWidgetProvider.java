package barqsoft.footballscores.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;

public class CollectionWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int num = appWidgetIds.length;
        for (int i = 0; i < num; i++) {
            int appWidgetId = appWidgetIds[i];

            // Set up the intent that starts the StackViewService, which will
            // provide the views for this collection.
            Intent intent = new Intent(context, CollectionWidgetService.class);
            // Add the app widget ID to the intent extras.
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            // Get the layout for the App Widget
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_collection);
            remoteViews.setRemoteAdapter(appWidgetId, R.id.listView, intent);
            remoteViews.setEmptyView(R.id.listView, R.id.empty_view);

            Intent intent2 = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent2, 0);
            remoteViews.setPendingIntentTemplate(R.id.listView, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

}
