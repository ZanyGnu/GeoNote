package geonote.app.Note;

import android.location.Address;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

public class NoteInfo implements Parcelable {
    private static int NOTE_INFO_CURRENT_VERSION = 1;
    private LatLng LatLng;
    private String AddressText = "";
    private String AddressDetails = "";
    private Boolean EnableRaisingEvents = true;
    private ArrayList<String> Notes;

    public LatLng getLatLng() {
        return LatLng;
    }

    public void setLatLng(LatLng latLng) {
        LatLng = latLng;
    }

    public String getAddress() {
        return AddressText;
    }

    public String getAddressDetails() { return this.AddressDetails; }

    public static String getAddressString(Address address)
    {
        StringBuffer sb = new StringBuffer();

        sb.append(address.getAddressLine(0)).append(", ");
        sb.append(address.getLocality()).append(", ");
        sb.append(address.getPostalCode()).append(", ");
        sb.append(address.getCountryName());

        return sb.toString();
    }

    public void setAddress(String address) {
        AddressText = address;
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
        this.AddressText = parcel.readString();
        parcel.readStringList(this.Notes);
        this.AddressDetails  = parcel.readString();
        this.EnableRaisingEvents = parcel.readByte() != 0;

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        // first write out the version of the data.
        //dest.writeInt(NOTE_INFO_CURRENT_VERSION);
        dest.writeParcelable(this.LatLng, flags);
        dest.writeString(this.AddressText);
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

    public NoteInfo Address(String address)
    {
        this.AddressText = address;
        return this;
    }

    public NoteInfo Address(Address address)
    {
        this.AddressText = this.getAddressString(address);
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

    @Override
    public int hashCode() {
        return this.getLatLng().hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return true;
        }

        if (!(o instanceof NoteInfo)) {
            return false;
        }

        NoteInfo other = (NoteInfo)o;
        if (this.LatLng != other.LatLng
            || !this.AddressText.equals(other.AddressText)
            || !this.AddressDetails.equals(other.AddressDetails)
            || this.EnableRaisingEvents != other.EnableRaisingEvents) {
            return  false;
        }

        if (!checkIfEqual(this.Notes, other.Notes))
        {
            return false;
        }

        return true;
    }

    private static boolean checkIfEqual(ArrayList<String> thisNotes, ArrayList<String> otherNotes)
    {
        if (thisNotes == otherNotes) {
            return true;
        }

        if (thisNotes == null && otherNotes != null) {
            return false;
        }

        if (thisNotes != null) {
            if (otherNotes == null){
                return false;
            }

            if (otherNotes.size() != thisNotes.size()){
                return false;
            }

            for(int i = 0;i<thisNotes.size();i++){
                if (!thisNotes.get(i).equals(otherNotes.get(i))){
                    return false;
                }
            }
        }

        return true;
    }

    public float getDistanceFrom(Location location)
    {
        float results[] = new float[1];

        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                this.getLatLng().latitude,
                this.getLatLng().longitude,
                results
        );

        return results[0];
    }
}
