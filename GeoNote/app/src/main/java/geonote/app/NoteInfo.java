package geonote.app;

import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Ajay on 1/1/2015.
 */
public class NoteInfo implements Parcelable {
    private LatLng LatLng;
    private Address Address;
    private String AddressDetails;
    private Boolean EnableRaisingEvents;
    private ArrayList<String> Notes;

    public LatLng getLatLng() {
        return LatLng;
    }

    public void setLatLng(LatLng latLng) {
        LatLng = latLng;
    }

    public Address getAddress() {
        return Address;
    }

    public String getAddressDetails() { return this.AddressDetails; }

    public String getAddressString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append(this.Address.getAddressLine(0)).append("\n");
        sb.append(this.Address.getLocality()).append("\n");
        sb.append(this.Address.getPostalCode()).append("\n");
        sb.append(this.Address.getCountryName());

        return sb.toString();
    }

    public void setAddress(Address address) {
        Address = address;
    }

    public void setAddressDetails(String addressDetails) {
        this.AddressDetails = addressDetails;
    }

    public Boolean getEnableRaisingEvents() {
        if (this.EnableRaisingEvents == null)
        {
            return false;
        }

        return this.EnableRaisingEvents;
    }

    public void setEnableRaisingEvents(Boolean enableRaisingEvents) {
        this.EnableRaisingEvents = enableRaisingEvents;
    }

    public ArrayList<String> getNotes() {
        return Notes;
    }

    public void setNotes(ArrayList<String> notes) {
        Notes = notes;
    }

    public NoteInfo() {
        this.Notes = new ArrayList<String>();
    }

    public NoteInfo(Parcel parcel)
    {
        this.Notes = new ArrayList<String>();

        this.LatLng = parcel.readParcelable(com.google.android.gms.maps.model.LatLng.class.getClassLoader());
        this.Address = parcel.readParcelable(android.location.Address.class.getClassLoader());
        parcel.readStringList(this.Notes);
        this.AddressDetails  = parcel.readString();
        this.EnableRaisingEvents = parcel.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.LatLng, flags);
        dest.writeParcelable(this.Address, flags);
        dest.writeStringList(this.Notes);
        dest.writeString(this.AddressDetails);
        dest.writeByte((byte) (this.getEnableRaisingEvents() ? 1 : 0));
    }

    public static final Parcelable.Creator<NoteInfo> CREATOR = new Parcelable.Creator<NoteInfo>() {
        public NoteInfo createFromParcel(Parcel in) {
            return new NoteInfo(in);
        }

        public NoteInfo[] newArray(int size) {
            return new NoteInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
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

    public NoteInfo AddressDetails(String addressDetails)
    {
        this.AddressDetails = addressDetails;
        return this;
    }

    public NoteInfo EnableRaisingEvents(Boolean enableRaisingEvents)
    {
        this.EnableRaisingEvents = enableRaisingEvents;
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
