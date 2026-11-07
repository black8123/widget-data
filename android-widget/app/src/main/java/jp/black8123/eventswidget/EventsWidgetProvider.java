package jp.black8123.eventswidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class EventsWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "jp.black8123.eventswidget.REFRESH";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) {
            update(c, m, id);
        }
        m.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    static void update(Context c, AppWidgetManager m, int id) {
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget);
        Intent svc = new Intent(c, EventsService.class);
        svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setRemoteAdapter(R.id.list, svc);
        rv.setEmptyView(R.id.list, R.id.empty);

        Intent open = new Intent(c, OpenActivity.class);
        PendingIntent tpl = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        rv.setPendingIntentTemplate(R.id.list, tpl);

        Intent r = new Intent(c, EventsWidgetProvider.class);
        r.setAction(ACTION_REFRESH);
        rv.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 0, r,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        m.updateAppWidget(id, rv);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        if (ACTION_REFRESH.equals(i.getAction())) {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, EventsWidgetProvider.class));
            m.notifyAppWidgetViewDataChanged(ids, R.id.list);
        }
    }
}
