package com.example.spectrum;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.Log;
import java.util.Random;
import android.os.Handler;
import java.lang.Math;
import android.media.*;

public class MainActivity extends Activity {

    private Bitmap bmp = null;
    private final String tag = "spectrum_analyzer";
    private GraphicsView myview;
    private int mInterval = 100;
    private Handler mHandler;
    private float touchX = -1;
    private float touchY = -1;
    private static final int FREQUENCIES_COUNT = 64;
    private double[][] aFrequencies = null;
    private int currentRow = 0;
    private FFT fft;
    private AudioRecorder audioRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(tag, "start!");
        super.onCreate(savedInstanceState);
        myview=new GraphicsView(this); // создаем объект myview класса GraphicsView
        setContentView(myview); // отображаем его в Activity
        mHandler = new Handler();
        fft = new FFT(FREQUENCIES_COUNT);
        audioRecorder = new AudioRecorder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStatusChecker.run();
        audioRecorder.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mStatusChecker);
        audioRecorder.stop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        //Here you can get the size!
        Log.i(tag, "onWindowFocusChanged: width = " + myview.getWidth() + " height = " + myview.getHeight());
        bmp = Bitmap.createBitmap( myview.getWidth(), myview.getHeight(), Bitmap.Config.ARGB_8888);
        aFrequencies = new double[FREQUENCIES_COUNT][myview.getHeight()];
        currentRow = 0;
        Log.i(tag, "aFrequencies.length = " + aFrequencies.length  + " aFrequencies[0].length = " + aFrequencies[0].length);
    }

    public int getColorByValue(double value) {
        int green = (int) (Math.log10(value) * 10);
        if (green > 255) {
            green = 255;
        }
        if (green < 0) {
            green = 0;
        }
        return Color.rgb(0, green, 0);
    }
    public class GraphicsView extends View
    {

        public GraphicsView(Context context) { super(context); }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            /*Bitmap myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            canvas.drawBitmap(myBitmap, touchX, touchY, null);*/

            if (bmp != null) {
                canvas.drawBitmap(bmp, 0, 0, null);
            }
            touchX = touchY = -1;
        }
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            Random r = new Random();
            if (aFrequencies != null) {
                //here will be reading from mic
                //FFT
                byte[] buffer = audioRecorder.getBuffer();
                double[] x = new double[FREQUENCIES_COUNT];
                double[] y = new double[FREQUENCIES_COUNT];
                for(int fr = 0; fr < FREQUENCIES_COUNT; fr++) {
                    x[fr] = buffer[fr];
                }
                fft.fft(x, y);
                //copy to array of frequencies
                for(int fr = 0; fr < FREQUENCIES_COUNT; fr++) {
                    aFrequencies[fr][currentRow] = y[fr];
                }
            }
            if (bmp != null && aFrequencies != null) {
                Canvas c = new Canvas(bmp);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                //bmp.setPixel(r.nextInt(bmp.getWidth()), r.nextInt(bmp.getWidth()),  Color.argb(255, r.nextInt(255), r.nextInt(255), r.nextInt(255)));
                for(int y = 0; y < aFrequencies[0].length; y++) {
                    int arrayCurrentRow = y + currentRow - aFrequencies[0].length + 1;
                    if (arrayCurrentRow < 0) {
                        arrayCurrentRow = y + currentRow + 1;
                    }
                    for(int fr = 0; fr < FREQUENCIES_COUNT; fr++) {
                        paint.setColor(getColorByValue(aFrequencies[fr][arrayCurrentRow]));
                        int rectHeight = 1;
                        int rectWidth = (int) (bmp.getWidth() / FREQUENCIES_COUNT);
                        c.drawRect(fr*rectWidth, y, (fr+1)*rectWidth, y+rectHeight, paint);
                    }
                }
                if ((currentRow+1) >= aFrequencies[0].length) {
                    currentRow = 0;
                } else {
                    currentRow++;
                }
                myview.invalidate();
            }
            mHandler.postDelayed(mStatusChecker, mInterval);
        }
    };


    public class FFT {

        int n, m;

        // Lookup tables. Only need to recompute when size of FFT changes.
        double[] cos;
        double[] sin;

        public FFT(int n) {
            this.n = n;
            this.m = (int) (Math.log(n) / Math.log(2));

            // Make sure n is a power of 2
            if (n != (1 << m))
                throw new RuntimeException("FFT length must be power of 2");

            // precompute tables
            cos = new double[n / 2];
            sin = new double[n / 2];

            for (int i = 0; i < n / 2; i++) {
                cos[i] = Math.cos(-2 * Math.PI * i / n);
                sin[i] = Math.sin(-2 * Math.PI * i / n);
            }

        }

        public void fft(double[] x, double[] y) {
            int i, j, k, n1, n2, a;
            double c, s, t1, t2;

            // Bit-reverse
            j = 0;
            n2 = n / 2;
            for (i = 1; i < n - 1; i++) {
                n1 = n2;
                while (j >= n1) {
                    j = j - n1;
                    n1 = n1 / 2;
                }
                j = j + n1;

                if (i < j) {
                    t1 = x[i];
                    x[i] = x[j];
                    x[j] = t1;
                    t1 = y[i];
                    y[i] = y[j];
                    y[j] = t1;
                }
            }

            // FFT
            n1 = 0;
            n2 = 1;

            for (i = 0; i < m; i++) {
                n1 = n2;
                n2 = n2 + n2;
                a = 0;

                for (j = 0; j < n1; j++) {
                    c = cos[a];
                    s = sin[a];
                    a += 1 << (m - i - 1);

                    for (k = j; k < n; k = k + n2) {
                        t1 = c * x[k + n1] - s * y[k + n1];
                        t2 = s * x[k + n1] + c * y[k + n1];
                        x[k + n1] = x[k] - t1;
                        y[k + n1] = y[k] - t2;
                        x[k] = x[k] + t1;
                        y[k] = y[k] + t2;
                    }
                }
            }
        }
    }


}
