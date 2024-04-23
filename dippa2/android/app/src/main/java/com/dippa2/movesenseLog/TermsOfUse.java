package com.dippa2.movesenseLog;
import com.dippa2.R;

//Activity
import android.app.Activity;
import android.os.Bundle;


//webview
import android.webkit.WebView;

//Utils
import com.dippa2.movesenseLog.util.RawResourceReader;

public class TermsOfUse extends Activity{
	private static final String TAG = "TermsOfUse";
	     /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
  			setContentView(R.layout.terms);
	  
		  //WebView for EULA  			
  			WebView wv = (WebView) findViewById(R.id.terms);
			String eula = RawResourceReader.readTextFileFromRawResource(this, R.raw.terms);
			wv.loadData(eula, "text/html", null);
    }
	
	
	
	/*Power saving*/
     protected void onResume() {
     		super.onResume();
     }

     protected void onPause() {
      		super.onPause();
     }
	
	protected void onDestroy(){
		//BROADCAST the selected files and/or set the return intent here
      super.onDestroy();
	}
    
}
