package org.bbbmanager.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;

import org.bbbmanager.agent.common.servlet.Configuration;

public abstract class HTTPUtils {

	public static InputStream getUrlInputStream(String strUrl) throws IOException {
		Integer readTimeout = Integer.parseInt(Configuration.getConfig("common.http.timeout.read").trim());
		Integer connectTimeout = Integer.parseInt(Configuration.getConfig("common.http.timeout.connect").trim());
		
		return getUrlInputStream(strUrl, readTimeout, connectTimeout);
	}
	
	
	public static InputStream getUrlInputStream(String strUrl, Integer readTimeout, Integer connectTimeout) throws IOException {
		URL url = new URL(strUrl);
		
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Request-Method", "GET");  
		connection.setDoInput(true);  
		connection.setDoOutput(false);
		connection.setReadTimeout(readTimeout);
		connection.setConnectTimeout(connectTimeout);
		connection.setUseCaches(false);
		connection.connect();
		
		return connection.getInputStream();
	}
	
	/**
	 * Do a post and return sucess boolean
	 * */
	public static Boolean doPost(String strURL, Hashtable<String, String> params) throws IOException {
		URL url = new URL(strURL);
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Request-Method", "GET");
		connection.setRequestProperty("Accept-Language", "en");
		connection.setDoInput(true);  
		connection.setDoOutput(true);
		connection.setReadTimeout(10000);
		connection.setConnectTimeout(10000);
		connection.setUseCaches(false);
		connection.setRequestMethod("POST");
		connection.connect();

		OutputStream os = connection.getOutputStream();
		os.write(getPostParamString(params).getBytes());
		
		InputStream is = connection.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		
		String line;
		while((line = br.readLine()) != null){
			line = null;
			//Dont use output of server
		}
		
		return (connection.getResponseCode() == 200);
	}
	
	/**
	 * Convert a hashtable in a string for writing the post
	 * */
	private static String getPostParamString(Hashtable<String, String> params) throws UnsupportedEncodingException {
	    if(params.size() == 0)
	        return "";

	    StringBuffer buf = new StringBuffer();
	    Enumeration<String> keys = params.keys();
	    while(keys.hasMoreElements()) {
	        buf.append(buf.length() == 0 ? "" : "&");
	        String key = keys.nextElement();
	        buf.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(params.get(key), "UTF-8"));
	    }
	    return buf.toString();
	}
}
