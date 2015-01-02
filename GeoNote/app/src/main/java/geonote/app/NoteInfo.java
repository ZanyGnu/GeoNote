package geonote.app;

import android.location.Address;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Ajay on 1/1/2015.
 */
public class NoteInfo {
    private LatLng LatLng;
    private Address Address;
    private ArrayList<String> Notes;

    public static NoteInfo createNoteInfo() {
        return new NoteInfo();
    }

    public LatLng getLatLng() {
        return LatLng;
    }

    public void setLatLng(LatLng latLng) {
        LatLng = latLng;
    }

    public Address getAddress() {
        return Address;
    }

    public void setAddress(Address address) {
        Address = address;
    }

    public ArrayList<String> getNotes() {
        return Notes;
    }

    public void setNotes(ArrayList<String> notes) {
        Notes = notes;
    }

    public NoteInfo LatLng(LatLng latLng)
    {
        this.LatLng = latLng ;
        return this;
    }

    public NoteInfo Address(Address address)
    {
        this.Address = address;
        return this;
    }

    public NoteInfo Notes(ArrayList<String> notes)
    {
        this.Notes = notes;
        return this;
    }

    public NoteInfo AddNote(String note)
    {
        if (this.Notes == null) {
            this.Notes = new ArrayList<String>();
        }

        this.Notes.add(note);

        return this;
    }

    public String toString()
    {
        String notesText = "";

        for(String noteText: this.getNotes())
        {
            notesText += noteText + "\n";
        }

        return notesText;
    }
}
