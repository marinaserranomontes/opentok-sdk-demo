package com.opentok.android.demo.helloworld;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.RelativeLayout;

import com.opentok.android.OpentokException;
import com.opentok.android.Publisher;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.demo.R;

/**This application demonstrates the basic workflow for getting started with the OpenTok Android SDK.
 * Basic hello-world activity shows publishing audio and video and subscribing to an audio and video stream
 */
public class HelloWorldActivity extends Activity implements Publisher.Listener, Subscriber.Listener, Session.Listener {

	private static final String LOGTAG = "demo-hello-world";
	private static final boolean AUTO_CONNECT = true;
	private static final boolean AUTO_PUBLISH = true;
	
	/*Fill the following variables using your own Project info from the Dashboard*/
    private static String API_KEY = "25173032"; // Replace with your API Key
    private static String SESSION_ID ="2_MX4yNTE3MzAzMn4xMjcuMC4wLjF-V2VkIEp1biAwNSAwMDozODowNSBQRFQgMjAxM34wLjk0MzEyODE3fg"; // Replace with your generated Session ID
	// Replace with your generated Token (use Project Tools or from a server-side library)
    private static String TOKEN = "T1==cGFydG5lcl9pZD0yNTE3MzAzMiZzZGtfdmVyc2lvbj10YnJ1YnktdGJyYi12MC45MS4yMDExLTAyLTE3JnNpZz0yYzJjOTE5ZWY3MTNmODJmMTRhNzAyNjQ4YTViNDkxZDA4MjE0NTYzOnJvbGU9cHVibGlzaGVyJnNlc3Npb25faWQ9Ml9NWDR5TlRFM016QXpNbjR4TWpjdU1DNHdMakYtVjJWa0lFcDFiaUF3TlNBd01Eb3pPRG93TlNCUVJGUWdNakF4TTM0d0xqazBNekV5T0RFM2ZnJmNyZWF0ZV90aW1lPTEzNzA0MTc5MzMmbm9uY2U9MC43MzA4MTIxOTE4ODIzODUmZXhwaXJlX3RpbWU9MTM3MzAwOTkzMyZjb25uZWN0aW9uX2RhdGE9";

    private RelativeLayout publisherView;
    private RelativeLayout subscriberView;
    private Publisher publisher;
    private Subscriber subscriber;
    private Session session;
    private WakeLock wakeLock;
    private boolean subscriberToSelf = true; // Change to false if you want to subscribe to streams other than your own.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	setContentView(R.layout.main_layout);
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
    	publisherView = (RelativeLayout) findViewById(R.id.publisherview);
    	subscriberView = (RelativeLayout) findViewById(R.id.subscriberview);
    	
		// Disable screen dimming
    	PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Full Wake Lock");
		
