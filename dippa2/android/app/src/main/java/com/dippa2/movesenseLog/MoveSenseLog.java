package com.dippa2.movesenseLog;


import com.dippa2.R;
//DEBUGGING
import android.util.Log;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


//ActionBar
import android.content.SharedPreferences;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;


//Visualisation
import android.widget.LinearLayout;
import 	android.widget.Button;

//Requesting file write permission
import android.os.Build;
import android.content.pm.PackageManager;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.os.Handler;

//Power saving
import android.provider.Settings;
import android.os.PowerManager;
import android.net.Uri;

//Service calling
import android.os.Parcelable;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import java.util.ArrayList;
import android.location.LocationManager;
import com.google.gson.Gson;

//Custom classes
import com.dippa2.movesenseLog.service.MoveSenseService;	//acc service
import com.dippa2.movesenseLog.util.Constants;
import com.dippa2.movesenseLog.graphicsView.GraphicsSurfaceRawView;
import com.dippa2.movesenseLog.util.MyScanResult;



public class MoveSenseLog extends AppCompatActivity implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener  {
    public static final String LOG_TAG = MoveSenseLog.class.getName();
    public static final String TAG =LOG_TAG;
	// Requesting permissions
	private static final int REQUEST_PERMISSIONS = 201;	 
    private boolean permissionToWriteAccepted = false;
    
		//Settings
		String ecgSR;
      String imuSR;
	  String hrToggle;
      TextView ecgSRText;
      TextView imuSRText;
	  TextView hrText;
	  TextView hrTextView;

    

    // UI
    private ListView mScanResultListView;
    private ArrayList<MyScanResult> mScanResArrayList = new ArrayList<>();
    ArrayAdapter<MyScanResult> mScanResArrayAdapter;
	boolean scanning = false;
   
	
	
