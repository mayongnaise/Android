package com.example.takephoto.activities;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.takephoto.R;
import com.example.takephoto.utilities.AndroidMultiPartEntity;
import com.example.takephoto.utilities.AndroidMultiPartEntity.ProgressListener;
import com.example.takephoto.utilities.ConnectionChecker;
import com.example.takephoto.utilities.CustomHttpClient;

public class MainActivity extends Activity{

	Activity activity = null;
	
	ArrayList<Uri> uriLists = new ArrayList<Uri>();
	ArrayList<Bitmap> imageLists = new ArrayList<Bitmap>();
	
	Button takePhoto;

	ConnectionChecker connectionChecker;
	
	InputStream inputStream;

	int resultOfUpload = 0, item_count = 0;
	
	LinearLayout items; 
	
	long totalSize = 0;
	
	ProgressDialog pDialog;
	
	String path = "";
	
	Uri pictureUri;
		
	ViewHolder view;
	
	private static final int CAMERA_REQUEST = 1;
	public static final int MEDIA_TYPE_IMAGE = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);

		activity = MainActivity.this;
		
		initializeAllFields();

		 // Checking camera if supported
        if (!isDeviceSupportCamera()) {
           
        	Toast.makeText(getApplicationContext(), "Sorry! Your device doesn't support camera", Toast.LENGTH_LONG).show();
           
            finish();
        
        }
        
        // Getting all uploaded images from server
        new GetImagesFromServer().execute();
        
	}
	
	public void initializeAllFields(){

		takePhoto = (Button) findViewById(R.id._b__useCamera);
		
		items = (LinearLayout) findViewById(R.id._ll__items);
		
		takePhoto.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
			
				startCamera();
				
			}
		});
		
	}
	
	// Calling Camera Intent for Photo Capturing
	public void startCamera(){
		
		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE); 
		
        startActivityForResult(cameraIntent, CAMERA_REQUEST); 
	    
	}
    
	 
	// Receiving activity result method after closing the camera
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
        // if the result is capturing Image
        if (requestCode == CAMERA_REQUEST) {
        	
            if (resultCode == RESULT_OK) {
            	
            	pictureUri = data.getData();
            	
            	uriLists.add(pictureUri); // Save URI of captured image to ArrayList to handle uploading of each images
            	
				path = getPath(pictureUri);
				
            	// Compressing bitmap	
            	Bitmap usableBitmap = compressBitmap(pictureUri);
			
            	// Adding each captured photo as table row
				addCapturedPhotoToView(usableBitmap);
			
            } else if (resultCode == RESULT_CANCELED) {                 
                
            	// Canceling camera will display this message
                Toast.makeText(getApplicationContext(), "User cancelled image capture", Toast.LENGTH_SHORT).show();
             
            } else {
            
            	// Unable to make a successful capture
                Toast.makeText(getApplicationContext(), "Sorry! Failed to capture image", Toast.LENGTH_SHORT).show();
         
            }
         
        }
        
    }
    
    // Add each photo to a Custom and Dynamic Table Rows
    public void addCapturedPhotoToView(Bitmap bm){
    	
    	TableRow tr = new TableRow(this);
    	
    	DisplayMetrics dm = new DisplayMetrics();
        
	    getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
	       
	    int height = dm.heightPixels / 6;
			  
		final LinearLayout ll = (LinearLayout) View.inflate(this, R.layout.layout_items, null);
		TableRow.LayoutParams head_params = new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, height);
		ll.setLayoutParams(head_params);
		
		view = new ViewHolder();
        
		view.imageItem = (ImageView) ll.findViewById(R.id._img__preview);
        view.progressBar = (ProgressBar) ll.findViewById(R.id._pb__uploading);
        view.status = (TextView) ll.findViewById(R.id._tv__status);

        view.imageItem.setTag(item_count);
        
        view.imageItem.setImageBitmap(bm);

        tr.addView(ll);
    	
		items.addView(tr);

    	item_count++;
    	
		runOnUiThread(new Runnable() {
			
			public void run() {
			
				// Calling uploading of image
		        new UploadFileToServer().execute();
		        
			}
		});
		
    }
    
    /**
     * AsyncTask that will get list of image file links using webservice and download all images
     */
    @SuppressLint("NewApi")
	public class GetImagesFromServer extends AsyncTask<String, String, String> {
	
    	int success = 0;
    	
    	String url = "";

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			
			pDialog = new ProgressDialog(activity);
			pDialog.setMessage("Getting images...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}
		    	
		@Override
		protected String doInBackground(String... params) {
			
			// Getting all image URL links from server
			getListOfImages();
			
			return null;
			
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			
			pDialog.dismiss();

			// Add each images as table rows
   			addImageFromServerToList();
   			
		}

    }
    
    public void getListOfImages(){
    	
    	String url = "";
    	
    	String url_get_image_lists =  "http://npoolshop.com/images/get_image_lists.php";
		
		ArrayList<NameValuePair> params1 = new ArrayList<NameValuePair>();
		
   		try {
   			
   			String response = CustomHttpClient.executeHttpPost(url_get_image_lists, params1);
		    
		    JSONObject json = new JSONObject(response.toString());
		    
   			JSONArray images = json.getJSONArray("images");
   			
   			for (int i = 0; i < images.length(); i++) {
   				
   				JSONObject c = images.getJSONObject(i);
				
   				url = c.getString("url");

   				// Download images from server and compress bitmap
       			Bitmap bm = downloadImage(url);
       			
       			// Save bitmaps to ArrayList
       			imageLists.add(bm);
       			
       		}
   			
   		}catch (Exception e) {
   			
   			e.printStackTrace();
       		
   			// Network Manager
   			connectionChecker = new ConnectionChecker();
   			
       		if(connectionChecker.checkForInternetConnection(activity) ==false){
       			
       			AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
   				 
		        alertDialog.setTitle("Network Error");

		        alertDialog.setMessage("You don't have an Internet Connection.");

		        alertDialog.setIcon(R.drawable.fail);

		        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
		        	
		        });
		        
		        alertDialog.show();
       		
       		} else {
       		
       			connectionChecker.checkServerConnectionError(activity, url_get_image_lists, params1);
       		
       		}
   		}
   		
    }
   
    private Bitmap downloadImage(String url) {
        
        Bitmap bm = null;
        
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();

            BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis, null, options);
            
            bis.close();
            is.close();
            
        } catch (IOException e) {
            
        	Log.e("Image Download","Error getting the image from server : " + e.getMessage().toString());
        
        } 
        
        return bm;
        
    }
    
    public void addImageFromServerToList(){
    	
    	for (int i = 0; i < imageLists.size(); i++) {
		
	    	TableRow tr = new TableRow(this);
	    	
	    	DisplayMetrics dm = new DisplayMetrics();
	        
		    getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
		       
		    int height = dm.heightPixels / 6;
				  
			final LinearLayout ll = (LinearLayout) View.inflate(this, R.layout.layout_items, null);
			TableRow.LayoutParams head_params = new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, height);
			ll.setLayoutParams(head_params);
			
			view = new ViewHolder();
	        
			view.imageItem = (ImageView) ll.findViewById(R.id._img__preview);
	        view.progressBar = (ProgressBar) ll.findViewById(R.id._pb__uploading);
	        view.status = (TextView) ll.findViewById(R.id._tv__status);
	
	        view.imageItem.setTag(item_count);
	        
	        view.imageItem.setImageBitmap(imageLists.get(i));
	
	        view.status.setText(getResources().getString(R.string.uploaded));
	        view.status.setTextColor(Color.parseColor("#00ff00"));
	        
	        tr.addView(ll);

			items.addView(tr);
			
	    	item_count++;
	    	
	    	uriLists.add(Uri.parse(imageLists.get(i).toString()));
    	
    	}
    	
    }

    /**
	 * Uploading the file to server
	 * */
	private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
		 
		// Getting Item Views
		ProgressBar pBar = (ProgressBar) ((RelativeLayout)((LinearLayout) ((TableRow) items.getChildAt((int) 
				view.imageItem.getTag())).getChildAt(0))
				.getChildAt(1)).getChildAt(0);
		TextView stat = (TextView) ((RelativeLayout)((LinearLayout) ((TableRow) items.getChildAt((int) 
				view.imageItem.getTag())).getChildAt(0)).getChildAt(1)).getChildAt(1);
		
		File sourceFile = null;
		Uri localUri = null;
		int index = (int) view.imageItem.getTag();
		
		@Override
		protected void onPreExecute() {
			// setting progress bar to zero
			pBar.setProgress(0);
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			// Making progress bar visible
			pBar.setVisibility(View.VISIBLE);

			// updating progress bar value
			pBar.setProgress(progress[0]);

			// updating percentage value
			stat.setText(String.valueOf(progress[0]) + "%");
			
		}

		@Override
		protected String doInBackground(Void... params) {
			return uploadFile();
		}

		@SuppressWarnings("deprecation")
		private String uploadFile() {

			String responseString = null;

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://npoolshop.com/images/fileUpload.php");

			try {
				
				AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
					
						new ProgressListener() {
	
						@Override
						public void transferred(long num) {
							publishProgress((int) ((num / (float) totalSize) * 100));
						}
					});
	
				sourceFile = new File(path);
				
				localUri = uriLists.get(index);
				
				// Adding file data to http body
				entity.addPart("image", new FileBody(sourceFile));

				totalSize = entity.getContentLength();
				httppost.setEntity(entity);

				// Making server call
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity r_entity = response.getEntity();

				int statusCode = response.getStatusLine().getStatusCode();

				if (statusCode == 200) {
					// Server response
					responseString = EntityUtils.toString(r_entity);
					resultOfUpload = 1;
					
				} else {
					responseString = "Error occurred! Http Status Code: "
							+ statusCode;
					resultOfUpload = 0;
				}

			} catch (ClientProtocolException e) {
				responseString = e.toString();
				resultOfUpload = 0;
			} catch (IOException e) {
				responseString = e.toString();
				resultOfUpload = 0;
			}

			return responseString;

		}

		@Override
		protected void onPostExecute(String result) {

			// Evaluating resut
			if(resultOfUpload == 0){

				stat.setText("Error: "+result);
		
			}
			else{
				
				pBar.setVisibility(View.GONE);
				stat.setText(getResources().getString(R.string.uploaded));
				stat.setTextColor(Color.parseColor("#00ff00"));
				
				getContentResolver().delete(localUri, null, null);
				
			}

			super.onPostExecute(result);
		}

	}
	
	
	/*
	 * -----------------------------Utilities-----------------------------------
	 */
	
	private Bitmap compressBitmap(Uri uri){

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 4;
		AssetFileDescriptor fileDescriptor =null;
		
		try {
	    
			fileDescriptor = this.getContentResolver().openAssetFileDescriptor(uri, "r");
		
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
		
		return BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
		
	}
	
	private String getPath(Uri uri) {
	    String[]  data = { MediaStore.Images.Media.DATA };
	    CursorLoader loader = new CursorLoader(getApplicationContext(), uri, data, null, null, null);
	    Cursor cursor = loader.loadInBackground();
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    cursor.moveToFirst();
	    
	    String a = cursor.getString(column_index);
	    
	    cursor.close();
	    
	    return a;
	}
	
	// View Holder Class for the inflated Layout
    public static class ViewHolder
    {

        public ImageView imageItem;
        public ProgressBar progressBar;
        public TextView status;
        
    }
    
	// Checking device has camera hardware or not
    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    
}
