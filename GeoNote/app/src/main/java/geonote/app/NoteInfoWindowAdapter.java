package geonote.app;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Ajay on 1/2/2015.
 */
public class NoteInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private LayoutInflater layoutInflater;
    private NotesRepository notesRepository;

    NoteInfoWindowAdapter(LayoutInflater layoutInflater, NotesRepository notesRepository)
    {
        this.layoutInflater = layoutInflater;
        this.notesRepository = notesRepository;
    }

    @Override
    public View getInfoWindow(Marker marker) {

        View contents = this.layoutInflater.inflate(R.layout.note_info_window, null);
        NoteInfo note = this.notesRepository.Notes.get(marker.getPosition());

        String title = marker.getTitle();

        TextView txtTitle = ((TextView) contents.findViewById(R.id.txtInfoWindowTitle));

        if (title != null) {
            // Spannable string allows us to edit the formatting of the text.
            SpannableString titleText = new SpannableString(title);
            titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
            txtTitle.setText(titleText);
        } else {
            txtTitle.setText("");
        }

        TextView txtType = ((TextView) contents.findViewById(R.id.txtInfoWindowNoteAddress));
        txtType.setText(note.toString());

        return contents;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}