package com.dippa2.movesenseLog.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;

public class RawResourceReader{
	private static final String TAG = "RawResourceReader";
	public static String readTextFileFromRawResource(final Context context,
			final int resourceId){
		final InputStream inputStream = context.getResources().openRawResource(
				resourceId);
		final InputStreamReader inputStreamReader = new InputStreamReader(
				inputStream);
		final BufferedReader bufferedReader = new BufferedReader(
				inputStreamReader);

		String nextLine;
		final StringBuilder body = new StringBuilder();

		try
		{
			while ((nextLine = bufferedReader.readLine()) != null)
			{
				body.append(nextLine);
				body.append('\n');
			}
		}
		catch (IOException e)
		{
			return null;
		}

		return body.toString();
	}
	
	//Read doubles from file
	public static double[][] readDoublesFromRawResource(final Context context,
			final int resourceId,String separator){
		final InputStream inputStream = context.getResources().openRawResource(
				resourceId);
		final InputStreamReader inputStreamReader = new InputStreamReader(
				inputStream);
		final BufferedReader bufferedReader = new BufferedReader(
				inputStreamReader);

		String nextLine;
		ArrayList<ArrayList<Double>> values = new ArrayList<ArrayList<Double>>();
		try
		{
			while ((nextLine = bufferedReader.readLine()) != null)
			{
				values.add(new ArrayList<Double>());
				String[] splitValues = nextLine.trim().split(separator);
				for (int v = 0; v<splitValues.length; ++v){
					values.get(values.size()-1).add(Double.parseDouble(splitValues[v]));
				}
			}
		}
		catch (IOException e)
		{
			return null;
		}
		
		//Pop the data into a 2D array
		double[][] ret = new double[values.size()][]; 
		for (int r = 0; r<values.size(); ++r){
			ret[r] = new double[values.get(r).size()];
			for (int c=0;c<values.get(r).size(); ++c){
				ret[r][c] = values.get(r).get(c);
			}
		}
		return ret;
	}
}
