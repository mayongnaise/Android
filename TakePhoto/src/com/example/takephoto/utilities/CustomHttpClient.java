package com.example.takephoto.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class CustomHttpClient {
	/** The time it takes for our client to timeout */
    public static final int HTTP_TIMEOUT = 0; // milliseconds

    /** Single instance of our HttpClient */
    private static HttpClient mHttpClient;

    /**
     * Get our single instance of our HttpClient object.
     */
    private static HttpClient getHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
            final HttpParams params = mHttpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, HTTP_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, HTTP_TIMEOUT);
            ConnManagerParams.setTimeout(params, HTTP_TIMEOUT);
        }
        return mHttpClient;
    }

    /**
     * Performs an HTTP Post request to the specified url with the
     * specified parameters.
     */
    public static String executeHttpPost(String url, ArrayList<NameValuePair> postParameters) throws Exception {
        BufferedReader in = null;
        try {
            HttpClient client = getHttpClient();
            HttpPost request = new HttpPost(url);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
            request.setEntity(formEntity);
            HttpResponse response = client.execute(request);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            response.getEntity().consumeContent();
            String result = sb.toString();
            return result;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Integer checkURLConnection(String url) throws Exception {
    	
    	URL link = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) link.openConnection();
        
        return conn.getResponseCode();
        
    }
    
    /**
     * Performs an HTTP GET request to the specified url.
     *
     * @param url The web address to post the request to
     * @return The result of the request
     * @throws Exception
     */
    public static String executeHttpGet(String url) throws Exception {
       
    	BufferedReader in = null;
        
    	try {
            HttpClient client = getHttpClient();
            HttpGet request = new HttpGet();
            
            request.setURI(new URI(url));
            
            HttpResponse response = client.execute(request);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
           
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
           
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            
            in.close();
            
            response.getEntity().consumeContent();
            
            return sb.toString();
            
        }
    	catch(Exception e){
    		
    		e.printStackTrace();
    		
    		return "0";
    		
    	}
         
    	finally {
    		if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
