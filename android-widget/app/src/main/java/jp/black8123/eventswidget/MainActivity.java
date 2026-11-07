package jp.black8123.eventswidget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setPadding(48, 96, 48, 48);
        t.setTextSize(16);
        t.setText("週末イベントのウィジェットです。\nホーム画面を長押しして「ウィジェット」から「週末イベント」を追加してください。");
        setContentView(t);
    }
}
