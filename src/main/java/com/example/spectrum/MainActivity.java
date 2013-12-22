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

public class MainActivity extends Activity {

    private Bitmap bmp = null;
    private final String tag = "spectrum_analyzer";
    private GraphicsView myview;
    private int mInterval = 100;
    private Handler mHandler;
    float touchX = -1;
    float touchY = -1;
    static final int FREQUENCIES_COUNT = 64;
    int[][] aFrequencies = null;
    int currentRow = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(tag, "start!");
        super.onCreate(savedInstanceState);
        myview=new GraphicsView(this); // создаем объект myview класса GraphicsView
        setContentView(myview); // отображаем его в Activity
        mHandler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStatusChecker.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        //Here you can get the size!
        Log.i(tag, "onWindowFocusChanged: width = " + myview.getWidth() + " height = " + myview.getHeight());
        bmp = Bitmap.createBitmap( myview.getWidth(), myview.getHeight(), Bitmap.Config.ARGB_8888);
        aFrequencies = new int[FREQUENCIES_COUNT][myview.getHeight()];
        currentRow = 0;
        Log.i(tag, "aFrequencies.length = " + aFrequencies.length  + " aFrequencies[0].length = " + aFrequencies[0].length);
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
                /*if ((touchX >= 0) && (touchY >= 0)) {
                    Random r = new Random();
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setColor(Color.rgb(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
                    Canvas c = new Canvas(bmp);
                    int rectSize = r.nextInt(30);
                    c.drawRect((int)touchX, (int)touchY, (int)touchX + rectSize, (int)touchY + rectSize, paint);

                    canvas.drawBitmap(bmp, 0, 0, null);

                } else {
                        canvas.drawBitmap(bmp, 0, 0, null);
                }*/
            }
            touchX = touchY = -1;

//            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//            paint.setColor(Color.rgb(61, 61, 61));
//
//            Canvas c = new Canvas(bmp);
//            c.drawCircle((int)touchX, (int)touchY, 10, paint);
//
//            canvas.drawBitmap(bmp, 0, 0, null);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN)
            {
                touchX = event.getX();
                touchY = event.getY();
                invalidate();
            }
            return true;
        }

    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            Random r = new Random();
            if (aFrequencies != null) {
                //here will be reading from mic
                //FFT
                //copy to array of frequencies
                for(int fr = 0; fr < FREQUENCIES_COUNT; fr++) {
                    aFrequencies[fr][currentRow] = r.nextInt(255);
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
                        paint.setColor(Color.rgb(0, aFrequencies[fr][arrayCurrentRow], 0));
                        int rectHeight = 1;
                        int rectWidth = (int) (bmp.getWidth() / FREQUENCIES_COUNT);
                        c.drawRect(fr*rectWidth, y, (fr+1)*rectWidth, y+rectHeight, paint);
                    }
                }
                if ((currentRow+1) == bmp.getHeight()) {
                    currentRow = 0;
                } else {
                    currentRow++;
                }
                myview.invalidate();
            }
            mHandler.postDelayed(mStatusChecker, mInterval);
        }
    };
}