	//Available configs?
	/*
		MoveSenseLog: IMU_INFO {"Content": {"SampleRates": [13, 26, 52, 104, 208, 416, 833, 1666], 
			"AccRanges": [2, 4, 8, 16]
			"GyroRanges": [245, 500, 1000, 2000]
			"MagnRanges": [400, 800, 1200, 1600]}}
		
		MoveSenseLog: ECG_INFO {"Content": {"CurrentSampleRate": 128, 
			"AvailableSampleRates": [125, 128, 200, 250, 256, 500, 512]
			"ArraySize": 16}}
	*/
	
    
	//Could make these into Arrays or hashmap -> add when needed
	// Deactivated graphicsView to improve performance of activity app
	/* private GraphicsSurfaceRawView[] graphicsViewAcc = null; */
	// Deactivated graphicsView to improve performance of activity app
	/* private GraphicsSurfaceRawView graphicsViewECG; */

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Get actionbar
		  Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		  setSupportActionBar(myToolbar);
        
        
		((Button) findViewById(R.id.buttonScan)).setEnabled(false);	//Disable scan button until permissions have been granted
		
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}
			
		//Have to have GPS on to use the app, request enable here
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),Constants.REQUEST_ENABLE_GPS);
			Toast.makeText(this, R.string.try_again, Toast.LENGTH_SHORT).show();
			finish();	//The user has to re-start the app with GPS enabled
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !((PowerManager) getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName())){
			//Request ignoring battery optimizations
			 startActivityForResult(
				 new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:" + getPackageName()))
				 ,Constants.REQUEST_DISABLE_BATTERY);
		 }
		
		// Deactivated graphicsView to improve performance of activity app
		/* graphicsViewAcc = new GraphicsSurfaceRawView[3];
		
		graphicsViewAcc[0] =  new GraphicsSurfaceRawView(this);
		((LinearLayout) findViewById(R.id.accLayout1)).addView(graphicsViewAcc[0]);
		graphicsViewAcc[1] =  new GraphicsSurfaceRawView(this);
		((LinearLayout) findViewById(R.id.accLayout2)).addView(graphicsViewAcc[1]);
		graphicsViewAcc[2] =  new GraphicsSurfaceRawView(this);
		((LinearLayout) findViewById(R.id.accLayout3)).addView(graphicsViewAcc[2]);

		graphicsViewECG =  new GraphicsSurfaceRawView(this);
		((LinearLayout) findViewById(R.id.ecgLayout)).addView(graphicsViewECG); */

        // Init Scan UI
        mScanResultListView = (ListView)findViewById(R.id.listScanResult);
        mScanResArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mScanResArrayList);
        mScanResultListView.setAdapter(mScanResArrayAdapter);
        mScanResultListView.setOnItemLongClickListener(this);
        mScanResultListView.setOnItemClickListener(this);

	 		ecgSRText =(TextView)findViewById(R.id.ecgSRTextView);
	 		imuSRText =(TextView)findViewById(R.id.imuSRTextView);
			hrText=(TextView)findViewById(R.id.hrTextView);
			hrTextView = (TextView)findViewById(R.id.hrBMPTextView);

        //Get settings
      getSettings();
		
      
      
		//Request permissions
		ArrayList<String> permsReq = new ArrayList<String>();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			permsReq.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
		}
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			 permsReq.add(Manifest.permission.ACCESS_FINE_LOCATION);
			 permsReq.add(Manifest.permission.BLUETOOTH_SCAN);
			 permsReq.add(Manifest.permission.BLUETOOTH_CONNECT);
         
      }else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
			permsReq.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			permsReq.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			permsReq.add(Manifest.permission.ACCESS_COARSE_LOCATION);
			permsReq.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
			permsReq.add(Manifest.permission.ACCESS_FINE_LOCATION);
      }else{
			permsReq.add(Manifest.permission.ACCESS_COARSE_LOCATION);
			permsReq.add(Manifest.permission.ACCESS_FINE_LOCATION);
         
      }
      
      ArrayList<String> permsReq2 = new ArrayList<String>();
      for (int i = 0;i<permsReq.size();++i){
         if (ContextCompat.checkSelfPermission(MoveSenseLog.this, permsReq.get(i)) != PackageManager.PERMISSION_GRANTED){
            permsReq2.add(permsReq.get(i));
         }
      }
      
      if (permsReq2.size() < 1){
         ((Button) findViewById(R.id.buttonScan)).setEnabled(true);
         prepServices();
      }else{
         ActivityCompat.requestPermissions(MoveSenseLog.this, permsReq2.toArray(new String[1]), REQUEST_PERMISSIONS);
      }

     
    }

	
	//Request file writing permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Log.e(TAG,"oneRequestPermission result");
        
        switch (requestCode){
            case REQUEST_PERMISSIONS:
				boolean permissionAccepted = true;
				for (int gr : grantResults){
					if (gr != PackageManager.PERMISSION_GRANTED){
						permissionAccepted = false;
						break;
					}
				}
            if (!permissionAccepted ){
					for (int i = 0; i< permissions.length;++i){
						Log.e(TAG,String.format("Permission %s not granted %d",permissions[i],grantResults[i]));
					}
               finish();
            }else{
               Log.e(TAG,"Got record and storage permissions");
               ((Button) findViewById(R.id.buttonScan)).setEnabled(true);
               prepServices();
            }
            break;
        }
        
    }
	
	public void prepServices(){
		//BROADCASTSERVICE register the receiver
        if (dataReceiver != null) {
//Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_COMMUNICATION_TO_ACTIVITY"
            IntentFilter intentFilter = new IntentFilter(Constants.IMU);
				intentFilter.addAction(Constants.ECG);
				intentFilter.addAction(Constants.STOP_SERVICE);
				intentFilter.addAction(Constants.FOUND_DEVICE);
				intentFilter.addAction(Constants.STOP_SCAN);
				intentFilter.addAction(Constants.TOAST);
				intentFilter.addAction(Constants.HR);
				intentFilter.addAction(Constants.SETTINGSUPDATED);
				//Map the intent filter to the receiver
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					  registerReceiver(dataReceiver, intentFilter, RECEIVER_EXPORTED);
				}else {
					  registerReceiver(dataReceiver, intentFilter);
				}
            
        }
        //Start services
         ContextCompat.startForegroundService(this,new Intent(MoveSenseLog.this, MoveSenseService.class).setAction(Constants.START_SERVICE));

	}
	
	
	 //Broadcast receiver
    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
			//Update Scan results into the GUI list, get these from the service
			if (intent.getAction().equals(Constants.FOUND_DEVICE)){
				//Add device to the list
				String json = intent.getStringExtra(Constants.FOUND_DEVICE);
				// replace if exists already, add otherwise
				MyScanResult msr = (new Gson()).fromJson(json, MyScanResult.class);
				if (mScanResArrayList.contains(msr)){
					mScanResArrayList.set(mScanResArrayList.indexOf(msr), msr);
				}else{
					mScanResArrayList.add(0, msr);
				}
				mScanResArrayAdapter.notifyDataSetChanged();
			}

			//Show 
			if (intent.getAction().equals(Constants.TOAST)){
				String toastText = intent.getStringExtra(Constants.TOAST);
				Toast.makeText(context, toastText,Toast.LENGTH_SHORT).show();
			}

			// Deactivated graphicsView to improve performance of activity app
			//Update IMU
