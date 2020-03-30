package org.carbonrom.IMSWidget;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.RemoteViews;

import java.util.Calendar;

public class IMSWidgetProvider extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        boolean permissionsGranted = true;
        for (String permission : MainActivity.requiredPermissions) {
            if (permissionsGranted) {
                permissionsGranted = context.checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED;
            }
        }
        if (!permissionsGranted) {
            Intent mainActivity = new Intent(context, MainActivity.class);
            context.startActivity(mainActivity);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.ims_widget);
            remoteViews.setTextViewText(R.id.ims_status, MainActivity.getIMSStatusString(context));
            Calendar calendar = Calendar.getInstance();
            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);
            int seconds = calendar.get(Calendar.SECOND);
            String updateTime = context.getResources().getString(R.string.last_updated,hours,minutes,seconds);
            remoteViews.setTextViewText(R.id.update_time, updateTime);

            Intent intent = new Intent(context, IMSWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.ims_status, pendingIntent);
            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingMainActivity =
                    PendingIntent.getActivity(context, 0, mainActivityIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.update_time, pendingMainActivity);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }
}
