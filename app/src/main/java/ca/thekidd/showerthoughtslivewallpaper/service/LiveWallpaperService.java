package ca.thekidd.showerthoughtslivewallpaper.service;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.SurfaceHolder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import ca.thekidd.showerthoughtslivewallpaper.utils.BitmapUtils;
import ca.thekidd.showerthoughtslivewallpaper.utils.JSONUtils;

public class LiveWallpaperService extends WallpaperService
{
    int x,y;
    Context context;
    SharedPreferences prefs;
    int day = -1;

    public void onCreate()
    {
        super.onCreate();
        context = this.getBaseContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void onDestroy()
    {
        super.onDestroy();
    }

    public Engine onCreateEngine()
    {
        return new MyWallpaperEngine();
    }

    class MyWallpaperEngine extends Engine
    {

        private BitmapWorkerTask syncTask = null;

        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };
        private boolean visible = true;
        public Bitmap background = null;
        public String quote = null, author = null;

        MyWallpaperEngine()
        {
            registerBroadcastReceiver();
            Calendar cal = Calendar.getInstance();
            day = cal.get(Calendar.DAY_OF_YEAR);
            if(syncTask != null)
                syncTask.cancel(true);
            syncTask = new BitmapWorkerTask();
            syncTask.execute();
        }


        public void onCreate(SurfaceHolder surfaceHolder)
        {
            super.onCreate(surfaceHolder);
        }

        @Override
        public Bundle onCommand(String action, int x, int y, int z,
                                Bundle extras, boolean resultRequested)
        {
            if (action.equals("android.wallpaper.tap")) {
                if(syncTask != null)
                    syncTask.cancel(true);
                syncTask = new BitmapWorkerTask();
                syncTask.execute();
            }
            return super.onCommand(action, x, y, z, extras, resultRequested);
        }

        @Override
        public void onVisibilityChanged(boolean visible)
        {
            this.visible = visible;
            // if screen wallpaper is visible then draw the image otherwise do not draw
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder)
        {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels)
        {
            draw();
        }

        void draw()
        {

            //reload each day
            Calendar cal = Calendar.getInstance();
            int newDay = cal.get(Calendar.DAY_OF_YEAR);
            if(day != newDay) {
                day = newDay;
                if(syncTask != null)
                    syncTask.cancel(true);
                syncTask = new BitmapWorkerTask();
                syncTask.execute();
            }

            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try
            {
                if(visible) {
                    c = holder.lockCanvas();
                    // clear the canvas
                    if (c != null) {
                        c.drawColor(Color.BLACK);
                        if (background != null) {
                            c.drawBitmap(background, 0, 0, null);
                        }

                        if (quote != null) {
                            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/font.otf");
                            TextPaint tp = new TextPaint();
                            tp.setColor(Color.WHITE);
                            tp.setTextSize(50);
                            tp.setTypeface(tf);
                            tp.setTextAlign(Paint.Align.CENTER);
                            tp.setAntiAlias(true);
                            StaticLayout sl = new StaticLayout(quote, tp,
                                    c.getWidth() - 60, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

                            KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                            boolean isPhoneLocked = myKM.inKeyguardRestrictedInputMode();
                            if (isPhoneLocked) {
                                c.translate(c.getWidth() / 2, c.getHeight() * prefs.getInt("pref_vertical_offset_lockscreen", 50) / 100);//950);
                            } else {
                                c.translate(c.getWidth() / 2, c.getHeight() * prefs.getInt("pref_vertical_offset", 20) / 100);//350);
                            }
                            sl.draw(c);

                            tp.setStyle(Paint.Style.STROKE);
                            tp.setStrokeWidth(1.2f);
                            tp.setColor(Color.BLACK);
                            sl.draw(c);

                            int height = sl.getHeight();

                            if (author != null) {
                                tp.setStyle(Paint.Style.FILL);
                                tp.setStrokeWidth(0);
                                tp.setColor(Color.WHITE);
                                tp.setTextSize(30);
                                tp.setTextAlign(Paint.Align.RIGHT);
                                sl = new StaticLayout(author, tp,
                                        c.getWidth() - 60, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                                c.translate(c.getWidth() / 2, height + 10);
                                sl.draw(c);

                                tp.setStyle(Paint.Style.STROKE);
                                tp.setStrokeWidth(0.8f);
                                tp.setColor(Color.BLACK);
                                sl.draw(c);
                            }
                        }
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }

            handler.removeCallbacks(drawRunner);
            if (visible) {
                handler.postDelayed(drawRunner, 3600000); // delay 10 mileseconds
            }

        }

        class BitmapWorkerTask extends AsyncTask<Void, Void, Void> {
            private int data = 0;

            public BitmapWorkerTask() {
            }

            // Decode image in background.
            @Override
            protected Void doInBackground(Void...params) {
                try {
                    //Background
                    JSONObject json = JSONUtils.getJSON("https://www.reddit.com/r/earthporn/top.json?sort=top&t=" + prefs.getString("pref_reddit_timespan", "week") + "&limit=100");
                    String imageUrl;
                    int attempts = 0;
                    do {
                        JSONArray posts = json.getJSONObject("data").getJSONArray("children");
                        int rand = (int) Math.floor(Math.random() * posts.length());
                        JSONObject post = posts.getJSONObject(rand).getJSONObject("data");

                        imageUrl = post.getString("url");
                        if (imageUrl.indexOf("imgur.com") > 0 || imageUrl.indexOf("/gallery/") > 0) {
                            if (imageUrl.contains("gifv")) {
                                if (imageUrl.indexOf("i.") == 0) {
                                    imageUrl = imageUrl.replace("imgur.com", "i.imgur.com");
                                }
                                imageUrl = imageUrl.replace(".gifv", ".gif");
                            }
                            if (imageUrl.indexOf("/a/") > 0 || imageUrl.indexOf("/gallery/") > 0) {
                                imageUrl = "";
                            }
                            if(imageUrl.charAt(imageUrl.length()-1) == '/')
                                imageUrl = imageUrl.substring(imageUrl.length()-2);
                            if (!imageUrl.contains(".jpg")) {
                                imageUrl = imageUrl + ".jpg";
                            }
                        }
                        attempts++;
                    } while(imageUrl.contains("flickr") && attempts < 5);//don't use flickr (can't get image url). Try again a few times
                    Log.d("Background", imageUrl);

                    //Quote
                    json = JSONUtils.getJSON("https://www.reddit.com/r/showerthoughts/top.json?sort=top&t=week&limit=100");
                    JSONArray posts = json.getJSONObject("data").getJSONArray("children");
                    int rand = (int) Math.floor(Math.random() * posts.length());
                    JSONObject post = posts.getJSONObject(rand).getJSONObject("data");

                    int height = getSurfaceHolder().getSurfaceFrame().height();
                    Bitmap newBackground = BitmapUtils.loadBitmap(imageUrl, height);
                    if(newBackground != null) {
                        Bitmap oldBackground = background;
                        background = newBackground;
                        if(oldBackground != null)
                            oldBackground.recycle();
                        quote = post.getString("title");
                        author = "/u/" + post.getString("author") + "   ";
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                handler.post(drawRunner);
            }
        }

        private void registerBroadcastReceiver() {
            final IntentFilter theFilter = new IntentFilter();
            /** System Defined Broadcast */
            theFilter.addAction(Intent.ACTION_SCREEN_ON);
            theFilter.addAction(Intent.ACTION_SCREEN_OFF);
            theFilter.addAction(Intent.ACTION_USER_PRESENT);

            BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handler.post(drawRunner);
                }
            };

            getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);
        }
    }
}
