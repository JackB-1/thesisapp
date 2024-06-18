package com.dippa2.movesenseLog.service;

/**Try having a separate thread for each sensor*/


import android.util.Log;	//Debugging

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsConnectionListener;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.MdsSubscription;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

//Data visualisation
import java.util.ArrayDeque;
import java.util.ArrayList;

//Internationalisation
import java.util.Locale;


//Service calling
import android.os.Parcelable;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

//service creation etc
import android.os.Binder;
import android.os.IBinder;
import android.app.Service;
import android.os.PowerManager;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.io.File;	//For saving results to a file
import java.io.FileOutputStream;	//Output stream to write to a file
import java.io.BufferedOutputStream; //Byte output stream
import java.io.OutputStreamWriter;	//Buffered output stream
import android.os.Environment;
import java.nio.ByteBuffer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import 	android.provider.MediaStore;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import java.io.FileWriter; //Text file for frames and time stamps
import java.io.PrintWriter;
import android.os.Build;
import androidx.core.content.ContextCompat;

//Custom classes
import com.dippa2.movesenseLog.util.Constants;
import com.dippa2.movesenseLog.util.MyScanResult;
import com.dippa2.movesenseLog.data.IMUData;
import com.dippa2.movesenseLog.data.ECGData;
import com.dippa2.movesenseLog.data.HRData;
import com.dippa2.movesenseLog.data.EnergyData;
import com.dippa2.movesenseLog.MoveSenseLog;
import com.dippa2.R;

public class SensorRunnable implements Runnable{
	public static final String TAG = SensorRunnable.class.getName();
	public static final String LOG_TAG = TAG;
		
	Context context;
	String serial = "";
	String macAddress;
	String sensorName; // added to include sensorname for broadcasting
	IntentFilter intentFilter;
	String date = null;	//Used in log file name
	TimeZone tz = null;
	Locale locale;
	String ecgSR;
	String imuSR;
	String hrToggle;
	int accView;
	
	
	HashMap<String,OutputFile> outputFiles =  new HashMap<>();
	public boolean logging = false;
    protected Mds mMds; // MDS
	
	public static final String URI_CONNECTEDDEVICES = "suunto://MDS/ConnectedDevices";
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    public static final String SCHEME_PREFIX = "suunto://";
	private HashMap<String,MdsSubscription> mdsSubscriptionMap = new HashMap<>();
	private HashMap<String,String> batteryMap = new HashMap<>();
	boolean scanning = false;
		
	 // Sensor subscription https://bitbucket.org/suunto/movesense-device-lib/src/master/MovesenseCoreLib/resources/movesense-api/meas/
	private int imuRate = 52; //208;//"SampleRates": [13, 26, 52, 104, 208, 416, 833, 1666],
	private int ecgRate = 128; //512;//"AvailableSampleRates": [125, 128, 200, 250, 256, 500, 512] No config available!
	
	//static private String IMU_MEAS = String.format("/Meas/IMU9/%d",imuRate);	
	//static private String ECG_MEAS = String.format("/Meas/ECG/%d",ecgRate);	
    static private final String HR_MEAS = "/Meas/HR";	//Used to subscribe to HR
	private final String BATTERY_PATH_GET = "/System/Energy";  // /Level";
	
	
	private final String IMU_CONFIG_GET = "/Meas/IMU/Config";	//No cofig available!, set acc and gyro separately
	private final String ACC_CONFIG_GET = "/Meas/Acc/Config";
	private final String GYR_CONFIG_GET = "/Meas/Gyro/Config";
	private final String MAG_CONFIG_GET = "/Meas/Magn/Config";	//No cofig available!
	private final String TIM_CONFIG_GET = "/Time";

	private final String IMU_INFO_GET = "/Meas/IMU/Info";
	private final String ACC_INFO_GET = "/Meas/Acc/Info";
	private final String ECG_INFO_GET = "/Meas/ECG/Info";	
	
	private BroadcastReceiver runnableReceiver = null;
	ArrayList<ArrayDeque<Float>> imudata = null;
	ArrayList<ArrayDeque<Float>> ecgData = null;
	
