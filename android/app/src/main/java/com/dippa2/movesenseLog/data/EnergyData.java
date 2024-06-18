package com.dippa2.movesenseLog.data;

/**
 * Created by lipponep on 22.11.2017.
 * Modified from AccDataResponse by tjrantal at gmail dot com 2019
 */

import com.google.gson.annotations.SerializedName;

public class EnergyData {
	
	@SerializedName("Content")
    public final Content content;

    public EnergyData(Content content) {
        this.content = content;
    }

	public static class Content {
		@SerializedName("Percent")
		public final int percent;

		@SerializedName("MilliVoltages")
		public final int mV;

		@SerializedName("InternalResistance")
		public final int internalR;

		public Content(int percent, int mV,int internalR) {
			this.percent = percent;
			this.mV = mV;
			this.internalR = internalR;
		}
	}
}
