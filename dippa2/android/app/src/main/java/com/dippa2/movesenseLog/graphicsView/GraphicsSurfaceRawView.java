package com.dippa2.movesenseLog.graphicsView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;		//Canvas for drawing the data on screen
import android.view.SurfaceHolder;	//Holder to obtain the canvas
import android.view.SurfaceView;
import android.util.AttributeSet;

//import android.util.Log;	//Debugging
import android.graphics.Paint;	//Debugging drawing
import android.graphics.Color;	//Debugging drawing
import android.graphics.Path;	//Plot trace
import android.graphics.Rect;
import java.util.Locale;
import android.annotation.SuppressLint;
import com.dippa2.movesenseLog.util.*;

@SuppressLint("all")
public class GraphicsSurfaceRawView extends GraphicsSurfaceView {
	private static final String TAG = "GraphicsSurfaceRawView";
	public boolean isLogging = false;

	public GraphicsSurfaceRawView(Context context) {
		super(context);
	}
	
	public GraphicsSurfaceRawView(Context context, AttributeSet attrs) {
		super(context,attrs);
	}

	public GraphicsSurfaceRawView(Context context, AttributeSet attrs,int defstyle) {
		super(context,attrs,defstyle);
	}

	/*Draw plots here*/
	@Override
	protected void onDraw(Canvas canvas) {
		double y;
		float width = (float) canvas.getWidth();
    	float height = (float) canvas.getHeight();
	    canvas.drawColor(Color.BLACK);	//Reset background color
	    
	    //Add REC
	    if (isLogging){
	    	Paint paint = new Paint(mPaint);
			paint.setTextSize(testTextSize);
			paint.setColor(Constants.colours[2]);
			Rect bounds = new Rect();
			String testText = "Accelerations [m/s²]";
			paint.getTextBounds(testText, 0, testText.length(), bounds);
			
			//float textScale = 0.7f*height/ bounds.height() < 0.7f*width/ bounds.width() ?
			//	0.7f*height/ bounds.height() : 0.7f*width/ bounds.width();


			float textScale = 0.7f*height/ bounds.height();


			// Calculate the desired size as a proportion of our testTextSize.
			paint.setTextSize(testTextSize * textScale);
			paint.setStyle(Paint.Style.FILL);
			
			//Plot REC
			paint.setTextAlign(Paint.Align.CENTER);
	    	canvas.drawText("REC",0.5f*width,0.5f*height,paint);
	    }
	    
	    if (data != null){
	    	for (int d = 0;d<data.length;++d){
	    		mPaint.setColor(Constants.colours[4+d]);
			 	path.reset();
			 	y = height/2d-(((double) (data[d][0]))/(normalise*2d)*height);
			 	path.moveTo(0f,(float) y);
			 	////////Log.d(TAG,"Data != null "+y);
			 	//Plot all datapoints?
			 	for (int i = 1; i < (int) data[d].length;++i) {
					y = height/2d-(((double) (data[d][i]))/(normalise*2d)*height);
					path.lineTo((float)(i/((float)data[d].length-1)*width),(float) y);
				}
				canvas.drawPath(path,mPaint);
			}

	    }
	    
	    //Draw text
	    if (text != null){
	    	//PLOT TIMESTAMPS
			Paint paint = new Paint(mPaint);
			paint.setTextSize(testTextSize);
			paint.setColor(Constants.colours[1]);
			Rect bounds = new Rect();
			String testText = "Accelerations [m/s²]";
			paint.getTextBounds(testText, 0, testText.length(), bounds);

			// Calculate the desired size as a proportion of our testTextSize.
			paint.setTextSize(testTextSize * (0.08f*height) / bounds.height());
			paint.setStyle(Paint.Style.FILL);
			
			//Plot y-axis values
			paint.setTextAlign(Paint.Align.LEFT);
			// Set the paint for that size.
			/*
			for (int i = 1; i<times.length-1;++i){
	    		canvas.drawText(times[i],((1f/(float)(times.length-1)))*((float)i)*width,0.99f*height,paint);
	    	}
	    	*/
	    	canvas.drawText(String.format(Locale.ROOT,"%.0f",normalise),0.01f*width,0.08f*height,paint);
	    	canvas.drawText(String.format(Locale.ROOT,"%.0f",0d),0.01f*width,(0.5f+0.04f)*height,paint);
	    	canvas.drawText(String.format(Locale.ROOT,"%.0f",-normalise),0.01f*width,(1f)*height,paint);
	    	
			paint.setTextAlign(Paint.Align.CENTER);
			canvas.drawText(text,0.5f*width,0.1f*height,paint);
	    }
	    
	    
	}
}
