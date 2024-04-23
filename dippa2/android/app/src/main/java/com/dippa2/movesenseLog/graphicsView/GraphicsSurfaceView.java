package com.dippa2.movesenseLog.graphicsView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;		//Canvas for drawing the data on screen
import android.view.SurfaceHolder;	//Holder to obtain the canvas
import android.view.SurfaceView;
import android.view.View;
import android.util.AttributeSet;

//import android.util.Log;	//Debugging
import android.graphics.Paint;	//Debugging drawing
import android.graphics.Color;	//Debugging drawing
import android.graphics.Path;	//Plot trace
import android.graphics.Rect;
import android.annotation.SuppressLint;
import com.dippa2.movesenseLog.util.*;

@SuppressLint("all")
public class GraphicsSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "GraphicsSurfaceView";

	/*For plotting holder and canvas*/
	protected SurfaceHolder holder;
	protected Canvas canvas;
	protected Paint  mPaint;
	Path path;
	double radius = 30;
	final float testTextSize = 48f;
	protected float[][] data = null;
	protected String[] times = null;
	double normalise = 40.0;
	public String text = null;
	public boolean surfaceCreated = false;


	public GraphicsSurfaceView(Context context) {
		super(context);
		////Log.d(TAG,"constructed1");
		init();

	}
	
	public GraphicsSurfaceView(Context context, AttributeSet attrs) {
		super(context,attrs);
		////Log.d(TAG,"constructed2");
		init();
	}

	public GraphicsSurfaceView(Context context, AttributeSet attrs,int defstyle) {
		super(context,attrs,defstyle);
		////Log.d(TAG,"constructed3");
		init();
	}	

	private void init(){
		getHolder().addCallback(this);
		setFocusable(true); // make sure we get key events
		mPaint = new Paint();
		mPaint.setDither(true);
		//mPaint.setColor(0xFF0000FF);
		mPaint.setColor(0xFFFFFFFF);
		mPaint.setStyle(Paint.Style.STROKE);//STROKE); //
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(3);
		mPaint.setTextAlign(Paint.Align.CENTER);
		mPaint.setTextSize(12f);
		path = new Path();
		/*
		data = new short[500];
		*/
		Paint template = new Paint(mPaint);
		template.setStyle(Paint.Style.FILL_AND_STROKE);
		
	}
	
	public void setTimes(String[] t){
		times = t;
	}
	
	/*call to plot the latest set of data*/
	public void updateData(float[][] data,double normalise,String text){
		this.normalise = normalise;
		this.data = data;
		this.text = text;
	    canvas = null;
	    holder = getHolder();
	    try {
	        canvas = holder.lockCanvas(null);
	        synchronized(holder) {
	            onDraw(canvas);
	        }

	    }catch (Exception err){ 
	    	////Log.d(TAG,"Canvas lock error "+err.toString());
	    	if (holder == null){
	    		////Log.d(TAG,"Holder null "+holder.toString());
	    	}else{
	    		////Log.d(TAG,"Holder not null "+holder.toString());
	    	}
	    }finally {

	        if(canvas != null) {
				////Log.d("tswt","unlockAndPost "+canvas.toString());
	            holder.unlockCanvasAndPost(canvas);
	        }

	    }
	}
	
	/*Draw Canvas for saving a screen capture*/
	public Bitmap getScreenShot(){
		Bitmap bm = Bitmap.createBitmap(800,300,Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bm);
		onDraw(canvas);
		return bm;
		
	}
	
	/*Draw plots here*/
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.BLACK);	//Reset background color
	}
	
	/*SurfaceHolder.Callback*/
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
        int height) {
		// TODO Auto-generated method stub
		this.holder = holder; 
	}

	public void surfaceCreated(SurfaceHolder holder) {
		this.holder = holder; 
		this.surfaceCreated = true;
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

}
