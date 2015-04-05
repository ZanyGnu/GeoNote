package geonote.app.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;

public abstract class BaseFacebookHandlerFragment extends Fragment {

    private static final String TAG = "FacebookFragment";
    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {
            onSessionStateChangeP(session, state, exception);
        }
    };

    protected GraphUser LoggedInUser;
    protected String getLoggedInUsername() {
        return LoggedInUser == null ? null : this.LoggedInUser.getId();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed()) ) {
            onSessionStateChangeP(session, session.getState(), null);
        }

        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
        uiHelper.onSaveInstanceState(outState);
    }

    private void onSessionStateChangeP(Session session, SessionState state, Exception exception)
    {
        if (session != null && session.isOpened()) {
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user,
                                        Response response) {
                    if (user != null) {
                        LoggedInUser = user;
                    }
                }
            });
            Request.executeBatchAsync(request);
        } else if (session.isClosed()) {
            LoggedInUser = null;
        }

        onSessionStateChange(session, state, exception);
    }

    protected void onSessionStateChange(Session session, SessionState state, Exception exception)
    {

    }
}