	//Handler handler = null;
	HandlerThread handlerThread;
	Handler handler;
	
	
	public SensorRunnable(Context context, String macAddress, String sensorName, Locale locale, TimeZone tz, String date, String ecgSR,String imuSR, String hrToggle, int accView){
		this.context = context;
		this.macAddress = macAddress;
		this.sensorName = sensorName;
		this.locale = locale;
		this.tz = tz;
		this.date = date;
		// this.ecgSR = ecgSR; // jack shut down ECG function with this, if ECG should be activated this line should be uncommented
		this.ecgSR = "OFF";
		this.imuSR = imuSR;
		// this.imuSR = imuSR; jack set imuSR to always be 52
		this.imuSR = "52";
		this.hrToggle = hrToggle;
		this.accView = accView;
		Log.e(TAG,String.format("SensorRunnable construct mac %s accview %d",macAddress,accView));
		
	}
	
	@Override
	public void run(){
		
		//Log.e(TAG,String.format("SensorRunnable run %s",context.toString()));
		//handler = new Handler();
		//mMds = Mds.builder().build(context);	//Init MDS
		
		handlerThread = new HandlerThread(String.format("Handler%02d",accView));
		handlerThread.start();
		handler = new Handler(handlerThread.getLooper());
		
		mMds = Mds.builder().responseHandler(handler).build(context);	//Init MDS with a response handler
		//Log.e(TAG,String.format("SensorRunnable got MDS %s",mMds.toString()));
		//prep data holders
		if (!imuSR.equals("OFF")){
		imudata  = new ArrayList<ArrayDeque<Float>>();
			for (int i =0;i<10;++i){
				imudata.add(new ArrayDeque<Float>((int) Math.ceil(2*imuRate)));
				for (int j = 0;j<2*imuRate;++j){
					imudata.get(i).addLast(0f);
				}
			}
		}
		

		if (!ecgSR.equals("OFF")){
			ecgData = new ArrayList<ArrayDeque<Float>>();
			ecgData.add(new ArrayDeque<Float>((int) Math.ceil(2*ecgRate)));
			for (int j = 0;j<2*ecgRate;++j){
				ecgData.get(0).addLast(0f);
			}
		}
		//Log.e(TAG,String.format("create receiver "));
		//Create broadcast receiver
		runnableReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				//Stop logging once we get the broadcast from UI
				if (intent.getAction().equals(Constants.STOP_SERVICE)){
					//Stop the service here
					cleanUp();
				}
				
				//Get battery status
				if (intent.getAction().equals(Constants.BATTERY)){
					//Get keys from mdsSubscriptionMap query battery from the ones that do not contain ECG
					getBatteryStatus(serial);	//serial is always known when runnable is instantiated
				}
			}
		};
		
		//Log.e(TAG,String.format("SensorRunnable got receiver %s",runnableReceiver.toString()));
		if (runnableReceiver != null) {
			//Create an intent filter to listen to the broadcasts
			intentFilter = new IntentFilter(Constants.STOP_SERVICE);
			intentFilter.addAction(Constants.BATTERY);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				  context.registerReceiver(runnableReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
			}else {
				  context.registerReceiver(runnableReceiver, intentFilter);
			}
        }
		
		//Connect and subscribe here
		Log.e(LOG_TAG, "Connecting to BLE device: " + macAddress);
        mMds.connect(macAddress, new MdsConnectionListener() {
			SensorRunnable sensorRunnable;
			
				MdsConnectionListener init(SensorRunnable sensorRunnable){
					this.sensorRunnable = sensorRunnable;
					return this;
				}
			
            @Override
            public void onConnect(String s) {
                //Log.e(LOG_TAG, "onConnect:" + s);
            }

            @Override
            public void onConnectionComplete(String macAddress, String serial) {
				sensorRunnable.serial = serial;
				//Create files to sample to
				startLogging(serial, ecgSR,imuSR);
				getBatteryStatus(serial);
				
				//Push configurations onto the devices here
				String ACC_CONFIG = "{\"config\":{\"GRange\":16}}";	//16 g range
				String GYR_CONFIG = "{\"config\":{\"DPSRange\":2000}}";	//2000 deg range
				//String TIM_CONFIG = "{\"value\":"+Long.toString(System.currentTimeMillis()*1000l)+"}";
				//Magnetometer sensitivity cannot be modified with movesense!
				//String MAG_CONFIG = "{\"config\":{\"Scale\":800}}";	//800 uT range
				String[] toPut = new String[]{ACC_CONFIG,GYR_CONFIG};//,TIM_CONFIG};
				String[] address = new String[]{ACC_CONFIG_GET,GYR_CONFIG_GET};//,TIM_CONFIG_GET};
				
				for (int t = 0; t< toPut.length;++t){
					//Log.e(LOG_TAG,String.format("PUT call %s %s ",SCHEME_PREFIX + serial + address[t],toPut[t]));
					
					
					handler.postDelayed( new BTRunnable(serial,address[t],toPut[t],sensorRunnable.mMds), 500+500*t);  
					
				}
				
				//Get configs
				/*
				String[] toGet = new String[]{IMU_CONFIG_GET,BATTERY_PATH_GET,TIM_CONFIG_GET};//,ACC_CONFIG_GET,GYR_CONFIG_GET,MAG_CONFIG_GET};
				for (int t = 0; t< toGet.length;++t){
					//Log.e(LOG_TAG,String.format("GET call %s",SCHEME_PREFIX + serial + toGet[t]));
					mMds.get(SCHEME_PREFIX + serial + toGet[t],
					null, new MdsResponseListener() {
						String in;
						MdsResponseListener init(String in){
							this.in = in;
							return this;
						}
						@Override
						public void onSuccess(String s) {
							Log.e(LOG_TAG,String.format("GET %s %s",in, s));
						}

						@Override
						public void onError(MdsException e) {
							Log.e(LOG_TAG, "onError: ", e);
						}
					}.init(toGet[t]));
				}
				*/
				
				//Subscribe to measurements
				if (!imuSR.equals("OFF")){
					handler.postDelayed( new SubscribeRunnable(sensorRunnable,serial), 500+500*toPut.length);
				} 
				//subscribeToIMU(serial);
				//Add ECG
				if (!ecgSR.equals("OFF")){
					handler.postDelayed( new SubscribeECGRunnable(sensorRunnable,serial), 500+500*(toPut.length+1)); 
					//subscribeToECG(serial);
				}
				//Add HR
				if (!hrToggle.equals("OFF")){
					handler.postDelayed( new SubscribeHRRunnable(sensorRunnable,serial), 500+500*(toPut.length+2)); 
					//subscribeToECG(serial);
				}
				
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "onError:" + e);
            }

            @Override
            public void onDisconnect(String bleAddress) {
                //Log.e(LOG_TAG, "onDisconnect: " + bleAddress);
            }
        }.init(this));
		//try{
		//	wait();	//Wait until nofication
		//}catch (Exception e){Log.e(TAG,"Waiting failed "+e.toString());}
	}
	
	public class BTRunnable implements Runnable{
		String serial;
		String address;
		String toPut;
		Mds mMds;
		public BTRunnable(String serial, String address, String toPut, Mds mMds){
			this.serial = serial;
			this.address = address;
			this.toPut = toPut;
			this.mMds = mMds;
		}
		
		@Override
		public void run(){
			mMds.put(SCHEME_PREFIX + serial + address, toPut,
				new MdsResponseListener() {
					String command;
					String address;
					MdsResponseListener init(String address, String command){
						this.address = address;
						this.command = command;
						return this;
					}
					@SuppressWarnings( "deprecation" )
					@Override
					public void onSuccess(String s) {
						//Log.e(LOG_TAG,String.format("PUT %s %s %s",address,command, s));
					}

					@Override
					public void onError(MdsException e) {
						Log.e(LOG_TAG, "onError: ", e);
					}
				}.init(address,toPut)
			);
		}
		
	}
	
	public class SubscribeRunnable implements Runnable{
		SensorRunnable sensorRunnable;
		String serial;
		public SubscribeRunnable(SensorRunnable sensorRunnable,String serial){
			this.sensorRunnable = sensorRunnable;
			this.serial = serial;
		}
	
		@Override
		public void run(){
         Log.e(TAG,"SubscribeRunnable "+serial);
			sensorRunnable.subscribeToIMU(serial);
		}
	}
	
	public class SubscribeECGRunnable implements Runnable{
		SensorRunnable sensorRunnable;
		String serial;
		public SubscribeECGRunnable(SensorRunnable sensorRunnable,String serial){
			this.sensorRunnable = sensorRunnable;
			this.serial = serial;
		}
	
		@Override
		public void run(){
			sensorRunnable.subscribeToECG(serial);
		}
	}
	
	public class SubscribeHRRunnable implements Runnable{
		SensorRunnable sensorRunnable;
		String serial;
		public SubscribeHRRunnable(SensorRunnable sensorRunnable,String serial){
			this.sensorRunnable = sensorRunnable;
			this.serial = serial;
		}
	
		@Override
		public void run(){
			sensorRunnable.subscribeToHR(serial);
		}
	}
	
	//Subscribe to the HR
	private void subscribeToHR(String connectedSerial) {
        // Clean up existing subscription (if there is one)
        if (mdsSubscriptionMap.get(connectedSerial+"HR") != null){ //mdsSubscription != null) {
            unsubscribe(connectedSerial+"HR");
		}
		
		//Log.e(TAG,String.format("SubscibeToECG %s ",connectedSerial));

        // Build JSON doc that describes what resource and device to subscribe
        StringBuilder sb = new StringBuilder();
        String strContract = sb.append("{\"Uri\": \"").append(connectedSerial).append(HR_MEAS).append("\"}").toString();
		Log.d(LOG_TAG,"Subscribe HR "+ strContract);
        
		mdsSubscriptionMap.put(connectedSerial+"HR",mMds.builder().responseHandler(handler).build(context).subscribe(URI_EVENTLISTENER,
                strContract, new MdsNotificationListener() {
                    @Override
                    public void onNotification(String data) {
						//Log.e(LOG_TAG,String.format("HR notification "+data));
						long astamp = System.currentTimeMillis();
						//Decode the incoming data
						if (true){	//Enable debugging
							HRData tempData = new Gson().fromJson(data, HRData.class);
							//Update HR in the notification?
							context.sendBroadcast(
								new Intent()
								.setAction(Constants.HR)
								.putExtra(Constants.HR,tempData.body.hr)							
							);
								
							//Write data to file (Fire up a new thread for the write?
							new Thread(
								new Runnable(){
									OutputFile of;
									OutputFile rof;	//For RR values
									Locale locale;
									String dataString;
									HRData hrData;
									Runnable init(OutputFile of,OutputFile rof,Locale locale,String dataString,HRData hrData){
										this.of = of;
										this.rof = rof;
										this.locale = locale;
										this.dataString = dataString;
										this.hrData = hrData;
										return this;
									}
									
									@Override
									public void run(){
										//Write HR
										writeData(of,dataString,new float[]{(float) hrData.body.hr}, locale);
										//Log.e(LOG_TAG,String.format("Write HR %f rr array length %d",hrData.body.hr,hrData.body.rrData.length));
										//Write RR values
										String tempDS;
										for (int i =0;i<hrData.body.rrData.length;++i){
											tempDS = dataString+String.format(locale,"\t%d",i);
											//Log.e(LOG_TAG,String.format("Write RR %d %d",i,hrData.body.rrData[i]));
											writeData(rof,tempDS,
												new int[]{ hrData.body.rrData[i]}
												, locale);
										}
										/*
										try{
											rof.myOutWriter.flush();
										}catch(Exception e){Log.e(LOG_TAG,"IntArray write failed "+e.toString());}
										*/
									}
								
								}.init(outputFiles.get(connectedSerial+"HR"),outputFiles.get(connectedSerial+"RR"),locale,String.format(locale,"%d",astamp),tempData)
							).start();
						}
                    }

                    @Override
                    public void onError(MdsException error) {
                        Log.e(LOG_TAG, "HR subscription onError(): ", error);
                        //unsubscribe(connectedSerial+"ECG");
                    }
                })
			);

    }
	
	
	//Subscribe to the IMU
	private void subscribeToIMU(String connectedSerial) {
        // Clean up existing subscription (if there is one)
        if (mdsSubscriptionMap.get(connectedSerial) != null){ //mdsSubscription != null) {
            unsubscribe(connectedSerial);
        }

        // Build JSON doc that describes what resource and device to subscribe
        
 
        StringBuilder sb = new StringBuilder();
        String strContract = sb.append("{\"Uri\": \"").append(connectedSerial).append(String.format("/Meas/IMU9/%s",imuSR)).append("\"}").toString();
		
        Log.e(LOG_TAG, strContract);

		mdsSubscriptionMap.put(connectedSerial,mMds.builder().responseHandler(handler).build(context).subscribe(URI_EVENTLISTENER,
                strContract, new MdsNotificationListener() {
                    @Override
                    public void onNotification(String data) {
						long astamp = System.currentTimeMillis();
						//Decode the incoming data
						IMUData imuData = new Gson().fromJson(data, IMUData.class);
						
						
						//Visualise data on screen, add all new data into imudata
						for (int j = 0; j<imuData.body.accArray.length; ++j){
							//Accelerations
							imudata.get(0).removeFirst();
							imudata.get(0).addLast((float ) imuData.body.accArray[j].x);
							imudata.get(1).removeFirst();
							imudata.get(1).addLast((float ) imuData.body.accArray[j].y);
							imudata.get(2).removeFirst();
							imudata.get(2).addLast((float ) imuData.body.accArray[j].z);
							//Gyro
							imudata.get(0+3).removeFirst();
							imudata.get(0+3).addLast((float ) imuData.body.gyrArray[j].x);
							imudata.get(1+3).removeFirst();
							imudata.get(1+3).addLast((float ) imuData.body.gyrArray[j].y);
							imudata.get(2+3).removeFirst();
							imudata.get(2+3).addLast((float ) imuData.body.gyrArray[j].z);
							//magArray
							imudata.get(0+6).removeFirst();
							imudata.get(0+6).addLast((float ) imuData.body.magArray[j].x);
							imudata.get(1+6).removeFirst();
							imudata.get(1+6).addLast((float ) imuData.body.magArray[j].y);
							imudata.get(2+6).removeFirst();
							imudata.get(2+6).addLast((float ) imuData.body.magArray[j].z);
						
						}
						//Visualisation
						float[][] rawData = new float[9][];
						for (int i = 0;i<9;++i){
							Float[] tt = imudata.get(i).toArray(new Float[imudata.get(i).size()]);
							rawData[i] = new float[tt.length];
							for (int j = 0;j<tt.length;++j){
								rawData[i][j] = tt[j];
							}
						}

						context.sendBroadcast(
							/*
									public static final String UPDATE_GRAPH_GYR_X = "com.dippa2.movesenseLog.UPDATE_GRAPH_GYR_X";
									public static final String UPDATE_GRAPH_GYR_Y = "com.dippa2.movesenseLog.UPDATE_GRAPH_GYR_Y";
									public static final String UPDATE_GRAPH_GYR_Z = "com.dippa2.movesenseLog.UPDATE_GRAPH_GYR_Z";
									public static final String UPDATE_GRAPH_MAG_X = "com.dippa2.movesenseLog.UPDATE_GRAPH_MAG_X";
									public static final String UPDATE_GRAPH_MAG_Y = "com.dippa2.movesenseLog.UPDATE_GRAPH_MAG_Y";
									public static final String UPDATE_GRAPH_MAG_Z = "com.dippa2.movesenseLog.UPDATE_GRAPH_MAG_Z";
							*/
							
							new Intent()
							.setAction(Constants.IMU)
							.putExtra(Constants.UPDATE_GRAPH_X,Arrays.copyOf(rawData[0],rawData[0].length))
							.putExtra(Constants.UPDATE_GRAPH_Y,Arrays.copyOf(rawData[1],rawData[1].length))
							.putExtra(Constants.UPDATE_GRAPH_Z,Arrays.copyOf(rawData[2],rawData[2].length))
							.putExtra(Constants.UPDATE_GRAPH_GYR_X,Arrays.copyOf(rawData[3],rawData[3].length))
							.putExtra(Constants.UPDATE_GRAPH_GYR_Y,Arrays.copyOf(rawData[4],rawData[4].length))
							.putExtra(Constants.UPDATE_GRAPH_GYR_Z,Arrays.copyOf(rawData[5],rawData[5].length))
							.putExtra(Constants.UPDATE_GRAPH_MAG_X,Arrays.copyOf(rawData[6],rawData[6].length))
							.putExtra(Constants.UPDATE_GRAPH_MAG_Y,Arrays.copyOf(rawData[7],rawData[7].length))
							.putExtra(Constants.UPDATE_GRAPH_MAG_Z,Arrays.copyOf(rawData[8],rawData[8].length))
							.putExtra(Constants.ACCVIEW,accView)
							.putExtra(Constants.BATTERY,batteryMap.get(connectedSerial))
							.putExtra(Constants.SENSOR_NAME, sensorName)
						);
						
						//Write data to file (Fire up a new thread for the write?
						new Thread(
							new Runnable(){
								OutputFile of;
								Locale locale;
								String dataString;
								IMUData imuData;
								Runnable init(OutputFile of,Locale locale,String dataString,IMUData imuData){
									this.of = of;
									this.locale = locale;
									this.dataString = dataString;
									this.imuData = imuData;
									return this;
								}
								
								@Override
								public void run(){
									String tempDS;
									for (int i =0;i<imuData.body.accArray.length;++i){
										tempDS = dataString+String.format(locale,"\t%d",i);
										writeData(of,tempDS,
											new float[]{
											(float ) imuData.body.accArray[i].x,(float ) imuData.body.accArray[i].y,(float ) imuData.body.accArray[i].z,
											(float ) imuData.body.gyrArray[i].x,(float ) imuData.body.gyrArray[i].y,(float ) imuData.body.gyrArray[i].z,
											(float ) imuData.body.magArray[i].x,(float ) imuData.body.magArray[i].y,(float ) imuData.body.magArray[i].z
																}
											, locale);
									}
								}
							
							}.init(outputFiles.get(connectedSerial+"IMU"),locale,String.format(locale,"%d\t%d",astamp,imuData.body.timestamp),imuData)
						).start();
                    }

                    @Override
                    public void onError(MdsException error) {
                        Log.e(LOG_TAG, "subscription onError(): ", error);
                        //unsubscribe(connectedSerial);
                    }
                })
			);

    }
	
	//Subscribe to the ECG
	private void subscribeToECG(String connectedSerial) {
        // Clean up existing subscription (if there is one)
        if (mdsSubscriptionMap.get(connectedSerial+"ECG") != null){ //mdsSubscription != null) {
            unsubscribe(connectedSerial+"ECG");
		}
		
		//Log.e(TAG,String.format("SubscibeToECG %s ",connectedSerial));

        // Build JSON doc that describes what resource and device to subscribe
        StringBuilder sb = new StringBuilder();
        String strContract = sb.append("{\"Uri\": \"").append(connectedSerial).append(String.format("/Meas/ECG/%s",ecgSR)).append("\"}").toString();
		//Log.d(LOG_TAG, strContract);
        
		mdsSubscriptionMap.put(connectedSerial+"ECG",mMds.builder().responseHandler(handler).build(context).subscribe(URI_EVENTLISTENER,
                strContract, new MdsNotificationListener() {
                    @Override
                    public void onNotification(String data) {
						long astamp = System.currentTimeMillis();
						//Decode the incoming data
						ECGData tempData = new Gson().fromJson(data, ECGData.class);
						
						//Visualise ECG
						for (int j = 0; j<tempData.body.ecg.length; ++j){
							//Accelerations
							ecgData.get(0).removeFirst();
							ecgData.get(0).addLast((float ) tempData.body.ecg[j]);
						}
						
						//Visualisation
						float[][] rawData = new float[1][];
						
						Float[] tt = ecgData.get(0).toArray(new Float[ecgData.get(0).size()]);
						rawData[0] = new float[tt.length];
						for (int j = 0;j<tt.length;++j){
							rawData[0][j] = tt[j];
						}
					

						context.sendBroadcast(
							new Intent()
							.setAction(Constants.ECG)
							.putExtra(Constants.ECG,Arrays.copyOf(rawData[0],rawData[0].length))
							.putExtra(Constants.ACCVIEW,accView)							
						);
							
						//Write data to file (Fire up a new thread for the write?
						new Thread(
							new Runnable(){
								OutputFile of;
								Locale locale;
								String dataString;
								ECGData ecgData;
								Runnable init(OutputFile of,Locale locale,String dataString,ECGData ecgData){
									this.of = of;
									this.locale = locale;
									this.dataString = dataString;
									this.ecgData = ecgData;
									return this;
								}
								
								@Override
								public void run(){
									String tempDS;
									for (int i =0;i<ecgData.body.ecg.length;++i){
										tempDS = dataString+String.format(locale,"\t%d",i);
										writeData(of,tempDS,
											new float[]{
											(float ) ecgData.body.ecg[i]
																}
											, locale);
									}
								}
							
							}.init(outputFiles.get(connectedSerial+"ECG"),locale,String.format(locale,"%d\t%d",astamp,tempData.body.timestamp),tempData)
						).start();
                    }

                    @Override
                    public void onError(MdsException error) {
                        Log.e(LOG_TAG, "subscription onError(): ", error);
                        //unsubscribe(connectedSerial+"ECG");
                    }
                })
			);

    }
	
	
	private void unsubscribe(String connectedSerial) {
		
		if (mdsSubscriptionMap.get(connectedSerial) != null){ //mdsSubscription != null) {
			//Log.e(LOG_TAG,"Unsubscribe "+connectedSerial);
            mdsSubscriptionMap.get(connectedSerial).unsubscribe();
			mdsSubscriptionMap.remove(connectedSerial);
        }

    }
	
	public void startLogging(String serial, String ecgSR, String imuSR){
		
		//Open log files
		if (!imuSR.equals("OFF")){
			outputFiles.put(serial+"IMU",createOutput("imuLog", "IMU_"+imuSR+"_"+serial, date,"TStamp "+tz.getID()+"\tSensorStamp\tpackageIndex\taX [m/s2]\taY [m/s2]\taZ [m/s2]\tgX [m/s2]\tgY [m/s2]\tgZ [m/s2]\tmX [m/s2]\tmY [m/s2]\tmZ [m/s2]"));
		}
		if (!ecgSR.equals("OFF")){
			outputFiles.put(serial+"ECG",createOutput("ecgLog", "ECG_"+ecgSR+"_"+serial, date,"TStamp "+tz.getID()+"\tSensorStamp\tpackageIndex\tValue [au]"));
		}
		if (!hrToggle.equals("OFF")){
			outputFiles.put(serial+"HR",createOutput("hrLog", "HR"+"_"+serial, date,"TStamp "+tz.getID()+"\tHR [bpm]"));
			outputFiles.put(serial+"RR",createOutput("rrLog", "RR"+"_"+serial, date,"TStamp "+tz.getID()+"\tpackageIndex\tRR interval [ms]"));

		}
		
    }
	
	public class OutputFile{
		public PrintWriter myOutWriter;
		public OutputFile(PrintWriter myOutWriter){
			this.myOutWriter = myOutWriter;
		}
	}
	
	protected OutputFile createOutput(String logFolder, String dataType, String date, String headerLine){
		ContentResolver cr = context.getContentResolver(); //Get content resolver
    	ContentValues cv = new ContentValues();
		cv.put(MediaStore.MediaColumns.DISPLAY_NAME, dataType+"_"+date+".txt");
		cv.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain"); //file extension, will automatically add to file
		cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
		Uri textUri = cr.insert(MediaStore.Files.getContentUri("external"), cv);
    	PrintWriter writer = null;
    	Log.e(TAG,"FilePath "+textUri.getPath());
    	boolean fileExists = (new File(textUri.getPath())).exists();
		try{
			writer = new PrintWriter(cr.openOutputStream(textUri,"wa"));//, false, Charset.forName("UTF-8"));	
        	Log.e(TAG,String.format("Create output stream %s",textUri.getPath()));
		}catch(Exception e){
			Log.e(TAG,"Could not PrintWriter "+e.toString());
		}
		
		
		if(!fileExists){
			try{
				//Log.d(TAG,"Write header "+headerLine);
				writer.print(headerLine+"\n");
			}catch(Exception e){
				Log.e(TAG,"Could not write header "+e.toString());
			}
		}
		return new OutputFile(writer);	
	}
		
	//Write data to output file
	public static void writeData(OutputFile outputFile,String dataString,float[] values, Locale locale){
		for (int f = 0; f<values.length;++f){
			dataString+=String.format(locale,"\t%f",values[f]);
		}
		try{
			outputFile.myOutWriter.print(dataString+"\n");
		}catch(Exception e){}
	}
	
	//Write integer data to output file
	public static void writeData(OutputFile outputFile,String dataString,int[] values, Locale locale){
		for (int f = 0; f<values.length;++f){
			dataString+=String.format(locale,"\t%d",values[f]);
		}
		try{
			outputFile.myOutWriter.print(dataString+"\n");
		}catch(Exception e){Log.e(LOG_TAG,"IntArray write failed "+e.toString());}
	}
  
	public void closeOutput(OutputFile of){
		if (of != null){
			try{
				of.myOutWriter.flush();
				of.myOutWriter.close();
			}catch(Exception e){}
		}
	}	
	
	public void getBatteryStatus(String serial){
		//Log.e(TAG,"Requesting battery status "+serial);
		String[] toGet = new String[]{BATTERY_PATH_GET};//,ACC_CONFIG_GET,GYR_CONFIG_GET,MAG_CONFIG_GET};
		for (int t = 0; t< toGet.length;++t){
			//Log.e(LOG_TAG,String.format("GET call %s",SCHEME_PREFIX + serial + toGet[t]));
			mMds.get(SCHEME_PREFIX + serial + toGet[t],
			null, new MdsResponseListener() {
				String serial;
				Context context;
				MdsResponseListener init(String serial,Context context){
					this.serial = serial;
					this.context = context;
					return this;
				}
				@SuppressWarnings( "deprecation" )
				@Override
				public void onSuccess(String s) {
					//Log.e(TAG,"Requesting battery status onSuccess "+serial+ " "+s);
					EnergyData energyData = new Gson().fromJson(s, EnergyData.class);
					String toastText = String.format("bat %d mV",energyData.content.mV);
					batteryMap.put(serial,toastText);
				}

				@Override
				public void onError(MdsException e) {
					Log.e(LOG_TAG, "onError: ", e);
				}
			}.init(serial,context));
		}
		
	}
	
	private void cleanUp(){
		//Log.e(LOG_TAG,"Shutdownd called");
		Set<String> keys = mdsSubscriptionMap.keySet();
		Iterator<String> it = keys.iterator();
		ArrayList<String> keyList = new ArrayList<String>();
		while (it.hasNext()) {
			keyList.add(it.next());
		}
		
		for (int i = 0; i<keyList.size();++i){
			unsubscribe(keyList.get(i));
		}
		
		//Close output files as well
		Set<String> keyso = outputFiles.keySet();
		Iterator<String> ito = keyso.iterator();
		ArrayList<String> keyListo = new ArrayList<String>();
		while (ito.hasNext()) {
			keyListo.add(ito.next());
		}
		
		for (int i = 0; i<keyListo.size();++i){
			Log.e(TAG,String.format("SensorRunnable cleanUp outputFile %d %s",i,keyListo.get(i)));
			
			closeOutput(outputFiles.get(keyListo.get(i)));
			outputFiles.remove(keyListo.get(i));
		}
		
		if (runnableReceiver != null){
			context.unregisterReceiver(runnableReceiver);
		}
	}
	
}
