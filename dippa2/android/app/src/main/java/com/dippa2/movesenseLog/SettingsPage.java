package com.dippa2.movesenseLog;
import com.dippa2.R;

import android.util.Log;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

import android.view.View;
import 	android.widget.Button;


//Save settings from session to the next
import android.content.Context;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

//TTS
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

//Debugging
import android.widget.Toast;

//Custom classes
import com.dippa2.movesenseLog.util.Constants;

public class SettingsPage extends Activity{
	private static final String TAG = "SettingsPage";


	
	
	Button cancelButton;
	Button saveButton;
	
	
	
	//ecgSR
   private Spinner ecgSRSpinner;
   private ArrayAdapter<String> eaa = null;
   String[] ecgRates = {"OFF","125", "128", "200", "250", "256", "500", "512"};
   
   //imuSR
	private Spinner imuSRSpinner;
   private ArrayAdapter<String> iaa = null;
   String[] imuRates = {"OFF","13", "26", "52", "104", "208", "416", "833", "1666"};
   
   private Spinner hrSpinner;
   private ArrayAdapter<String> haa = null;
   String[] hrToggles = {"OFF","ON"};
   
   String imuRate;
   String ecgRate;
   String hrToggle;

	     /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
  			setContentView(R.layout.settings);
  			

     
     
	  	
  		//Load settings
		loadSettings();
  		
  		//Get spinner handles	
	    ecgSRSpinner = (Spinner) findViewById(R.id.ecgSRList);
       imuSRSpinner = (Spinner) findViewById(R.id.imuSRList);
		hrSpinner = (Spinner) findViewById(R.id.hrList);	
		//Prep Spinners
		prepSpinner(ecgSRSpinner, eaa, getAList(ecgRates),ecgRate);
		prepSpinner(imuSRSpinner, iaa, getAList(imuRates),imuRate);
		prepSpinner(hrSpinner, haa, getAList(hrToggles),hrToggle);
		
		//Save button, will show the interstitial when settings are saved
		saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
        		@Override
				public void onClick(View v) {
                    settingsReady();	//Save settings, and return
					}
				}
        );
			
			//Cancel button
			cancelButton = (Button) findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
        		@Override
				public void onClick(View v) {
						finish();	//Will set the result to canceled by default
					}
				}
        );
        
    }
    
    private ArrayList<String> getAList(String[] items){
    	ArrayList<String> ret = new ArrayList<String>();
    	for (int i = 0; i<items.length;++i){
    		ret.add(items[i]);
    	}
    	return ret;
 	}
    
    
    private void prepSpinner(Spinner spinner, ArrayAdapter<String> ain, ArrayList<String> items,String selection){
    				ain = new ArrayAdapter<String>(SettingsPage.this,android.R.layout.simple_spinner_item,items);
					ain.setDropDownViewResource(R.layout.spinner_item);
					spinner.setAdapter(ain);
					
					int currentSelection = ain.getPosition(selection) > -1 ? ain.getPosition(selection) : 0;
					spinner.setSelection(currentSelection);
    }
    	
	//Call this to save settings (after showing the interstitial ad)
	private void settingsReady(){
		//Save settings here
		SharedPreferences sp = getSharedPreferences(Constants.APP_CLASS,Context.MODE_PRIVATE);
		SharedPreferences.Editor se = sp.edit();
		//Language selections
		se.putString(Constants.ecgSR,(String) ecgSRSpinner.getSelectedItem());
		se.putString(Constants.imuSR,(String) imuSRSpinner.getSelectedItem());
		se.putString(Constants.hrToggle,(String) hrSpinner.getSelectedItem());
		se.commit();
	
		//Send SettingsUpdated intent in ExercisePacer
		Intent returnIntent = new Intent();
		//returnIntent.putExtra("result",result);
		setResult(Activity.RESULT_OK,returnIntent);
		Log.e(TAG,String.format("Settings ready, return %d",Activity.RESULT_OK));
		
		finish();
	}
	
	//Reads settings from shared preferences
	private void loadSettings(){
		SharedPreferences sp = getSharedPreferences(Constants.APP_CLASS,Context.MODE_PRIVATE);
		ecgRate = sp.getString(Constants.ecgSR,"128");
		imuRate = sp.getString(Constants.imuSR,"52");
		hrToggle = sp.getString(Constants.hrToggle,"OFF");
	}
}
