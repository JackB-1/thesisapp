package com.dippa2.movesenseLog.data;

/**
 * Created by lipponep on 22.11.2017.
 * Modified from AccDataResponse by tjrantal at gmail dot com 2019
 */

import com.google.gson.annotations.SerializedName;

public class ECGData {

    @SerializedName("Body")
    public final Body body;
	
	@SerializedName("Uri")
	public final String uri;
	
	@SerializedName("Method")
	public final String method;

    public ECGData(Body body, String uri,String method) {
        this.body = body;
		this.uri = uri;
		this.method = method;
    }

    public static class Body {
		@SerializedName("Samples")
        public final short[] ecg;
		
        @SerializedName("Timestamp")
        public final long timestamp;

        

        public Body(short[] ecg, long timestamp) {
            this.ecg = ecg;
			this.timestamp = timestamp;
            
        }
    }

}
