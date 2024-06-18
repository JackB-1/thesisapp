/**Strings to be used to communicate/relay intent between ActivityMonitor and IMUCaptureService*/
package com.dippa2.movesenseLog.util;

import android.graphics.Color;
public final class Constants{
	//Communication through Broadcasts
	public static final String DEVICE = "com.dippa2.movesenseLog.util.Constants.DEVICE";
	public static final String DEVICEACC = "com.dippa2.movesenseLog.util.Constants.DEVICEACC";
	public static final String SCAN = "com.dippa2.movesenseLog.util.Constants.SCAN";
	public static final String STOP_SCAN = "com.dippa2.movesenseLog.util.Constants.STOP_SCAN";
	public static final String TOAST = "com.dippa2.movesenseLog.TOAST";
	public static final String BATTERY = "com.dippa2.movesenseLog.BATTERY";
	public static final String ACCVIEW = "com.dippa2.movesenseLog.ACCVIEW";
	public static final String SETTINGSUPDATED = "com.dippa2.movesenseLog.SETTINGSUPDATED";
	public static final String SENSOR_NAME = "com.dippa2.movesenseLog.SENSOR_NAME"; // added to include sensorname for broadcasting
	
	public static final int SETTINGSPAGEINTENT = 242;	//Used to launch settings
	
	public static final String ecgSR = "com.dippa2.movesenseLog.ecgSR";
	public static final String imuSR = "com.dippa2.movesenseLog.imuSR";
	public static final String hrToggle = "com.dippa2.movesenseLog.hrToggle";	
	
	
	public static final String FOUND_DEVICE = "com.dippa2.movesenseLog.util.Constants.FOUND_DEVICE";
	public static final String IMU = "com.dippa2.movesenseLog.util.Constants.IMU";
	public static final String ECG = "com.dippa2.movesenseLog.util.Constants.ECG";
	public static final String HR = "com.dippa2.movesenseLog.util.Constants.HR";
	public static final String APP_CLASS = "com.dippa2.movesenseLog.APP_CLASS";
	public static final String DO_START_SERVICE = "com.dippa2.movesenseLog.DO_START_SERVICE";
	public static final String START_SERVICE = "com.dippa2.movesenseLog.START_SERVICE";
	public static final String STOP_SERVICE = "com.dippa2.movesenseLog.STOP_SERVICE";
	public static final String START_LOGGING = "com.dippa2.movesenseLog.START_LOGGING";
	public static final String STOP_LOGGING = "com.dippa2.movesenseLog.STOP_LOGGING";
	public static final String SERVICE_LOGGING = "com.dippa2.movesenseLog.SERVICE_LOGGING";
	public static final String UPDATE_ACC_GRAPH = "com.dippa2.movesenseLog.UPDATE_ACC_GRAPH";
	public static final String UPDATE_GYRO_GRAPH = "com.dippa2.movesenseLog.UPDATE_GYRO_GRAPH";
	public static final String UPDATE_MAG_GRAPH = "com.dippa2.movesenseLog.UPDATE_MAG_GRAPH";
	public static final String UPDATE_GRAPH_X = "com.dippa2.movesenseLog.UPDATE_GRAPH_X";
	public static final String UPDATE_GRAPH_Y = "com.dippa2.movesenseLog.UPDATE_GRAPH_Y";
	public static final String UPDATE_GRAPH_Z = "com.dippa2.movesenseLog.UPDATE_GRAPH_Z";
	public static final String UPDATE_GRAPH_GYR_X = "com.dippa2.movesenseLog.UPDATE_GRAPH_GYR_X";
	public static final String UPDATE_GRAPH_GYR_Y = "com.dippa2.movesenseLog.UPDATE_GRAPH_GYR_Y";
	public static final String UPDATE_GRAPH_GYR_Z = "com.dippa2.movesenseLog.UPDATE_GRAPH_GYR_Z";
	public static final String UPDATE_GRAPH_MAG_X = "com.dippa2.movesenseLog.UPDATE_GRAPH_MAG_X";
	public static final String UPDATE_GRAPH_MAG_Y = "com.dippa2.movesenseLog.UPDATE_GRAPH_MAG_Y";
	public static final String UPDATE_GRAPH_MAG_Z = "com.dippa2.movesenseLog.UPDATE_GRAPH_MAG_Z";
	public static final String RANGE = "com.dippa2.movesenseLog.RANGE";
	public static final String startLog = "Start Logging";
	public static final String stopLog = "Stop Logging";
	public static final String logging = "Logging";
	public static final int fgServiceInt = 132;
	public static final int REQUEST_ENABLE_GPS = 156; //Used to recognise the TTS engine check activity
	public static final int REQUEST_DISABLE_BATTERY = 158;
	public static final String CAPTURE_NOTIFICATION_CHANNEL_ID = "com.dippa2.movesenseLog.utils.Constants.CAPTURE_NOTIFICATION_CHANNEL_ID";
	
	public static final float maxY = 1f;	//MAD scale
	public static final float[] rectangleYs = {0f, 0.0149f,0.091f,0.414f,1f}; //MAD visualisation rectangles
	
	//Colors
	public static final int[] colours = {0xFF000000	/*0 = black*/,
													0xFFFFFFFF	/*1 = white*/,
													0XFFFF3232	/*2 = RED*/,
													0XFFFF9632	/*3 = ORANGE*/,
													0XFFFFFF46	/*4 = YELLOW*/,
													0XFF32FF32	/*5 = GREEN*/,
													0XFF3232FF	/*6 = BLUE*/,
													Color.TRANSPARENT /*7 = Transparent*/
													};
	public static final double[] normalisation = {20d, 2048d,200d};
	public static final double updateInterval = 1d/30d;
}
