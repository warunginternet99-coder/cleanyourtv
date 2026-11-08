
package com.gathin.tvhome;
 
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
 
/**
 * Kartu jadwal + klasemen Manchester United untuk Home (menggantikan isi racePanel).
 * Data dari ESPN; jika gagal, memakai data contoh per 6 Nov 2026.
 */
public class MuCards extends LinearLayout {
    static final String ESPN_TEAM = "360";
    static final String SCHEDULE = "https://site.api.espn.com/apis/site/v2/sports/soccer/all/teams/360/schedule";
    static final String TABLE = "https://site.api.espn.com/apis/v2/sports/soccer/eng.1/standings";
    static final String CREST = "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/360.png&w=128&h=128";
    static final int GOLD = 0xFFF7D046, MUTED = 0xFFB9B0CC, WHITE = 0xFFFFFFFF;
    static final long REFRESH_MS = 30 * 60 * 1000L;
 
    static class Mt { long t; String home = "?", away = "?"; int hs = -1, as = -1; boolean done; boolean muHome; }
    static class Row { int pos, played, pts; String name = "?"; boolean me; }
 
    final TimeZone wib = TimeZone.getTimeZone("Asia/Jakarta");
    final Locale id = new Locale("id", "ID");
    final float density = getResources().getDisplayMetrics().density;
    List<Mt> matches = new ArrayList<Mt>();
    List<Row> table = new ArrayList<Row>();
    TextView title, countdown, whenTv, lastTv, side;
    LinearLayout formRow, standings;
    ImageView crest;
    long kickoff = 0;
    boolean attached;
 
    public MuCards(Context c) { super(c); init(); }
    public MuCards(Context c, AttributeSet a) { super(c, a); init(); }
 
    int dp(float v) { return Math.round(v * density); }
 
    TextView tv(float sp, int color, boolean bold) {
        TextView t = new TextView(getContext());
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setSingleLine(true);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }
 
