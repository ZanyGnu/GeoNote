package geonote.app.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import java.util.Locale;

import geonote.app.Activity.MainActivity;
import geonote.app.Constants;
import geonote.app.NotesManager;
import geonote.app.NotesRepository;
import geonote.app.R;

public class SplashScreenFragment extends BaseFacebookHandlerFragment {

    View mCurrentView = null;
    NotesManager mNotesManager = null;
    NotesRepository mNotesRepository = null;
    TextView mSplashScreenTextView = null;
    Time startupTime = new Time();
    int splashScreenShowTimeMillis = 3*1000;

    public SplashScreenFragment() {
        startupTime.setToNow();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mCurrentView = inflater.inflate(R.layout.fragment_splash_screen, container, false);

        mSplashScreenTextView = (TextView) mCurrentView.findViewById(R.id.splashScreenText);

        mNotesManager = new NotesManager();
        mNotesRepository = new NotesRepository(new Geocoder(getActivity().getBaseContext(), Locale.getDefault()));

        try {
            NotesRepository.SetInstance(mNotesRepository);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mNotesManager.mOnNotesLoadedListener  = new NotesManager.OnNotesLoadedListener() {
            @Override
            public void onNotesLoaded() {
                mSplashScreenTextView.append("\nNotes loaded.");
                launchMainActivity();
            }
        };
        try{
            ApplicationInfo info = getActivity().getPackageManager().getApplicationInfo("com.facebook.android", 0);
            mSplashScreenTextView.append("\nLogging into facebook.");
        } catch( PackageManager.NameNotFoundException e ){
            mSplashScreenTextView.append("\nNo facebook app found.");
            mNotesManager.loadNotes(getActivity(), mNotesRepository, null);
        }

        return mCurrentView;
    }

    public void loadNotes(Activity activity, final String userName) {

        // TODO: Load notes only if not already loaded
        mSplashScreenTextView.append("\nRetrieving notes.");
        mNotesManager.loadNotes(activity, mNotesRepository, userName);
    }

    protected void onSessionStateChange(Session session, SessionState state, Exception exception){
        if (session != null && session.isOpened()) {
            mSplashScreenTextView.append("\nLogged in. Getting user details.");
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user,
                                        Response response) {
                    if (user != null) {
                        System.out.println("onSessionStateChange: LoadNotes: session is open. username:"+user.getName());

                        loadNotes(getActivity(), getLoggedInUsername());
                    }
                }
            });
            Request.executeBatchAsync(request);
        } else {
            mSplashScreenTextView.append("\nUser not already logged in.");
            System.out.println("onSessionStateChange: LoadNotes: session was closed.");
            loadNotes(getActivity(), getLoggedInUsername());
        }
    }

    private void launchMainActivity() {
        mSplashScreenTextView.append("\nLaunching map view.");

        final Fragment currentFragment = this;
        final Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                LaunchMainActivity(getActivity(), currentFragment);
                SplashScreenFragment.this.getActivity().finish();
            }
        };

        Time now = new Time();
        now.setToNow();
        timerHandler.postDelayed(timerRunnable,
                    startupTime.toMillis(false)
                    + splashScreenShowTimeMillis
                    - now.toMillis(false));
    }

    public static void LaunchMainActivity(Activity currentActivity, Fragment currentFragment) {
        Intent myIntent = new Intent(currentActivity, MainActivity.class);
        if (currentFragment != null) {
            currentFragment.startActivityForResult(myIntent, Constants.ACTIVITY_NOTE_VIEW);
        } else {
            currentActivity.startActivityForResult(myIntent, Constants.ACTIVITY_NOTE_VIEW);
        }
    }
}