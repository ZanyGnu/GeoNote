package geonote.app.Activity;

import android.app.Activity;
import android.content.Intent;
import android.location.Geocoder;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.Window;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import java.util.Locale;

import geonote.app.Constants;
import geonote.app.Fragments.BaseFacebookHandlerFragment;
import geonote.app.NoteInfo;
import geonote.app.NotesManager;
import geonote.app.NotesRepository;
import geonote.app.R;

public class SplashScreenActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_screen);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SplashScreenFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SplashScreenFragment extends BaseFacebookHandlerFragment {

        View mCurrentView = null;
        NotesManager mNotesManager = null;
        NotesRepository mNotesRepository = null;

        public SplashScreenFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mCurrentView = inflater.inflate(R.layout.fragment_splash_screen, container, false);

            mNotesManager = new NotesManager();
            mNotesRepository = new NotesRepository(new Geocoder(getActivity().getBaseContext(), Locale.getDefault()));

            mNotesManager.mOnNotesLoadedListener  = new NotesManager.OnNotesLoadedListener() {
                @Override
                public void onNotesLoaded() {
                    launchMainActivity();
                }
            };

            return mCurrentView;
        }

        public void loadNotes(Activity activity, final String userName) {

            // TODO: Load notes only if not already loaded

            mNotesManager.loadNotes(activity, mNotesRepository, userName);
        }

        protected void onSessionStateChange(Session session, SessionState state, Exception exception){
            if (session != null && session.isOpened()) {
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
            } else if (session.isClosed()) {
                System.out.println("onSessionStateChange: LoadNotes: session was closed.");
                loadNotes(getActivity(), getLoggedInUsername());
            }
        }

        private void launchMainActivity() {
            LaunchMainActivity(this.getActivity(), this);
            SplashScreenFragment.this.getActivity().finish();
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
}
