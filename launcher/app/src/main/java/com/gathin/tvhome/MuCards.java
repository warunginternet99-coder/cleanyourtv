package com.gathin.tvhome;
 
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
 
/**
 * Kartu jadwal + klasemen Manchester United untuk Home (mengisi racePanel).
 * Peluncur tidak punya izin internet: datanya dibaca dari add-on MU Games (com.handica.mugames).
 * Logo: crest.png di folder data peluncur jika ada, kalau tidak perisai sederhana.
 */
public class MuCards extends LinearLayout {
    static final String AUTH = "content://com.handica.mugames/";
    static final int GOLD = 0xFFF7D046, MUTED = 0xFFB9B0CC, WHITE = 0xFFFFFFFF;
 
    static class Mt { long t; String home = "?", away = "?", lg = "Premier League"; int hs = -1, as = -1; boolean done, muHome; }
    static class Row { int pos, played, pts; String name = "?"; boolean me; }
 
    final TimeZone wib = TimeZone.getTimeZone("Asia/Jakarta");
    final Locale id = new Locale("id", "ID");
    final float density = getResources().getDisplayMetrics().density;
    final Handler main = new Handler(Looper.getMainLooper());
    List<Mt> matches = new ArrayList<Mt>();
    List<Row> table = new ArrayList<Row>();
    String city = "", temp = "", wdesc = "";
    TextView kickerTv, title, countdown, whenTv, lastHead, lastLines, side, weatherTv;
    LinearLayout formRow, standings;
    ImageView crest;
    long kickoff = 0;
    boolean attached;
    ContentObserver observer;
 
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
        l.setPadding(dp(18), dp(12), dp(18), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xB3180F28); g.setCornerRadius(dp(20)); g.setStroke(dp(1), 0x33FFFFFF);
        l.setBackground(g);
        return l;
    }
 
    void init() {
        setOrientation(HORIZONTAL);
        setFocusable(false);
        setClipChildren(false);
 
        // ===== Kartu kiri: jadwal =====
        LinearLayout left = card();
        left.setOrientation(HORIZONTAL);
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(VERTICAL);
 
        LinearLayout kRow = new LinearLayout(getContext());
        kRow.setOrientation(HORIZONTAL);
        kRow.setGravity(Gravity.CENTER_VERTICAL);
        View bar = new View(getContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GOLD); bg.setCornerRadius(dp(2));
        bar.setBackground(bg);
        LayoutParams barLp = new LayoutParams(dp(4), dp(14));
        barLp.rightMargin = dp(9);
        kRow.addView(bar, barLp);
        kickerTv = tv(11, WHITE, true);
        kickerTv.setText("PREMIER LEAGUE");
        kickerTv.setLetterSpacing(0.12f);
        kRow.addView(kickerTv);
 
        title = tv(22, WHITE, false); title.setText("Memuat…");
        title.setPadding(0, dp(5), 0, 0);
        countdown = tv(15, GOLD, true);
        countdown.setPadding(0, dp(4), 0, 0);
        whenTv = tv(12.5f, MUTED, false);
        whenTv.setPadding(0, dp(2), 0, 0);
 
        formRow = new LinearLayout(getContext());
        formRow.setOrientation(HORIZONTAL);
        formRow.setGravity(Gravity.CENTER_VERTICAL);
        formRow.setPadding(0, dp(6), 0, dp(6));
 
        View line = new View(getContext());
        line.setBackgroundColor(0x33FFFFFF);
        lastHead = tv(13, WHITE, true);
        lastHead.setPadding(0, dp(6), 0, 0);
        lastLines = new TextView(getContext());
        lastLines.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        lastLines.setTextColor(MUTED);
        lastLines.setIncludeFontPadding(false);
        lastLines.setLineSpacing(0, 1.15f);
        lastLines.setPadding(0, dp(3), 0, 0);
 
        col.addView(kRow); col.addView(title); col.addView(countdown); col.addView(whenTv); col.addView(formRow);
        col.addView(line, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
        col.addView(lastHead); col.addView(lastLines);
        left.addView(col, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
 
        LinearLayout rightCol = new LinearLayout(getContext());
        rightCol.setOrientation(VERTICAL);
        rightCol.setGravity(Gravity.END);
        side = tv(12, GOLD, true);
        side.setLetterSpacing(0.1f);
        crest = new ImageView(getContext());
        crest.setScaleType(ImageView.ScaleType.FIT_CENTER);
        rightCol.addView(side);
        LayoutParams cl = new LayoutParams(dp(72), dp(72));
        cl.topMargin = dp(10);
        rightCol.addView(crest, cl);
        left.addView(rightCol, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
 
        // ===== Kartu kanan: klasemen =====
        LinearLayout right = card();
        LinearLayout wRow = new LinearLayout(getContext());
        wRow.setOrientation(HORIZONTAL);
        wRow.setGravity(Gravity.CENTER_VERTICAL);
        View sq = new View(getContext());
        GradientDrawable sg = new GradientDrawable();
        sg.setColor(GOLD); sg.setCornerRadius(dp(2));
        sq.setBackground(sg);
        LayoutParams sqLp = new LayoutParams(dp(8), dp(8));
        sqLp.rightMargin = dp(9);
        wRow.addView(sq, sqLp);
        weatherTv = tv(11, WHITE, true);
        weatherTv.setLetterSpacing(0.04f);
        wRow.addView(weatherTv);
        TextView k2 = tv(15, WHITE, true);
        k2.setText("KLASEMEN PREMIER LEAGUE");
        k2.setLetterSpacing(0.1f);
        k2.setPadding(0, dp(5), 0, dp(4));
        standings = new LinearLayout(getContext());
        standings.setOrientation(VERTICAL);
        right.addView(wRow); right.addView(k2); right.addView(standings);
 
        // Lebar kiri:kanan mengikuti foto contoh (sekitar 1,85 : 1); tinggi kedua kartu sama.
        LayoutParams lp1 = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.85f);
        lp1.rightMargin = dp(22);
        addView(left, lp1);
        addView(right, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
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
        loadOwnCrest();
        observer = new ContentObserver(main) {
            @Override public void onChange(boolean selfChange) { reload(); }
        };
        try {
            for (String s : new String[]{"matches", "table", "weather"})
                getContext().getContentResolver().registerContentObserver(Uri.parse(AUTH + s), true, observer);
        } catch (Exception e) { }
        reload();
        post(tick);
        postDelayed(poll, 60000);
    }
 
    @Override protected void onDetachedFromWindow() {
        attached = false;
        removeCallbacks(tick);
        removeCallbacks(poll);
        try { if (observer != null) getContext().getContentResolver().unregisterContentObserver(observer); } catch (Exception e) { }
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
                    countdown.setText(dd > 0 ? "Kick-off dalam " + dd + " hari " + hh + "j" : "Kick-off dalam " + hh + "j " + mm + "m");
                }
            }
            postDelayed(this, 30000);
        }
    };
    final Runnable poll = new Runnable() {
        public void run() { if (attached) { reload(); postDelayed(this, 300000); } }
    };
 
    // ---------- Data dari add-on ----------
 
    void reload() {
        new Thread(new Runnable() {
            public void run() {
                final List<Mt> ms = new ArrayList<Mt>();
                final List<Row> rows = new ArrayList<Row>();
                final String[] w = {"", "", ""};
                Cursor c = null;
                try {
                    c = getContext().getContentResolver().query(Uri.parse(AUTH + "matches"), null, null, null, null);
                    while (c != null && c.moveToNext()) {
                        Mt m = new Mt();
                        m.t = c.getLong(0); m.home = c.getString(1); m.away = c.getString(2);
                        m.hs = c.getInt(3); m.as = c.getInt(4); m.done = c.getInt(5) == 1; m.muHome = c.getInt(6) == 1;
                        if (c.getColumnCount() > 7 && c.getString(7) != null && c.getString(7).length() > 0) m.lg = c.getString(7);
                        ms.add(m);
                    }
                } catch (Exception e) { ms.clear(); } finally { if (c != null) c.close(); }
                try {
                    c = getContext().getContentResolver().query(Uri.parse(AUTH + "table"), null, null, null, null);
                    while (c != null && c.moveToNext()) {
                        Row r = new Row();
                        r.pos = c.getInt(0); r.name = c.getString(1); r.played = c.getInt(2); r.pts = c.getInt(3); r.me = c.getInt(4) == 1;
                        rows.add(r);
                    }
                } catch (Exception e) { rows.clear(); } finally { if (c != null) c.close(); }
                try {
                    c = getContext().getContentResolver().query(Uri.parse(AUTH + "weather"), null, null, null, null);
                    if (c != null && c.moveToFirst()) { w[0] = c.getString(0); w[1] = c.getString(1); w[2] = c.getString(2); }
                } catch (Exception e) { } finally { if (c != null) c.close(); }
                main.post(new Runnable() {
                    public void run() {
                        if (!attached) return;
                        if (!ms.isEmpty()) matches = ms;
                        if (!rows.isEmpty()) table = rows;
                        city = w[0]; temp = w[1]; wdesc = w[2];
                        render();
                    }
                });
            }
        }).start();
    }
 
    // ---------- Data awal (jika add-on belum terpasang) ----------
 
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
 
    /** crest.png buatanmu di /sdcard/Android/data/com.gathin.tvhome/files/ menggantikan perisai sederhana. */
    void loadOwnCrest() {
        try {
            File ext = getContext().getExternalFilesDir(null);
            if (ext == null) return;
            File f = new File(ext, "crest.png");
            if (!f.exists()) return;
            Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (bm != null) crest.setImageBitmap(bm);
        } catch (Exception e) { }
    }
 
    // ---------- Tampilan ----------
 
    String opp(Mt m) { return m.muHome ? m.away : m.home; }
 
    void render() {
        Collections.sort(matches, new Comparator<Mt>() { public int compare(Mt a, Mt b) { return a.t < b.t ? -1 : a.t > b.t ? 1 : 0; } });
        SimpleDateFormat full = new SimpleDateFormat("EEEE d MMM HH:mm", id); full.setTimeZone(wib);
        SimpleDateFormat shortF = new SimpleDateFormat("EEE d MMM HH:mm", id); shortF.setTimeZone(wib);
        long now = System.currentTimeMillis();
        Mt next = null; List<Mt> done = new ArrayList<Mt>(); List<Mt> later = new ArrayList<Mt>();
        for (Mt m : matches) {
            if (m.done) done.add(m);
            else if (m.t > now - 3 * 3600000L) { if (next == null) next = m; else if (later.size() < 2) later.add(m); }
        }
        if (next != null) {
            kickoff = next.t;
            kickerTv.setText(next.lg.toUpperCase(id));
            title.setText(next.home + " vs " + next.away);
            whenTv.setText(full.format(new Date(next.t)) + " WIB");
            side.setText(next.muHome ? "KANDANG" : "TANDANG");
        } else { kickoff = 0; title.setText("Tidak ada jadwal"); countdown.setText(""); whenTv.setText(""); side.setText(""); }
 
        formRow.removeAllViews();
        TextView fl = tv(12, MUTED, true); fl.setText("FORM"); fl.setLetterSpacing(0.08f);
        LayoutParams flp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.rightMargin = dp(10);
        formRow.addView(fl, flp);
        for (int i = Math.max(0, done.size() - 5); i < done.size(); i++) {
            Mt m = done.get(i);
            int mine = m.muHome ? m.hs : m.as, theirs = m.muHome ? m.as : m.hs;
            String r = mine > theirs ? "M" : mine == theirs ? "S" : "K";
            TextView c = tv(13, WHITE, true);
            c.setText(r); c.setGravity(Gravity.CENTER);
            GradientDrawable g = new GradientDrawable();
            g.setColor(r.equals("M") ? 0xFF18B56B : r.equals("S") ? 0xFF8896A8 : 0xFFD93A3A);
            g.setCornerRadius(dp(6));
            c.setBackground(g);
            LayoutParams lp = new LayoutParams(dp(26), dp(26));
            lp.rightMargin = dp(5);
            formRow.addView(c, lp);
        }
        if (!done.isEmpty()) {
            Mt l = done.get(done.size() - 1);
            lastHead.setText("Terakhir: " + l.home + " " + l.hs + " - " + l.as + " " + l.away);
        } else lastHead.setText("");
        StringBuilder sb = new StringBuilder();
        for (Mt m : later) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(m.muHome ? "vs " : "di ").append(opp(m)).append(" · ").append(shortF.format(new Date(m.t))).append(" WIB");
        }
        lastLines.setText(sb.toString());
 
        String wtxt = city;
        if (temp != null && temp.length() > 0) wtxt += " · " + temp;
        if (wdesc != null && wdesc.length() > 0) wtxt += " · " + wdesc;
        weatherTv.setText(wtxt);
 
        standings.removeAllViews();
        standings.addView(rowView("#", "TIM", "M", "POIN", false, MUTED, 11, false));
        int me = 1;
        for (Row r : table) if (r.me) me = r.pos;
        int prev = 0;
        for (Row r : table) {
            boolean show = r.pos <= 2 || Math.abs(r.pos - me) <= 1;
            if (!show) continue;
            if (prev != 0 && r.pos - prev > 1) {
                View gap = new View(getContext());
                standings.addView(gap, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5)));
            }
            standings.addView(rowView(String.valueOf(r.pos), r.name, String.valueOf(r.played), String.valueOf(r.pts), r.me, WHITE, 14, true));
            prev = r.pos;
        }
        post(tick);
    }
 
    LinearLayout rowView(String a, String b, String c, String d, boolean me, int base, float sp, boolean bold) {
        int col = me ? GOLD : base;
        LinearLayout l = new LinearLayout(getContext());
        l.setOrientation(HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(dp(8), dp(3), dp(8), dp(3));
        TextView ta = tv(sp, col, bold); ta.setText(a);
        TextView tb = tv(sp, col, bold); tb.setText(b);
        TextView tc = tv(sp, col, bold); tc.setText(c); tc.setGravity(Gravity.END);
        TextView td = tv(sp, col, bold); td.setText(d); td.setGravity(Gravity.END);
        l.addView(ta, new LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT));
        l.addView(tb, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        l.addView(tc, new LayoutParams(dp(34), ViewGroup.LayoutParams.WRAP_CONTENT));
        l.addView(td, new LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));
        if (me) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(0x66F7D046); g.setCornerRadius(dp(8));
            l.setBackground(g);
        }
        return l;
    }
}
 
