package com.teamkite.youtubeuploader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.Scopes;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;
//import com.google.api.services.samples.youtube.cmdline.Auth;



public class YoutubeUploader extends UnityPlayerActivity {
	
	private static YouTube youtube;
	
	private static final String TAG = "YoutubeUploader";
	
	private static final String OnCompleted = "OnCompleted";
	private static final String OnAuth = "OnAuth";
	private static final String OnCancelled = "OnCancelled";
	private static final String OnFailed = "OnFailed";
	
    private static final String VIDEO_FILE_FORMAT = "video/*";
    
    public static YoutubeUploader activity;
    
    private static GoogleAccountCredential credential;
    
    public static final String[] SCOPES = {Scopes.PLUS_ME,YouTubeScopes.YOUTUBE_UPLOAD,YouTubeScopes.YOUTUBE_READONLY};
    
    public static final HttpTransport HTTP_TRANSPORT_DEFAULT = AndroidHttp.newCompatibleTransport();
    
    public static final JsonFactory JSON_FACTORY_DEFAULT = GsonFactory.getDefaultInstance();   
    
    private static String thisGameObjectCallBack;
    

    protected void onCreate(Bundle savedInstanceState) {
    	
		super.onCreate(savedInstanceState);
		activity = this;
    }

    public void onBackPressed(){
    }
  
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    	if (requestCode == 1) {
    		if(resultCode == RESULT_OK){
	            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
	            
	            
	            
	            AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {
	                @Override
	                protected Void doInBackground(String... params) {	
	                    getTokens(params[0]);
						return null;
	                }
	
	                @Override
	                protected void onPostExecute(Void token) {
	                    Log.i(TAG, "Access token retrieved:" + token);
	                }
	
	            };
	            task.execute(accountName);
    		}
    		else if(resultCode == RESULT_CANCELED){
    			UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnCancelled, "" );
    		}
    	}

    }
    public static void getTokens(String accountName){

        try {
        	    
        	credential = GoogleAccountCredential.usingOAuth2(activity,Arrays.asList(SCOPES));// Collections.singleton(YouTubeScopes.YOUTUBE_UPLOAD));
        	
        	credential.setSelectedAccountName(accountName);
        	
        	String tokens = credential.getToken();   
        	Log.i(TAG,"Tokens :"+tokens+ " callback "+thisGameObjectCallBack);
        	if(tokens != null){
        		UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnAuth, "" );
        	}
        	else{
        		UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, "Can't auth" );
        	}
        	

        }catch (GooglePlayServicesAvailabilityException playEx) {
        	Log.e(TAG,"GooglePlayServicesAvailabilityException authentication exception: " + playEx.getMessage());
        	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, playEx.getMessage() );
        }catch (UserRecoverableAuthException recoverableException) {
        	activity.startActivityForResult(recoverableException.getIntent(), 0);  
        	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed,recoverableException.getMessage() );
        	Log.i(TAG,"UserRecoverableAuthException authentication exception: " + recoverableException.getMessage());
        } catch (GoogleAuthException authEx) {
            // This is likely unrecoverable.
        	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, authEx.getMessage() );
        	Log.e(TAG,"Unrecoverable authentication exception: " + authEx.getMessage());      
        } catch (IOException e) {
        	Log.e(TAG,"IOException: " + e.getMessage());
        	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, e.getMessage() );
        	e.printStackTrace();
        }
    }
    
    public static void authGoogle(String gameObjectCallBack){
    	thisGameObjectCallBack = gameObjectCallBack;
    	
    	Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, "", null, null, null);
    	
    	activity.startActivityForResult(intent, 1);
    	
    }
    
    private static class BackgroundUpload extends AsyncTask<YouTube.Videos.Insert, Void, Void>{
    	 @Override
         protected Void doInBackground(YouTube.Videos.Insert... params) {
             try{          	
            	 YouTube.Videos.Insert videoInsert = params[0];
            	
            	 
            	 // Set the upload type and add an event listener.
                 MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

                 // Indicate whether direct media upload is enabled. A value of
                 // "True" indicates that direct media upload is enabled and that
                 // the entire media content will be uploaded in a single request.
                 // A value of "False," which is the default, indicates that the
                 // request will use the resumable media upload protocol, which
                 // supports the ability to resume an upload operation after a
                 // network interruption or other transmission failure, saving
                 // time and bandwidth in the event of network failures.
                 uploader.setDirectUploadEnabled(false);

                 MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                     public void progressChanged(MediaHttpUploader uploader) throws IOException {
                         switch (uploader.getUploadState()) {
                             case INITIATION_STARTED:
                             	Log.i(TAG,"Initiation Started");
                                 break;
                             case INITIATION_COMPLETE:
                             	Log.i(TAG,"Initiation Completed");
                                 break;
                             case MEDIA_IN_PROGRESS:
                             	Log.i(TAG,"Upload in progress");
                             	Log.i(TAG,"Upload percentage: " + uploader.getProgress());
                             	//UnityPlayer.UnitySendMessage("YoutubeUploaderEvents", "Completed", String.valueOf(uploader.getProgress()) );
                                 break;
                             case MEDIA_COMPLETE:
                             	Log.i(TAG,"Upload Completed!");
                             	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnCompleted, "" );
                                 break;
                             case NOT_STARTED:
                             	Log.i(TAG,"Upload Not Started!");
                                 break;
                         }
                     }
                 };
                 uploader.setProgressListener(progressListener);   	 
            	 
                 // Call the API and upload the video.
                 Video returnedVideo = videoInsert.execute();

                 // Print data about the newly inserted video from the API response.
                 Log.i(TAG,"\n================== Video Data ==================\n");
                 Log.i(TAG,"  - Id: " + returnedVideo.getId());
                 Log.i(TAG,"  - Title: " + returnedVideo.getSnippet().getTitle());
                 Log.i(TAG,"  - Tags: " + returnedVideo.getSnippet().getTags());
                 Log.i(TAG,"  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());
                 Log.i(TAG,"  - Video Count: " + returnedVideo.getStatistics().getViewCount());

             } catch (GoogleJsonResponseException e) {
             	Log.e(TAG,"GoogleJsonResponseException code: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
             	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, e.getDetails().getMessage() );
                 e.printStackTrace();
             } catch (IOException e) {
             	Log.e(TAG,"IOException: " + e.getMessage());
             	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, e.getMessage() );
                 e.printStackTrace();
             } catch (Throwable t) {
             	Log.e(TAG,"Throwable: " + t.getMessage());
             	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, t.getMessage() );
                 t.printStackTrace();
             }	
             
             return null;
         }

         @Override
         protected void onPostExecute(Void result) {
         }

         @Override
         protected void onPreExecute() {
         }

         @Override
         protected void onProgressUpdate(Void... values) {
         }


    }
    
    public static void  uploadVideo(String path,String title, String description,String[] tag){  	
    	// This OAuth 2.0 access scope allows an application to upload files
        // to the authenticated user's YouTube channel, but doesn't allow
        // other types of access.
        //List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.upload");
        try {
        	
    		youtube = new YouTube.Builder(HTTP_TRANSPORT_DEFAULT, JSON_FACTORY_DEFAULT, credential).setApplicationName("VideoUploader").build();
        	        	
            Log.i(TAG,"Uploading: " + path);

            // Add extra information to the video before uploading.
            Video videoObjectDefiningMetadata = new Video();

            // Set the video to be publicly visible. This is the default
            // setting. Other supporting settings are "unlisted" and "private."
            
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            videoObjectDefiningMetadata.setStatus(status);
           

            // Most of the video's metadata is set on the VideoSnippet object.
            VideoSnippet snippet = new VideoSnippet();

            snippet.setTitle(title);
            snippet.setDescription(description);

            // Set the keyword tags that you want to associate with the video.
            List<String> tags = new ArrayList<String>(Arrays.asList(tag));
         
            snippet.setTags(tags); 

            // Add the completed snippet object to the video resource.
            videoObjectDefiningMetadata.setSnippet(snippet);

            //InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT,context.getAssets().open(SAMPLE_VIDEO_FILENAME));
            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT,new FileInputStream(path));

            // Insert the video. The command sends three arguments. The first
            // specifies which information the API request is setting and which
            // information the API response should return. The second argument
            // is the video resource that contains metadata about the new video.
            // The third argument is the actual video content.
            YouTube.Videos.Insert videoInsert = youtube.videos().insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);
            
          
            new BackgroundUpload().execute(videoInsert);


        } catch (GoogleJsonResponseException e) {
        	Log.e(TAG,"GoogleJsonResponseException code: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
        	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, e.getDetails().getMessage() );
            e.printStackTrace();
        } catch (IOException e) {
        	Log.e(TAG,"IOException: " + e.getMessage());
        	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, e.getMessage() );
            e.printStackTrace();
        } catch (Throwable t) {
        	Log.e(TAG,"Throwable: " + t.getMessage());
        	UnityPlayer.UnitySendMessage(thisGameObjectCallBack, OnFailed, t.getMessage() );
            t.printStackTrace();
        }	
    	
    }
}