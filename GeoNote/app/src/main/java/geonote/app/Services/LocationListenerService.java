package geonote.app.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;

import java.util.HashSet;

import geonote.app.Activity.NoteViewActivity;
import geonote.app.Constants;
import geonote.app.NoteInfo;
import geonote.app.NotesManager;
import geonote.app.NotesRepository;
import geonote.app.R;
import geonote.app.Settings;

public class LocationListenerService extends Service implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "LocationListenerService";
    protected LocationRequest mLocationRequest;
    protected Location mLastLocation = null;
    protected GoogleApiClient mGoogleApiClient;
    protected NotificationManager mNotificationManager = null;
    protected HashSet<NoteInfo> mSentNotifications = new HashSet<>();
    private Settings mSettings;
    protected NotesRepository mNotesRepository;
    protected NoteInfo mCurrentShownNotificationNote = null;
    boolean mTrackingStarted = false;

    // Binder given to clients
    private final IBinder mBinder = new LocationListenerBinder();

    public static interface OnLocationChangedListener {
        void onLocationChanged(Location location);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocationListenerBinder extends Binder {
        public OnLocationChangedListener mOnLocationChangedListener;

        public LocationListenerService getService() {
            // Return this instance of LocationListenerService so clients can call public methods.
            return LocationListenerService.this;
        }
    }

    public Location getLastLocation(){
        return mLastLocation;
    }

    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate");
        this.mSettings = new Settings(this.getBaseContext());

        this.mNotificationManager =
                (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);

        mNotesRepository = NotesRepository.Instance;

        if (mNotesRepository == null) {
            loadNotes();
            // also make sure to re-load notes if the notes are modified.
            mNotesRepository.mNotesModifiedListener = new NotesRepository.NotesModifiedListener() {
                @Override
                public void OnNotesModified() {
                    loadNotes();
                }
            };
        }

        createLocationRequest();

        buildGoogleApiClient();

        super.onCreate();
    }

    private void loadNotes(){
        SharedPreferences settings = this.getSharedPreferences(Constants.PREFS_NOTES, 0);
        mNotesRepository = new NotesRepository(null);
        NotesManager.loadNotesFromLocalStore(settings, mNotesRepository);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    protected void createLocationRequest() {
        Log.d(TAG, "createLocationRequest");
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(6 * 60 * 1000); // 1 hour
            mLocationRequest.setFastestInterval(60 * 1000); // 1 minute
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
    }

    // region Overrides for GoogleApiClient.ConnectionCallbacks

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates");
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this);
    }

    // endregion

    // region Overrides for GoogleApiClient.OnConnectionFailedListener

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed");
        stopSelf();
    }

    // endregion

    // region Overrides for LocationListener

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "onLocationChanged");
        this.mLastLocation = location;

        LocationListenerBinder binder = ((LocationListenerBinder)mBinder);

        if (binder != null) {
            if (binder.mOnLocationChangedListener != null) {
                binder.mOnLocationChangedListener.onLocationChanged(location);
            }
        }
        if (!mSettings.isNotificationsEnabled()) {
            return ;
        }

        // check if there is a note in the nearby location.
        float closestMatch = Integer.MAX_VALUE;
        NoteInfo noteInfoToNotifyOn = null;

        for(NoteInfo noteInfo: this.mNotesRepository.Notes.values())
        {
            // if we have a note within about GEO_FENCE_RADIUS meters from where we are,
            // and the note requested for an alert, send a notification.
            float distanceFromNote = noteInfo.getDistanceFrom(mLastLocation);
            if (distanceFromNote < mSettings.getGeoFenceRadius()) {
                if (closestMatch > distanceFromNote && noteInfo.getEnableRaisingEvents())
                {
                    closestMatch = distanceFromNote;
                    noteInfoToNotifyOn = noteInfo;
                }
            }
        }

        if (noteInfoToNotifyOn != null) {
            // send the notification from the closest note only if we havent already sent it.
            // TODO - do we need to remember this for a time period too?
            if (!mSentNotifications.contains(noteInfoToNotifyOn)) {
                sendNotification(noteInfoToNotifyOn.toString(), noteInfoToNotifyOn);
                mSentNotifications.add(noteInfoToNotifyOn);
            }
        }

        // if the posted notification is outside of GEO_FENCE_RADIUS,
        // cancel the sent notification.
        if (mCurrentShownNotificationNote!=null && mCurrentShownNotificationNote.getDistanceFrom(mLastLocation) >= mSettings.getGeoFenceRadius()) {
            mNotificationManager.cancel(Constants.CURRENT_NOTIFICATION_ID);
        }
    }

    protected void sendNotification(String notificationContents, NoteInfo noteInfo) {
        Log.e(TAG, "sendNotification :" + "for note " + noteInfo.toString());

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this.getBaseContext())
                        .setSmallIcon(R.drawable.notespin)
                        .setContentTitle("Note available at nearby location")
                        .setAutoCancel(true)
                        .setContentText(notificationContents);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this.getBaseContext(), NoteViewActivity.class);
        resultIntent.putExtra("noteInfoExtra", noteInfo);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.getBaseContext());

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(NoteViewActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;

        mNotificationManager.notify(Constants.CURRENT_NOTIFICATION_ID, notification);
        mCurrentShownNotificationNote = noteInfo;
    }

    // endregion
}