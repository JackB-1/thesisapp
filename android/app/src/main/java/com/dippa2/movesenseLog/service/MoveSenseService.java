package com.dippa2.movesenseLog.service;

/**Handle bluetooth, movesense sampling and data storage in the service, i.e. all of it...*/

import android.util.Log;	//Debugging

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsConnectionListener;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.MdsSubscription;
//import com.polidea.rxandroidble.RxBleClient;
//import com.polidea.rxandroidble.RxBleDevice;
//import com.polidea.rxandroidble.scan.ScanSettings;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

//import rx.Subscription;
import io.reactivex.disposables.Disposable;

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
import android.os.Looper;

//Foreground notification
import androidx.core.app.NotificationCompat;
import android.app.NotificationChannel;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.os.Build;

//Custom classes
import com.dippa2.movesenseLog.util.Constants;
import com.dippa2.movesenseLog.util.MyScanResult;
import com.dippa2.movesenseLog.data.IMUData;
import com.dippa2.movesenseLog.data.ECGData;
import com.dippa2.movesenseLog.data.EnergyData;
import com.dippa2.movesenseLog.MoveSenseLog;
import com.dippa2.R;

public class MoveSenseService extends Service{
	
	public static final String TAG = MoveSenseService.class.getName();
	public static final String LOG_TAG = TAG;
	private PowerManager.WakeLock wl;
    private NotificationCompat.Builder initBuilder = null;
    IntentFilter intentFilter;

	String date = null;	//Used in log file name
	long tStampOffset;	//Used in log file
	TimeZone tz = null;
	Locale defaultLocale;
	boolean logging = false;
	
	String ecgSR;
	String imuSR;
	String hrToggle;	
	
	// MDS
    //private Mds mMds;
    public static final String URI_CONNECTEDDEVICES = "suunto://MDS/ConnectedDevices";
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    public static final String SCHEME_PREFIX = "suunto://";
	private HashMap<String,Thread> threadMap = new HashMap<>();
	private HashMap<String,MdsSubscription> mdsSubscriptionMap = new HashMap<>();
	private HashMap<String,String> batteryMap = new HashMap<>();
	boolean scanning = false;
	
    // BleClient singleton
    static private RxBleClient mBleClient = null;
	//Subscription mScanSubscription = null;
	Disposable mScanSubscription = null;
	
	
	