/* 			if (intent.getAction().equals(Constants.IMU)){
				float[][] rawData = new float[3][];
				float[][] gyrData = new float[3][];
				float[][] magData = new float[3][];
				rawData[0] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_X);
				rawData[1] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_Y); 
				rawData[2] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_Z); 
				gyrData[0] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_GYR_X);
				gyrData[1] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_GYR_Y); 
				gyrData[2] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_GYR_Z); 
				magData[0] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_MAG_X);
				magData[1] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_MAG_Y); 
				magData[2] = intent.getFloatArrayExtra(Constants.UPDATE_GRAPH_MAG_Z); 

				int accView = intent.getIntExtra(Constants.ACCVIEW,0);
				if (accView < graphicsViewAcc.length && graphicsViewAcc[accView].surfaceCreated){  
					graphicsViewAcc[accView].updateData(rawData,Constants.normalisation[0],"Acc [m/s2] "+intent.getStringExtra(Constants.BATTERY));
				}
				if (graphicsViewAcc[1].surfaceCreated){  
					graphicsViewAcc[1].updateData(gyrData,Constants.normalisation[1],"Gyr [deg/s]");
				}
				if (graphicsViewAcc[2].surfaceCreated){  
					graphicsViewAcc[2].updateData(magData,Constants.normalisation[2],"Mag [uT]");
				}				
			}
			
			//Update ECG
			if (intent.getAction().equals(Constants.ECG)){
				float[][] rawData = new float[1][];
				rawData[0] = intent.getFloatArrayExtra(Constants.ECG);
				int accView = intent.getIntExtra(Constants.ACCVIEW,0);
				if (accView < 1 && graphicsViewECG.surfaceCreated){  
					graphicsViewECG.updateData(rawData,Constants.normalisation[1],"ECG [V]");
				}
			} */

			//Update HR
			if (intent.getAction().equals(Constants.HR)){
					double hr = intent.getDoubleExtra(Constants.HR,-1d);
					//Update the HR textview here
					hrTextView.setText(String.format("HR %03d",(int) Math.round(hr)));
			 	}

			//Update stop scan button
			if (intent.getAction().equals(Constants.STOP_SCAN)){
				scanning = false;
				((Button) findViewById(R.id.buttonScan)).setText("Scan");
			}
			
			
			//Shutdown the program, if the service is closed 
			if (intent.getAction().equals(Constants.STOP_SERVICE)){
				//Log.e(LOG_TAG,"Received STOP_SERVICE INTENT");
				cleanUp();
				finish();
			}
			
			//Settings updated
			if (intent.getAction().equals(Constants.SETTINGSUPDATED)){
		      getSettings();
			}
			
				
        }
    };
    
    	//Settings need to be loaded only once, update when we get notifications...
	private void getSettings(){
		SharedPreferences sp = getSharedPreferences(Constants.APP_CLASS,Context.MODE_PRIVATE);
      ecgSR = sp.getString(Constants.ecgSR,"128");
      imuSR = sp.getString(Constants.imuSR,"52"); // replaced 104 with 52
	  hrToggle = sp.getString(Constants.hrToggle,"OFF");
      ecgSRText.setText("ECG SR "+ecgSR);
      imuSRText.setText("IMU SR "+imuSR);
	  hrText.setText("HR "+hrToggle);
	}
    
	
	//Listen for Settings activity result
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.e(TAG,String.format("ON ACTIVITY RESULT %d %d",requestCode,resultCode));
		//Settings finished
		if (requestCode == Constants.SETTINGSPAGEINTENT){
			if (resultCode == RESULT_OK){
				//String fileName = data.getStringExtra(Constants.FNAMES);
				//Log.e(TAG,"Got SETTINGS, broadcasting SETTINGSUPDATED "+dataReceiver.toString());
				//Send settings updated intent
				(new Handler()).postDelayed( new Runnable(){
						@Override
						public void run(){
							sendBroadcast(new Intent().setAction(Constants.SETTINGSUPDATED));
						}
					}, 200);  
				 
			}else{
				//Do nothing
			}
		}

		//Let Activity do with the result what it does... ?
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	
    public void onScanClicked(View view) {
		if (scanning == false){
			scanning = true;
			((Button) findViewById(R.id.buttonScan)).setText("Stop Scanning");
			// Start with empty list
			mScanResArrayList.clear();
			mScanResArrayAdapter.notifyDataSetChanged();
			//Fire an intent to start scanning in service
			//broadcast
			sendBroadcast(new Intent().setAction(Constants.SCAN));
		}else{
			onScanStopClicked();
		}
    }
	
	public void onBatteryClicked(View view) {
		sendBroadcast(new Intent().setAction(Constants.BATTERY));
	}

    public void onScanStopClicked() {
		//Broadcast stop scan
		sendBroadcast(new Intent().setAction(Constants.STOP_SCAN));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0 || position >= mScanResArrayList.size())
            return;

        MyScanResult device = mScanResArrayList.get(position);
		// Stop scanning
		onScanStopClicked();
		
		//Send device to Service
		Intent intent = new Intent();
		intent = intent.setAction(Constants.DEVICE);
		intent = intent.putExtra(Constants.DEVICE,(new Gson()).toJson(device));
		intent = intent.putExtra(Constants.ecgSR,ecgSR);
		intent = intent.putExtra(Constants.imuSR,imuSR);
		intent = intent.putExtra(Constants.hrToggle,hrToggle);
		sendBroadcast(intent);	//Send the intent to HRLog 
    }
	

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return true;
    }
	
	private void cleanUp(){
		unregisterReceiver(dataReceiver);
	}
	
		//Shutdown button listener, send broadcast to service (will send one back)    
	public void shutdownListener (View v) {
		//Log.e(LOG_TAG,"Send STOP_SERVICE to service");
		startService(new Intent(MoveSenseLog.this, MoveSenseService.class).setAction(Constants.STOP_SERVICE));
	}
	
	@Override
    protected void onDestroy() {
    		//BROADCAST SERVICE Unregister the receiver
		//Log.e(LOG_TAG,"onDestroy");
		try{
			cleanUp();
		}catch (Exception e){}
        super.onDestroy();
    }
    
 	 @Override
    protected void onPause() {
		//Log.e(LOG_TAG,"onPause called");
		try{
			cleanUp();	//Detach broadcast listener
	   }catch (Exception e){}
        super.onPause();
    }
    
     	 @Override
    protected void onResume() {
		//Log.e(LOG_TAG,"onResume called");
		try{
			prepServices();	//Re-attach broadcast listener
		}catch (Exception e){}
        super.onResume();
    }
    
    /**Toolbar*/
	/*Inflate the toolbar menu*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 MenuInflater inflater = getMenuInflater();
		 inflater.inflate(R.menu.toolbar, menu);
		 return true;
	}
	
	/*Listen to the toolbar*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		 if (item.getItemId() == R.id.modifySettings) {
		     		launchSettings();	//Launch settings
		         return true;
			
		 }else if (item.getItemId() == R.id.get_about){
		         // Launch Terms of use and policy, include graps + google maps eula!
		         Intent intent = new Intent(MoveSenseLog.this, TermsOfUse.class);
			  		startActivity(intent);
		         return true;
		 }else{
					// If we got here, the user's action was not recognized.
		         // Invoke the superclass to handle it.
		         return super.onOptionsItemSelected(item);

		 }
	}
	
	/**Settings activity handling*/
	private void launchSettings(){
		Intent intent = new Intent(MoveSenseLog.this, SettingsPage.class);
  		startActivityForResult(intent,Constants.SETTINGSPAGEINTENT);
	}

}
