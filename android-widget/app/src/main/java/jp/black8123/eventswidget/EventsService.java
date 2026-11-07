package jp.black8123.eventswidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class EventsService extends RemoteViewsService {
    static final String DATA_URL = "https://black8123.github.io/widget-data/events_mobile.json";
    static final long STALE_MS = 10 * 60 * 1000L;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    /** 今日以降の日だけを返す */
    static List<JSONObject> days(String json) {
        List<JSONObject> out = new ArrayList<>();
        if (json == null) return out;
        try {
            JSONArray arr = new JSONObject(json).getJSONArray("days");
            String today = LocalDate.now(ZoneId.of("Asia/Tokyo")).toString();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject d = arr.getJSONObject(i);
                if (d.getString("date").compareTo(today) >= 0) out.add(d);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static List<String> labels(String json) {
        List<String> l = new ArrayList<>();
        for (JSONObject d : days(json)) {
            l.add(d.optString("label"));
            if (l.size() >= 4) break;
        }
        return l;
    }

    static class Row {
        boolean header;
        String l1 = "";
        String l2 = "";
        String tag = "";
        String url = "";
    }

    static class Factory implements RemoteViewsFactory {
        private final Context ctx;
        private List<Row> rows = new ArrayList<>();

        Factory(Context c) {
            ctx = c;
        }

        @Override public void onCreate() { }
        @Override public void onDestroy() { }

        @Override
        public void onDataSetChanged() {
            SharedPreferences sp = ctx.getSharedPreferences("ev", Context.MODE_PRIVATE);
            String json = sp.getString("json", null);
            long ts = sp.getLong("ts", 0);
            boolean force = sp.getBoolean("force", false);
            long now = System.currentTimeMillis();
            if (json == null || force || now - ts > STALE_MS) {
                try {
                    json = fetch();
                    sp.edit().putString("json", json).putLong("ts", now).putBoolean("force", false).apply();
                } catch (Exception e) {
                    sp.edit().putBoolean("force", false).apply();
                }
            }
            if (json == null) {
                rows = message("データを読み込めませんでした。右上の ↻ で再読み込みできます。");
            } else {
                rows = build(json, sp.getInt("sel", 0));
            }
            AppWidgetManager m = AppWidgetManager.getInstance(ctx);
            int[] ids = m.getAppWidgetIds(new ComponentName(ctx, EventsWidgetProvider.class));
            EventsWidgetProvider.applyTabs(ctx, m, ids);
        }

        private String fetch() throws Exception {
            URL u = new URL(DATA_URL + "?t=" + System.currentTimeMillis());
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setUseCaches(false);
            try (InputStream in = con.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8");
            } finally {
                con.disconnect();
            }
        }

        private List<Row> message(String s) {
            List<Row> l = new ArrayList<>();
            Row r = new Row();
            r.header = true;
            r.l1 = s;
            l.add(r);
            return l;
        }

        private List<Row> build(String json, int sel) {
            List<JSONObject> ds = days(json);
            if (ds.isEmpty()) {
                return message("今週末の分は更新待ちです。毎週月曜に更新されます。");
            }
            int idx = Math.max(0, Math.min(sel, Math.min(ds.size(), 4) - 1));
            List<Row> l = new ArrayList<>();
            try {
                JSONArray evs = ds.get(idx).getJSONArray("events");
                for (int k = 0; k < evs.length(); k++) {
                    JSONObject e = evs.getJSONObject(k);
                    Row r = new Row();
                    r.l1 = e.optString("title");
                    String time = e.optString("time");
                    String place = e.optString("place");
                    String area = e.optString("area");
                    String note = e.optString("note");
                    StringBuilder sb = new StringBuilder();
                    if (!time.isEmpty()) sb.append(time).append("  ");
                    sb.append(place);
                    if (!area.isEmpty()) sb.append(" ・").append(area);
                    if (!note.isEmpty()) sb.append("  ※").append(note);
                    r.l2 = sb.toString();
                    r.tag = e.optString("tag");
                    r.url = e.optString("url");
                    l.add(r);
                }
            } catch (Exception ignored) { }
            if (l.isEmpty()) return message("この日のイベントはありません。");
            return l;
        }

        private int tagColor(String t) {
            switch (t) {
                case "行事": return Color.parseColor("#E58A5B");
                case "展覧会": return Color.parseColor("#8D9EE0");
                case "夜": return Color.parseColor("#B496E8");
                case "自然": return Color.parseColor("#6FCF8C");
                case "体験": return Color.parseColor("#E0B84A");
                case "グルメ": return Color.parseColor("#EE7F95");
                default: return Color.parseColor("#AEBBB5");
            }
        }

        @Override public int getCount() { return rows.size(); }

        @Override
        public RemoteViews getViewAt(int pos) {
            if (pos < 0 || pos >= rows.size()) return null;
            Row r = rows.get(pos);
            if (r.header) {
                RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.item_day);
                rv.setTextViewText(R.id.day_label, r.l1);
                return rv;
            }
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.item_event);
            rv.setTextViewText(R.id.tag, r.tag);
            rv.setTextColor(R.id.tag, tagColor(r.tag));
            rv.setTextViewText(R.id.title, r.l1);
            rv.setTextViewText(R.id.sub, r.l2);
            if (!r.url.isEmpty()) {
                Intent fill = new Intent();
                fill.setData(Uri.parse(r.url));
                rv.setOnClickFillInIntent(R.id.row, fill);
            }
            return rv;
        }

        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public long getItemId(int pos) { return pos; }
        @Override public boolean hasStableIds() { return true; }
    }
}