    LinearLayout card() {
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(VERTICAL);
        l.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xB3180F28); g.setCornerRadius(dp(14)); g.setStroke(dp(1), 0x28FFFFFF);
        l.setBackground(g);
        return l;
    }
 
    void init() {
        setOrientation(HORIZONTAL);
        setFocusable(false);
        setClipChildren(false);
 
        // Kartu kiri: jadwal
        LinearLayout left = card();
        left.setOrientation(HORIZONTAL);
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(VERTICAL);
        TextView kicker = tv(9, GOLD, true); kicker.setText("PREMIER LEAGUE");
        title = tv(18, WHITE, true); title.setText("Memuat…");
        title.setPadding(0, dp(3), 0, 0);
        countdown = tv(12, GOLD, true);
        whenTv = tv(10, MUTED, false);
        formRow = new LinearLayout(getContext());
        formRow.setOrientation(HORIZONTAL);
        formRow.setGravity(Gravity.CENTER_VERTICAL);
        formRow.setPadding(0, dp(5), 0, dp(5));
        View line = new View(getContext());
        line.setBackgroundColor(0x28FFFFFF);
        lastTv = new TextView(getContext());
        lastTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        lastTv.setTextColor(MUTED);
        lastTv.setIncludeFontPadding(false);
        lastTv.setLineSpacing(0, 1.15f);
        col.addView(kicker); col.addView(title); col.addView(countdown); col.addView(whenTv); col.addView(formRow);
        col.addView(line, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
        LinearLayout.LayoutParams lt = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lt.topMargin = dp(5);
        col.addView(lastTv, lt);
        left.addView(col, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
 
        LinearLayout rightCol = new LinearLayout(getContext());
        rightCol.setOrientation(VERTICAL);
        rightCol.setGravity(Gravity.END);
        side = tv(9, GOLD, true);
        crest = new ImageView(getContext());
        crest.setScaleType(ImageView.ScaleType.FIT_CENTER);
        rightCol.addView(side);
        LayoutParams cl = new LayoutParams(dp(62), dp(62));
        cl.topMargin = dp(6);
        rightCol.addView(crest, cl);
        left.addView(rightCol, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
 
        // Kartu kanan: klasemen
        LinearLayout right = card();
        TextView k2 = tv(9, GOLD, true); k2.setText("KLASEMEN PREMIER LEAGUE");
        standings = new LinearLayout(getContext());
        standings.setOrientation(VERTICAL);
        standings.setPadding(0, dp(4), 0, 0);
        right.addView(k2); right.addView(standings);
 
        LayoutParams lp1 = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
        lp1.rightMargin = dp(12);
        addView(left, lp1);
        addView(right, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        setPlaceholderCrest();
    }
 
    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = getResources().getDisplayMetrics().widthPixels - dp(60 + 56);
        super.onMeasure(MeasureSpec.makeMeasureSpec(Math.max(w, dp(300)), MeasureSpec.EXACTLY), hSpec);
    }
 
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        // Sembunyikan isi lama racePanel (teks satu baris); updatePanel hanya mengubah teks/visibilitasnya.
        ViewParent p = getParent();
        if (p instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) p;
            for (int i = 0; i < g.getChildCount(); i++) if (g.getChildAt(i) != this) g.getChildAt(i).setAlpha(0f);
        }
        loadSnapshot();
        render();
        refresh();
        post(tick);
    }
 
    @Override protected void onDetachedFromWindow() {
        attached = false;
        removeCallbacks(tick);
        removeCallbacks(again);
        super.onDetachedFromWindow();
    }
 
    final Runnable tick = new Runnable() {
        public void run() {
            if (!attached) return;
            if (kickoff > 0) {
                long d = kickoff - System.currentTimeMillis();
                if (d <= 0) countdown.setText("Pertandingan berlangsung / selesai");
                else {
                    long dd = d / 86400000L, hh = d % 86400000L / 3600000L, mm = d % 3600000L / 60000L;
                    countdown.setText("Kick-off dalam " + (dd > 0 ? dd + " hari " : "") + hh + " j " + mm + " mnt");
                }
            }
            postDelayed(this, 30000);
        }
    };
    final Runnable again = new Runnable() { public void run() { if (attached) refresh(); } };
 
    // ---------- Data ----------
 
    SimpleDateFormat utcFmt() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f;
    }
 
    Mt mt(String utc, String h, String a, int hs, int as, boolean done, boolean muHome) {
        Mt m = new Mt();
        try { m.t = utcFmt().parse(utc).getTime(); } catch (Exception e) { }
        m.home = h; m.away = a; m.hs = hs; m.as = as; m.done = done; m.muHome = muHome;
        return m;
    }
 
    Row row(int pos, String name, int played, int pts, boolean me) {
        Row r = new Row(); r.pos = pos; r.name = name; r.played = played; r.pts = pts; r.me = me; return r;
    }
 
    void loadSnapshot() {
        matches = new ArrayList<Mt>();
        matches.add(mt("2026-09-20T15:30Z", "Fulham", "Man United", 1, 1, true, false));
        matches.add(mt("2026-10-10T16:30Z", "Man United", "Tottenham", 1, 1, true, true));
        matches.add(mt("2026-10-18T13:00Z", "Leeds", "Man United", 1, 1, true, false));
        matches.add(mt("2026-10-25T14:00Z", "Man United", "Bournemouth", 0, 3, true, true));
        matches.add(mt("2026-10-31T12:30Z", "Chelsea", "Man United", 2, 0, true, false));
        matches.add(mt("2026-11-08T16:30Z", "Man United", "Aston Villa", -1, -1, false, true));
        matches.add(mt("2026-11-22T16:30Z", "Liverpool", "Man United", -1, -1, false, false));
        matches.add(mt("2026-11-28T15:00Z", "Man United", "Brentford", -1, -1, false, true));
        table = new ArrayList<Row>();
        table.add(row(1, "Arsenal", 9, 24, false)); table.add(row(2, "Man City", 9, 21, false));
        table.add(row(3, "Brighton", 9, 19, false)); table.add(row(4, "Chelsea", 9, 16, false));
        table.add(row(5, "Liverpool", 9, 15, false)); table.add(row(16, "Coventry", 9, 9, false));
        table.add(row(17, "Man United", 9, 7, true)); table.add(row(18, "Crystal Palace", 9, 7, false));
    }
 
    void refresh() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    List<Mt> all = new ArrayList<Mt>();
                    addEvents(all, get(SCHEDULE));
                    try { addEvents(all, get(SCHEDULE + "?fixture=true")); } catch (Exception e) { }
                    final List<Mt> ms = dedupe(all);
                    if (!ms.isEmpty()) post(new Runnable() { public void run() { matches = ms; render(); } });
                } catch (Exception e) { }
                try {
                    final List<Row> rows = parseTable(get(TABLE));
                    if (!rows.isEmpty()) post(new Runnable() { public void run() { table = rows; render(); } });
                } catch (Exception e) { }
                try { loadCrest(); } catch (Exception e) { }
            }
        }).start();
        removeCallbacks(again);
        postDelayed(again, REFRESH_MS);
    }
 
    List<Mt> dedupe(List<Mt> in) {
        List<Mt> out = new ArrayList<Mt>();
        for (Mt m : in) {
            boolean dup = false;
            for (Mt o : out) if (Math.abs(o.t - m.t) < 3600000L && o.home.equals(m.home) && o.away.equals(m.away)) {
                if (m.done && !o.done) { o.done = true; o.hs = m.hs; o.as = m.as; }
                dup = true; break;
            }
            if (!dup) out.add(m);
        }
        return out;
    }
 
    static int scoreOf(JSONObject c) {
        Object s = c.opt("score");
        try {
            if (s instanceof JSONObject) {
                JSONObject o = (JSONObject) s;
                if (o.has("value")) return (int) Math.round(o.getDouble("value"));
                return Integer.parseInt(o.optString("displayValue", "-1"));
            }
            if (s != null && s != JSONObject.NULL) return (int) Math.round(Double.parseDouble(String.valueOf(s)));
        } catch (Exception e) { }
        return -1;
    }
 
    void addEvents(List<Mt> out, String body) throws Exception {
        JSONArray events = new JSONObject(body).getJSONArray("events");
        SimpleDateFormat utc = utcFmt();
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.getJSONObject(i);
            String date = e.optString("date", "");
            JSONArray comps = e.optJSONArray("competitions");
            if (date.length() < 16 || comps == null || comps.length() == 0) continue;
            JSONObject comp = comps.getJSONObject(0);
            JSONArray cs = comp.optJSONArray("competitors");
            if (cs == null) continue;
            Mt m = new Mt();
            boolean haveH = false, haveA = false;
            for (int k = 0; k < cs.length(); k++) {
                JSONObject c = cs.getJSONObject(k);
                JSONObject team = c.optJSONObject("team");
                String nm = team == null ? "?" : team.optString("shortDisplayName", team.optString("displayName", "?"));
                String tid = team == null ? "" : team.optString("id", "");
                if ("home".equals(c.optString("homeAway"))) { m.home = nm; m.hs = scoreOf(c); haveH = true; if (ESPN_TEAM.equals(tid)) m.muHome = true; }
                else if ("away".equals(c.optString("homeAway"))) { m.away = nm; m.as = scoreOf(c); haveA = true; }
            }
            if (!haveH || !haveA) continue;
            JSONObject st = comp.optJSONObject("status");
            if (st == null) st = e.optJSONObject("status");
            JSONObject type = st == null ? null : st.optJSONObject("type");
            m.done = type != null && type.optBoolean("completed", false);
            m.t = utc.parse(date.substring(0, 16) + "Z").getTime();
            out.add(m);
        }
    }
 
    List<Row> parseTable(String body) throws Exception {
        JSONObject o = new JSONObject(body);
        JSONArray ch = o.optJSONArray("children");
        JSONObject std = ch != null ? ch.getJSONObject(0).getJSONObject("standings") : o.getJSONObject("standings");
        JSONArray entries = std.getJSONArray("entries");
        List<Row> rows = new ArrayList<Row>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            JSONObject team = e.getJSONObject("team");
            Row r = new Row();
            r.name = team.optString("shortDisplayName", team.optString("displayName", "?"));
            r.me = ESPN_TEAM.equals(team.optString("id", ""));
            r.pos = i + 1;
            JSONArray stats = e.optJSONArray("stats");
            for (int k = 0; stats != null && k < stats.length(); k++) {
                JSONObject s = stats.getJSONObject(k);
                String n = s.optString("name", "");
                if (n.equals("rank")) r.pos = (int) s.optDouble("value", r.pos);
                else if (n.equals("points")) r.pts = (int) s.optDouble("value", 0);
                else if (n.equals("gamesPlayed")) r.played = (int) s.optDouble("value", 0);
            }
            rows.add(r);
        }
        Collections.sort(rows, new Comparator<Row>() { public int compare(Row a, Row b) { return a.pos - b.pos; } });
        return rows;
    }
 
    String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "cleanyourtv");
        c.setConnectTimeout(15000); c.setReadTimeout(15000);
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }
 
    // ---------- Logo ----------
 
    void setPlaceholderCrest() {
        Bitmap bm = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Path sh = new Path();
        sh.moveTo(28, 20); sh.lineTo(228, 20); sh.lineTo(228, 130);
        sh.quadTo(228, 220, 128, 246); sh.quadTo(28, 220, 28, 130); sh.close();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL); p.setColor(0xFFDA291C); c.drawPath(sh, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(10); p.setColor(0xFFFBE122); c.drawPath(sh, p);
        p.setStyle(Paint.Style.FILL); p.setColor(0xFFFBE122); p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextSize(96);
        c.drawText("MU", 128, 160, p);
        crest.setImageBitmap(bm);
    }
 
    void loadCrest() throws Exception {
        File dir = getContext().getFilesDir();
        File cached = new File(dir, "crest_mu.png");
        File own = null;
        try {
            File ext = getContext().getExternalFilesDir(null);
            if (ext != null) own = new File(ext, "crest.png");
        } catch (Exception e) { }
        File use = null;
        if (own != null && own.exists()) use = own;
        else {
            if (!cached.exists()) {
                HttpURLConnection c = (HttpURLConnection) new URL(CREST).openConnection();
                c.setRequestProperty("User-Agent", "cleanyourtv");
                c.setConnectTimeout(15000); c.setReadTimeout(15000);
                InputStream in = c.getInputStream();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[4096]; int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                in.close();
                FileOutputStream fo = new FileOutputStream(cached);
                fo.write(bo.toByteArray()); fo.close();
            }
            use = cached;
        }
        final Bitmap bm = BitmapFactory.decodeFile(use.getAbsolutePath());
        if (bm != null) post(new Runnable() { public void run() { crest.setImageBitmap(bm); } });
    }
 
    // ---------- Tampilan ----------
 
    void render() {
        Collections.sort(matches, new Comparator<Mt>() { public int compare(Mt a, Mt b) { return a.t < b.t ? -1 : a.t > b.t ? 1 : 0; } });
        SimpleDateFormat out = new SimpleDateFormat("EEEE d MMM HH:mm", id);
        out.setTimeZone(wib);
        long now = System.currentTimeMillis();
        Mt next = null; List<Mt> done = new ArrayList<Mt>(); List<Mt> later = new ArrayList<Mt>();
        for (Mt m : matches) {
            if (m.done) done.add(m);
            else if (m.t > now - 3 * 3600000L) { if (next == null) next = m; else if (later.size() < 2) later.add(m); }
        }
        if (next != null) {
            kickoff = next.t;
            title.setText(next.home + " vs " + next.away);
            whenTv.setText(out.format(new Date(next.t)) + " WIB");
            side.setText(next.muHome ? "KANDANG" : "TANDANG");
        } else { kickoff = 0; title.setText("Tidak ada jadwal"); countdown.setText(""); whenTv.setText(""); side.setText(""); }
 
        formRow.removeAllViews();
        TextView fl = tv(9, MUTED, true); fl.setText("FORM");
        LayoutParams flp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.rightMargin = dp(6);
        formRow.addView(fl, flp);
        for (int i = Math.max(0, done.size() - 5); i < done.size(); i++) {
            Mt m = done.get(i);
            int mine = m.muHome ? m.hs : m.as, theirs = m.muHome ? m.as : m.hs;
            String r = mine > theirs ? "M" : mine == theirs ? "S" : "K";
            TextView c = tv(10, WHITE, true);
            c.setText(r); c.setGravity(Gravity.CENTER);
            GradientDrawable g = new GradientDrawable();
            g.setColor(r.equals("M") ? 0xFF18B56B : r.equals("S") ? 0xFF8896A8 : 0xFFD93A3A);
            g.setCornerRadius(dp(5));
            c.setBackground(g);
            LayoutParams lp = new LayoutParams(dp(20), dp(20));
            lp.rightMargin = dp(4);
            formRow.addView(c, lp);
        }
        StringBuilder sb = new StringBuilder();
        if (!done.isEmpty()) {
            Mt l = done.get(done.size() - 1);
            sb.append("Terakhir: ").append(l.home).append(" ").append(l.hs).append(" – ").append(l.as).append(" ").append(l.away);
        }
        for (Mt m : later) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Lalu: ").append(m.home).append(" vs ").append(m.away).append(" · ").append(out.format(new Date(m.t))).append(" WIB");
        }
        lastTv.setText(sb.toString());
 
        standings.removeAllViews();
        standings.addView(rowView("#", "TIM", "M", "POIN", false, MUTED));
        int me = 1;
        for (Row r : table) if (r.me) me = r.pos;
        int prev = 0;
        for (Row r : table) {
            boolean show = r.pos <= 3 || Math.abs(r.pos - me) <= 1;
            if (!show) continue;
            if (prev != 0 && r.pos - prev > 1) {
                TextView d = tv(10, MUTED, false); d.setText("⋮"); d.setGravity(Gravity.CENTER);
                standings.addView(d);
            }
            standings.addView(rowView(String.valueOf(r.pos), r.name, String.valueOf(r.played), String.valueOf(r.pts), r.me, WHITE));
            prev = r.pos;
        }
        post(tick);
    }
 
    LinearLayout rowView(String a, String b, String c, String d, boolean me, int base) {
        int col = me ? GOLD : base;
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(HORIZONTAL);
        l.setPadding(dp(5), dp(2), dp(5), dp(2));
        TextView ta = tv(11, col, me); ta.setText(a);
        TextView tb = tv(11, col, me); tb.setText(b);
        TextView tc = tv(11, col, me); tc.setText(c); tc.setGravity(Gravity.END);
        TextView td = tv(11, col, me); td.setText(d); td.setGravity(Gravity.END);
        l.addView(ta, new LayoutParams(dp(22), ViewGroup.LayoutParams.WRAP_CONTENT));
        l.addView(tb, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        l.addView(tc, new LayoutParams(dp(26), ViewGroup.LayoutParams.WRAP_CONTENT));
        l.addView(td, new LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
        if (me) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(0x47F7D046); g.setCornerRadius(dp(5));
            l.setBackground(g);
        }
        return l;
    }
}
 
