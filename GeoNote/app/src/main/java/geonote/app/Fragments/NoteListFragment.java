package geonote.app.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import geonote.app.Constants;
import geonote.app.NoteInfo;
import geonote.app.NotesRepository;
import geonote.app.R;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class NoteListFragment
        extends     Fragment
        implements  AbsListView.OnItemClickListener {

    private OnFragmentInteractionListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private NoteListArrayAdapter mAdapter;
    private NotesRepository mNotesRepository;

    // TODO: Rename and change types of parameters
    public static NoteListFragment newInstance() {
        NoteListFragment fragment = new NoteListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NoteListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
        }

        setUpNotesRepository();

        mAdapter = new NoteListArrayAdapter(getActivity(), mNotesRepository.getNotes());
    }

    protected void setUpNotesRepository() {

        SharedPreferences settings = this.getActivity().getSharedPreferences(Constants.PREFS_NOTES, 0);
        String settingJson = settings.getString(Constants.PREFS_NOTES_VALUES_JSON, "");

        mNotesRepository = new NotesRepository(null);
        mNotesRepository.deserializeFromJson(settingJson);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater){
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

        SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextChange(String newText)
            {
                // this is your adapter that will be filtered
                mAdapter.getFilter().filter(newText);
                System.out.println("on text chnge text: "+newText);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query)
            {
                // this is your adapter that will be filtered
                mAdapter.getFilter().filter(query);
                System.out.println("on query submit: "+query);
                return true;
            }
        };
        searchView.setOnQueryTextListener(textChangeListener);

        super.onCreateOptionsMenu(menu,this.getActivity().getMenuInflater());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onFragmentInteraction(mNotesRepository.getNotes().get(position).getAddressDetails());
        }

        MapViewFragment.LaunchNoteViewActivity(mNotesRepository.getNotes().get(position), getActivity(), this);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the request went well (OK) and the request was ACTIVITY_NOTE_VIEW
        if (requestCode == Constants.ACTIVITY_NOTE_VIEW) {
            NoteInfo noteInfo = null;

            switch (resultCode) {
                case Constants.RESULT_SAVE_NOTE:
                    noteInfo = data.getParcelableExtra("result");

                    // replace existing note with new note.
                    this.mNotesRepository.Notes.put(noteInfo.getLatLng(), noteInfo);

                    break;

                case Constants.RESULT_DELETE_NOTE:
                    noteInfo = data.getParcelableExtra("result");
                    this.mNotesRepository.Notes.remove(noteInfo.getLatLng());
            }

            // lets remember the changes
            MapViewFragment.commitNotes(this.getActivity(), this.mNotesRepository);
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

    public class NoteListArrayAdapter extends ArrayAdapter<NoteInfo> implements Filterable {

        private final Activity context;
        private ArrayList<NoteInfo> mNotes;
        private ArrayList<NoteInfo> mFilteredNotes;

        public NoteListArrayAdapter(Activity context, ArrayList<NoteInfo> notes) {
            super(context, R.layout.notes_list_item_view, notes);
            this.context = context;
            this.mNotes = notes;
            this.mFilteredNotes = notes;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            NoteInfo noteInfo = mFilteredNotes.get(position);

            LayoutInflater inflater = context.getLayoutInflater();

            View rowView= inflater.inflate(R.layout.notes_list_item_view, null, true);

            String getMapURL = "http://maps.googleapis.com/maps/api/staticmap?zoom=18&size=560x560&markers=size:mid|color:red|"
                    + noteInfo.getLatLng().latitude
                    + ","
                    + noteInfo.getLatLng().longitude
                    + "&sensor=false";

            new DownloadImageTask((ImageView) rowView.findViewById(R.id.mapImageHolder))
                    .execute(getMapURL);

            TextView txtPlaceDetails = (TextView) rowView.findViewById(R.id.txt_list_view_place_details);
            TextView txtAddress = (TextView) rowView.findViewById(R.id.txt_list_view_address);
            TextView txtNotes = (TextView) rowView.findViewById(R.id.txt_list_view_notes);

            txtPlaceDetails.setText(noteInfo.getAddressDetails());
            txtAddress.setText(noteInfo.getAddress());
            txtNotes.setText(noteInfo.toString());

            return rowView;
        }

        public int getCount() {
            return mFilteredNotes.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    List<NoteInfo> filteredResult = getFilteredResults(charSequence);

                    FilterResults results = new FilterResults();
                    results.values = filteredResult;
                    results.count = filteredResult.size();

                    return results;
                }

                private List<NoteInfo> getFilteredResults(CharSequence charSequence) {
                    if(charSequence.length() == 0 ) {
                        return mNotes;
                    }

                    ArrayList<NoteInfo> filteredNotes = new ArrayList<NoteInfo>();
                    for(NoteInfo note: mNotes)
                    {
                        if (note.toString().contains(charSequence))
                        {
                            filteredNotes.add(note);
                        }
                    }

                    return filteredNotes;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    mFilteredNotes = (ArrayList<NoteInfo>) filterResults.values;

                    notifyDataSetChanged();
                }
            };
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

}
