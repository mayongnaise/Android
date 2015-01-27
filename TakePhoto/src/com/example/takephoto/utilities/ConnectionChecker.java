package com.example.takephoto.utilities;

import java.util.ArrayList;

import org.apache.http.NameValuePair;

import com.example.takephoto.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectionChecker {

	public ConnectionChecker(){
		
	}
	
	public boolean checkForInternetConnection(Activity activity){
		
		ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    	
		NetworkInfo netInfo  = cm.getActiveNetworkInfo();

		if (netInfo != null && netInfo.isConnectedOrConnecting()) {

			return true;

		}

		return false;

	}


	public void checkServerConnectionError(final Activity activity, String url, ArrayList<NameValuePair> postParameters){

		String res = "";

		try {

			res = CustomHttpClient.executeHttpPost(url, postParameters);

		}
		catch (Exception e1) {

			e1.printStackTrace();

		}

		if(res.equalsIgnoreCase("") || res.isEmpty() || res.equalsIgnoreCase("null")){

			activity.runOnUiThread(new Runnable() {

				public void run() {

					if(!activity.isFinishing()){
						
						AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
	
						alertDialog.setTitle("Failed to Connect to Server");
	
						alertDialog.setMessage("Unknown error has occured. \n\n Possible causes: \n * Connection refused. \n * Network is not the same with web service");
	
						alertDialog.setIcon(R.drawable.fail);
	
						alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	
							public void onClick(DialogInterface dialog, int which) {
	
								activity.finish();
	
							}
						});
	
						alertDialog.show();
						
					}
				}
			});

		}
		else{

			res = res.replaceAll("<(.*?)\\>"," ");

			res = res.replace( "/<[^>]+>/g", " ");

			res = res.replaceAll("<(.*?)\\\n"," ");

			res = res.replaceFirst("(.*?)\\>", " ");

			res = res.replaceAll("&nbsp;"," ");

			res = res.replaceAll("&amp;"," ");

			res = res.replaceAll("&lt;","<");

			res = res.replaceAll("&gt;",">");

			final String res1 = res;

			activity.runOnUiThread(new Runnable() {

				public void run() {

					AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

					alertDialog.setTitle("Failed to Connect to Server");

					alertDialog.setMessage(res1);

					alertDialog.setIcon(R.drawable.fail);

					alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {

							//finish();

						}
					});

					alertDialog.show();

				}
			});

		}

	}
	
}
