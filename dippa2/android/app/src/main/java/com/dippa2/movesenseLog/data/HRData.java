package com.dippa2.movesenseLog.data;

/**
 * Created by lipponep on 22.11.2017.
 * Modified from AccDataResponse by tjrantal at gmail dot com 2020
 */

import com.google.gson.annotations.SerializedName;

public class HRData {

    @SerializedName("Body")
    public final Body body;
	
	@SerializedName("Uri")
	public final String uri;
	
	@SerializedName("Method")
	public final String method;

    public HRData(Body body, String uri,String method) {
        this.body = body;
		this.uri = uri;
		this.method = method;
    }

    public static class Body {
		@SerializedName("average")
        public final double hr;
		
        @SerializedName("rrData")
        public final int[] rrData;

        public Body(double hr, int[] rrData) {
            this.hr = hr;
			this.rrData = rrData;
        }
    }
}
