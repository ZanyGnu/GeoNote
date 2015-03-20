package geonote.app.Activity;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import geonote.app.Fragments.BaseFacebookHandlerFragment;
import geonote.app.R;

public class LoginActivityFB extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_fb);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login_activity_fb, menu);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    /**
     * Fragment containing the login button.
     */
    public static class PlaceholderFragment extends BaseFacebookHandlerFragment {

        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_login_fb, container, false);
            LoginButton authButton = (LoginButton) view.findViewById(R.id.authButton);
            authButton.setFragment(this);
            //authButton.setReadPermissions(Arrays.asList("user_likes", "user_status"));

            Button button = (Button) view.findViewById(R.id.buttonGetUserDetails);
            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    populateLoggedInUser();
                }
            });

            return view;
        }

        @Override
        protected void onSessionStateChange(Session session, SessionState state, Exception exception){
            populateLoggedInUser();
        }

        private void populateLoggedInUser() {

            final TextView txtUserDetails = (TextView) this.getActivity().findViewById(R.id.userDetails);

            final Session session = Session.getActiveSession();
            if (session != null && session.isOpened()) {
                // If the session is open, make an API call to get user data
                // and define a new callback to handle the response
                Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        // If the response is successful
                        if (session == Session.getActiveSession()) {
                            if (user != null) {
                                String user_ID = user.getId();//user id
                                String profileName = user.getName();//user's profile name
                                txtUserDetails.setText(user.getName());
                            }
                        }
                    }
                });
                Request.executeBatchAsync(request);
            } else if (session == null || session.isClosed()) {

            }
        }
    }
}
