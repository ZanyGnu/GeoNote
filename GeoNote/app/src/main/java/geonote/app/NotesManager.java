package geonote.app;

import android.app.Activity;
import android.content.SharedPreferences;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import geonote.app.Droplet.Model.Droplet;
import geonote.app.Tasks.GetDropletTask;
import geonote.app.Tasks.SaveDropletTask;

public class NotesManager {

    public OnNotesLoadedListener mOnNotesLoadedListener;

    public static interface OnNotesLoadedListener {
        void onNotesLoaded();
    }

    public void loadNotes(Activity activity, final NotesRepository notesRepository, final String userName) {

        if (activity == null){
            return;
        }

        SharedPreferences settings = activity.getSharedPreferences(Constants.PREFS_NOTES, 0);

        final String settingJson = settings.getString(Constants.PREFS_NOTES_VALUES_JSON, "");
        final Integer notesVersion = settings.getInt(Constants.PREFS_NOTES_VERSION, 0);

        System.out.println("LoadNotes: looking for logged in user");
        // If the user is logged in, lets also send these notes to the droplet server
        // and associate it with the logged in user.
        if (userName != null && userName != "") {

            System.out.println("LoadNotes: Found logged in user");
            ArrayList<Droplet> droplets = new ArrayList<>();

            new GetDropletTask() {
                @Override
                protected void onPostExecute(Droplet result) {
                    final Integer notesVersionOnServer = Integer.parseInt(result.Content);

                    System.out.println("LoadNotes: notesVersionOnServer " + notesVersionOnServer);
                    System.out.println("LoadNotes: notesVersionLocal " + notesVersion);

                    if (notesVersionOnServer > notesVersion) {
                        System.out.println("LoadNotes: getting notes from the cloud");
                        new GetDropletTask() {
                            @Override
                            protected void onPostExecute(Droplet result) {
                                System.out.println("LoadNotes: got notes from the cloud, loading repository");
                                notesRepository.deserializeFromJson(result.Content, notesVersionOnServer);

                                onRepositoryLoaded();
                            }
                        }.execute(new GetDropletTask.GetDropletTaskParam(userName, "notes"));
                    }
                    else {
                        // the version in the local machine is more advanced. Load that instead
                        // and queue a work item to update the cloud version.
                        notesRepository.deserializeFromJson(settingJson, notesVersion);
                        onRepositoryLoaded();
                        saveNotesToCloud(userName, settingJson, notesVersion);
                    }
                }
            }.execute(new GetDropletTask.GetDropletTaskParam(userName, "notes-version"));
        }
        else {
            // no user logged in, load the version from local disk.
            System.out.println("LoadNotes: Loading notes from local machine.");
            //System.out.println("LoadNotes: called like so " + Arrays.toString(Thread.currentThread().getStackTrace()));
            notesRepository.deserializeFromJson(settingJson, notesVersion);

            onRepositoryLoaded();
        }
    }

    public void commitNotes(Activity activity, NotesRepository notesRepository, String userName) {

        // increment the version of the notes every time we commit
        notesRepository.NotesVersion++;

        SharedPreferences settings = activity.getSharedPreferences(Constants.PREFS_NOTES, 0);

        String notesJson = notesRepository.serializeToJson();
        Integer notesVersion = notesRepository.NotesVersion;
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(Constants.PREFS_NOTES_VALUES_JSON, notesJson);
        editor.putInt(Constants.PREFS_NOTES_VERSION, notesVersion);

        // Commit the edits!
        editor.commit();

        // If the user is logged in, lets also send these notes to the droplet server
        // and associate it with the logged in user.
        if (userName != null && userName != "") {

            saveNotesToCloud(userName, notesJson, notesVersion);
        }
    }

    private void onRepositoryLoaded() {
        if (mOnNotesLoadedListener!=null)
        {
            mOnNotesLoadedListener.onNotesLoaded();
        }
    }

    private void saveNotesToCloud(String userName, String notesJson, Integer notesVersion) {
        ArrayList<Droplet> droplets = new ArrayList<>();

        droplets.add(new Droplet("notes", notesJson));
        droplets.add(new Droplet("notes-version", notesVersion.toString()));

        new SaveDropletTask().execute(
                new SaveDropletTask.SaveDropletTaskParam(userName, droplets));
    }
}