	/*Service to work with MoveSense sensors and Bluetooth*/
	public void onCreate(){
		super.onCreate();
		getWakeLock();
		if (serviceReceiver != null) {
			//Create an intent filter to listen to the broadcasts
            intentFilter = new IntentFilter(Constants.DEVICE);
            intentFilter.addAction(Constants.STOP_SERVICE);
				intentFilter.addAction(Constants.SCAN);
				intentFilter.addAction(Constants.STOP_SCAN);
				intentFilter.addAction(Constants.HR);	//HR updates -> update notification
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					  registerReceiver(serviceReceiver, intentFilter, RECEIVER_EXPORTED);
				}else {
					  registerReceiver(serviceReceiver, intentFilter);
				}
				
				
        }
        defaultLocale = Locale.getDefault();
		//Log.e(LOG_TAG,"Service onCreate");
    }
	 
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags , startId);
        if (intent.getAction().equals(Constants.STOP_SERVICE)){
        		
        		//Send STOP_SERVICE broadcast
        		Intent new_intent = new Intent();
				new_intent.setAction(Constants.STOP_SERVICE);
				sendBroadcast(new_intent);
        		
	        	stopForeground(true);
		     	stopSelf();	//Call this to stop the service
        }
        
        if(intent.getAction().equals(Constants.START_SERVICE)){
			//Log.e(LOG_TAG,"Service START_SERVICE");
        	if (!logging){
        		//Log.d(TAG,"createInit "+intent.getAction());
        		createInitNotification();
        	}
        	 //Log.d(TAG,"Start service");
	     	//Start the service in foreground
	     	startForeground(Constants.fgServiceInt, initBuilder.build());		//Does nothing if already running
        }
        return START_NOT_STICKY; //Do not restart after crash...//START_STICKY; // If we get killed, after returning from here, restart
    }
	
	//For notification
     private void createInitNotification() {
	     createNotificationChannel();	//Required for Android 9 and above
        Intent notificationIntent = new Intent(this, MoveSenseLog.class);
        notificationIntent.setAction("MovesenseNotification");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
         //Launch HRLog as the default action
          Intent arIntent = new Intent(this, MoveSenseLog.class);
        PendingIntent parIntent = PendingIntent.getActivity(this, 0,
                arIntent, PendingIntent.FLAG_IMMUTABLE);      
 			//Enable shutting down the monitor from taskbar
 			Intent closeIntent = new Intent(this, MoveSenseService.class);
 			closeIntent.setAction(Constants.STOP_SERVICE);
        PendingIntent pcloseIntent = PendingIntent.getService(this, 0,
                closeIntent, PendingIntent.FLAG_IMMUTABLE);
 	        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_launcher);
                
         initBuilder = new NotificationCompat.Builder(this,Constants.CAPTURE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Movesense Logger")
                .setContentText("Ready")
                .setContentIntent(parIntent)
                .setTicker("Movesense Logger")
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setColor(Constants.colours[5])
                .setOngoing(true)
                .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_close, "Exit",pcloseIntent).build());
                //Log.d(TAG,"Notification built");       
    }    
    
	
	//Create a nofitication channel. Required on SDK 26  (O) or higher
	private void createNotificationChannel(){
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         NotificationChannel serviceChannel = new NotificationChannel(
                 Constants.CAPTURE_NOTIFICATION_CHANNEL_ID,
                 getResources().getString(R.string.app_name),
                 NotificationManager.IMPORTANCE_LOW
         );
		serviceChannel.setSound( null, null );	//Turn of notification sound
         NotificationManager manager = getSystemService(NotificationManager.class);
         manager.createNotificationChannel(serviceChannel);
     }
  }

  //Broadcast receiver
    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
			//Log.e(LOG_TAG,"SERVICE onReceive "+intent.getAction());
			
				if (intent.getAction().equals(Constants.HR)){
					double hr = intent.getDoubleExtra(Constants.HR,-1d);
					//Update the notification with HR
					NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					initBuilder.setContentText(String.format("HR %03d",(int) Math.round(hr)));
					nm.notify(Constants.fgServiceInt, initBuilder.build());
			 	}
			
				//Connect BT to DEVICE once we get the broadcast from UI
				if (intent.getAction().equals(Constants.DEVICE)){
					//Connect to sensor here
					String json = intent.getStringExtra(Constants.DEVICE);
					MyScanResult device = (new Gson()).fromJson(json, MyScanResult.class);
					ecgSR = intent.getStringExtra(Constants.ecgSR);
					imuSR = intent.getStringExtra(Constants.imuSR);
					hrToggle = intent.getStringExtra(Constants.hrToggle);
					connectBLEDevice(device);
			 	}

				if (intent.getAction().equals(Constants.SCAN)){
					//Scan for sensors here
					//Log.e(LOG_TAG,"SERVICE SCAN");
					
					mScanSubscription = getBleClient().scanBleDevices(new ScanSettings.Builder().build()).subscribe(
                        scanResult -> {
                            // Process scan result here. filter movesense devices.
							//Log.e(LOG_TAG,"SERVICE SCANNING BLE "+scanResult.getBleDevice().getName());
                            if (scanResult.getBleDevice()!=null &&
                                    scanResult.getBleDevice().getName() != null &&
                                    scanResult.getBleDevice().getName().startsWith("Movesense")) {
								
								//Broadcast the Device to UI
								//Log.e(LOG_TAG,"SERVICE FOUND_DEVICE "+scanResult.getBleDevice().getName());
								sendBroadcast(new Intent()
												.setAction(Constants.FOUND_DEVICE)
												.putExtra(Constants.FOUND_DEVICE,(new Gson()).toJson(new MyScanResult(scanResult))));	//Send the intent to HRLog 
                            }
                        },
                        throwable -> {
                            //Log.e(LOG_TAG,"scan error: " + throwable);
                            // Handle an error here.

                            // Re-enable scan buttons, just like with ScanStop
                            stopScanning();
                        }
					);
					

			 	}
				if (intent.getAction().equals(Constants.STOP_SCAN)){
					//Stop scanning here
					stopScanning();
			 	}
			 	//Stop logging once we get the broadcast from UI
				if (intent.getAction().equals(Constants.STOP_SERVICE)){
					//Stop the service here
			 	}
			}
    };
    
	
	private void stopScanning() {
		//Log.e(LOG_TAG,"Service stopScanning");
		if (mScanSubscription != null){
			//mScanSubscription.unsubscribe();
			mScanSubscription.dispose();
			mScanSubscription = null;
		}
	}
	
	private RxBleClient getBleClient() {
        // Init RxAndroidBle (Ble helper library) if not yet initialized
        if (mBleClient == null)
        {
            mBleClient = RxBleClient.create(this);
        }

        return mBleClient;
    }

	private void connectBLEDevice(MyScanResult device) {
		startLogging();
		//Log.e(TAG,String.format("connectBLEDevice name %s",device.name));
        RxBleDevice bleDevice = getBleClient().getBleDevice(device.macAddress);
		
		//Start SensorRunnable here, feed in mac address, serial etc
		threadMap.put(device.macAddress,
					new Thread(new SensorRunnable(this, bleDevice.getMacAddress(), bleDevice.getName(), defaultLocale, tz, date, ecgSR,imuSR,hrToggle, threadMap.size()))
				);
		threadMap.get(device.macAddress).start();
    }

	
  
	public void startLogging(){
    	//Just set the file name
		if (tz == null){
			date = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
			tz = TimeZone.getDefault();
			tStampOffset = (long) tz.getOffset(System.currentTimeMillis());
		}
		logging = true;
    }
  
	private void cleanUp(){
		//Log.e(LOG_TAG,"Shutdownd called");
		
		Set<String> keys = threadMap.keySet();
		Iterator<String> it = keys.iterator();
		ArrayList<String> keyList = new ArrayList<String>();
		while (it.hasNext()) {
			keyList.add(it.next());
		}
		
		for (int i = 0; i<keyList.size();++i){
			//Log.e(TAG,String.format("cleanUp notifying thread %d",i));
			try{
				//threadMap.get(keyList.get(i)).notify();
				threadMap.get(keyList.get(i)).join();	//SensorRunnables will get broadcast and call cleanUp on themselves -> should finish and join
			}catch (Exception e){Log.e(TAG,"Could not  notify "+e.toString());}
			threadMap.remove(keyList.get(i));
		}
		//Log.e(TAG,String.format("cleanUp unregisterReceiver"));
		unregisterReceiver(serviceReceiver);
	}
  
  
  
	@Override
    public void onDestroy() {
    
		//Clean up here
		stopScanning();
		cleanUp();
		wl.release();									//Release wakelock
		unregisterReceiver(serviceReceiver);	//BROADCAST Unregister the receiver
		super.onDestroy();
    }
	
	//Wakelock etc
	private void getWakeLock() {
   		//Log.d(TAG,"WLock");
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();	//Start wake lock, wl.release(); needs to be called to shut this down...
    }
    
    //Implement abstract onBind (requires the IBinder stuff...)
    public class LocalBinder extends Binder {
        MoveSenseService getService() {
            return MoveSenseService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
}
