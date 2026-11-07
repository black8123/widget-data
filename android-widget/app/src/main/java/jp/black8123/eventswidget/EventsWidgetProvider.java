package jp.black8123.eventswidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

public class EventsWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "jp.black8123.eventswidget.REFRESH";
    public static final String ACTION_TAB = "jp.black8123.eventswidget.TAB";
    public static final String EXTRA_INDEX = "index";
    static final int[] TAB_IDS = {R.id.tab0, R.id.tab1, R.id.tab2, R.id.tab3};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        c.getSharedPreferences("ev", Context.MODE_PRIVATE).edit().putBoolean("force", true).apply();
        for (int id : ids) {
            update(c, m, id);
        }
        applyTabs(c, m, ids);
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

        for (int i = 0; i < TAB_IDS.length; i++) {
            Intent t = new Intent(c, EventsWidgetProvider.class);
            t.setAction(ACTION_TAB);
            t.putExtra(EXTRA_INDEX, i);
            t.setData(Uri.parse("tab://" + i));
            rv.setOnClickPendingIntent(TAB_IDS[i], PendingIntent.getBroadcast(c, 10 + i, t,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        m.updateAppWidget(id, rv);
    }

    /** タブの文字・選択状態を、保存済みのデータから反映する */
    static void applyTabs(Context c, AppWidgetManager m, int[] ids) {
        if (ids == null || ids.length == 0) return;
        SharedPreferences sp = c.getSharedPreferences("ev", Context.MODE_PRIVATE);
        List<String> labels = EventsService.labels(sp.getString("json", null));
        int sel = Math.min(sp.getInt("sel", 0), Math.max(0, labels.size() - 1));
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget);
        for (int i = 0; i < TAB_IDS.length; i++) {
            if (i < labels.size()) {
                rv.setViewVisibility(TAB_IDS[i], View.VISIBLE);
                rv.setTextViewText(TAB_IDS[i], labels.get(i));
                boolean on = i == sel;
                rv.setInt(TAB_IDS[i], "setBackgroundResource", on ? R.drawable.tab_sel : R.drawable.tab_bg);
                rv.setTextColor(TAB_IDS[i], on ? 0xFF10201A : 0xFFD0D8D4);
            } else {
                rv.setViewVisibility(TAB_IDS[i], View.GONE);
            }
        }
        m.partiallyUpdateAppWidget(ids, rv);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (ACTION_REFRESH.equals(a) || ACTION_TAB.equals(a)) {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, EventsWidgetProvider.class));
            SharedPreferences.Editor ed = c.getSharedPreferences("ev", Context.MODE_PRIVATE).edit();
            if (ACTION_REFRESH.equals(a)) {
                ed.putBoolean("force", true);
            } else {
                ed.putInt("sel", i.getIntExtra(EXTRA_INDEX, 0));
            }
            ed.apply();
            applyTabs(c, m, ids);
            m.notifyAppWidgetViewDataChanged(ids, R.id.list);
        }
    }
}