    	if (AUTO_CONNECT) {
            sessionConnect();
    	}
    }


    @Override
    public void onStop() {
    	super.onStop();
		
    	finishSession();
    	if (wakeLock.isHeld()) {
            wakeLock.release();
    	}
    	finish();
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
    	if (!wakeLock.isHeld()) {
            wakeLock.acquire();
    	}
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	if (wakeLock.isHeld()) {
            wakeLock.release();
    	}
    }
    
    private void sessionConnect() {
    	session = Session.newInstance(HelloWorldActivity.this, SESSION_ID, HelloWorldActivity.this);
    	session.connect(TOKEN);
    }
	

    public void finishSession() {
    	if (session != null) {
    	    session.disconnect();
        }
    }
	
    @Override
    public void onSessionConnected() {
		
    	Log.i(LOGTAG, "session connected");
    	
    	// Session is ready to publish.
    	if (AUTO_PUBLISH) {
    		//Create Publisher instance.
    		publisher = Publisher.newInstance(HelloWorldActivity.this);
    		publisher.setName("hello");
    		publisher.setListener(HelloWorldActivity.this);
			
			RelativeLayout.LayoutParams publisherViewParams=new RelativeLayout.LayoutParams(publisher.getView().getLayoutParams().width, publisher.getView().getLayoutParams().height);
			publisherViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			publisherViewParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			publisherViewParams.bottomMargin=measurePixels(8);
			publisherViewParams.rightMargin=measurePixels(8);;
			publisherView.setLayoutParams(publisherViewParams);
			publisherView.addView(publisher.getView());
			session.publish(publisher);		
		}
			
	}

	
    @Override
    public void onSessionDroppedStream(Stream stream) {
    	Log.i(LOGTAG, String.format("stream dropped", stream.toString()));
    	subscriber = null;
    	subscriberView.removeAllViews();
	}

    @Override
    public void onSessionReceivedStream(final Stream stream) {
    	Log.i(LOGTAG, "session received stream");

		//If this incoming stream is our own Publisher stream and subscriberToSelf is true let's look in the mirror.
		if ((subscriberToSelf && session.getConnection().equals(stream.getConnection()) ) || 
			(!subscriberToSelf && !(session.getConnection().getConnectionId().equals(stream.getConnection().getConnectionId())))){
			subscriber = Subscriber.newInstance(HelloWorldActivity.this, stream);
			RelativeLayout.LayoutParams subscriberViewParams=new RelativeLayout.LayoutParams(780, getResources().getDisplayMetrics().heightPixels);
			subscriberViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			subscriberView.addView(subscriber.getView());
			subscriberView.getChildAt(0).setLayoutParams(subscriberViewParams);
			subscriber.setListener(HelloWorldActivity.this);
			session.subscribe(subscriber);
		}
	}

    @Override
    public void onSubscriberConnected(Subscriber subscriber) {
    	Log.i(LOGTAG, "subscriber connected");
    }

    @Override
    public void onSessionDisconnected() {
    	Log.i(LOGTAG, "session disconnected");
    	showAlert("Session disconnected: " + session.getSessionId());
    }

    @Override
    public void onSessionException(OpentokException exception) {
    	Log.e(LOGTAG, "session failed! " + exception.toString());
    	showAlert("There was an error connecting to session " + session.getSessionId());
    }
	
    @Override
    public void onSubscriberException(Subscriber subscriber, OpentokException exception) {
    	Log.i(LOGTAG, "subscriber " + subscriber + " failed! " + exception.toString());
    	showAlert("There was an error subscribing to stream " + subscriber.getStream().getStreamId());
    }

    @Override
    public void onPublisherChangedCamera(int cameraId) {
    	Log.i(LOGTAG, "publisher changed camera to cameraId: " + cameraId);
    }

    @Override
    public void onPublisherException(OpentokException exception) {
    	Log.i(LOGTAG, "publisher failed! " + exception.toString());
    	showAlert("There was an error publishing");
    }

    @Override
    public void onPublisherStreamingStarted() {
    	Log.i(LOGTAG, "publisher is streaming!");
    	if (subscriberView.getChildCount() != 0) {
    		subscriberView.removeViewAt(0);
    		subscriberView.addView(subscriber.getView());
    	}
    }

    @Override
    public void onPublisherStreamingStopped() {
    	Log.i(LOGTAG, "publisher disconnected");
    }

    public Publisher getPublisher() {
		return publisher;
	}

	public Subscriber getSubscriber() {
		return subscriber;
	}

	public Session getSession() {
		return session;
	}
    
	public RelativeLayout getPublisherView() {
		return publisherView;
	}

	public RelativeLayout getSubscriberView() {
		return subscriberView;
	}

	//utils
    private int measurePixels(int dp) {
    	double screenDensity = getResources().getDisplayMetrics().density;
		return (int) (screenDensity * dp);
    }
    
    private void showAlert(String message) {
    	if (!this.isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Message from video session ");
            builder.setMessage(message);
            builder.setPositiveButton("OK", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int arg1) {
					dialog.cancel();	
				}
			});

            builder.create();
            builder.show();
    	}
    }
}
