package geonote.app;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

public class NoteViewActivity extends ActionBarActivity {

    NoteInfo noteInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.noteInfo = intent.getParcelableExtra("noteInfoExtra");

        setContentView(R.layout.activity_note_view);

        TextView textView = (TextView) findViewById(R.id.txtNoteViewAddress);
        textView.setText(noteInfo.getAddressString());

        final EditText editText = (EditText) findViewById(R.id.editTextNoteView);
        editText.setText(noteInfo.toString());

        final TextView addressDetailsTextView = (TextView) findViewById(R.id.txtNoteViewPlaceDetails);
        Button saveButton = (Button) findViewById(R.id.buttonNoteActivitySave);

        final CheckBox checkBoxEnableAlerts = (CheckBox) findViewById(R.id.checkboxEnableAlerts);

        saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                noteInfo.getNotes().clear();
                for (String note : editText.getText().toString().split("\n")) {
                    noteInfo.AddNote(note);
                }
                noteInfo.AddressDetails(addressDetailsTextView.getText().toString())
                        .EnableRaisingEvents(checkBoxEnableAlerts.isChecked());

                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", noteInfo);
                setResult(RESULT_OK,returnIntent);

                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.buttonNoteActivityCancel);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_view, menu);
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
}
