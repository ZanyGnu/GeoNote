package geonote.app;

public class Constants {

    /** Standard activity result: operation succeeded. */
    public static final int RESULT_OK = -1;

    /** Standard activity result: operation canceled. */
    public static final int RESULT_CANCELED = 0;

    /** Start of user-defined activity results. */
    public static final int RESULT_FIRST_USER = 1;

    /** activity result: operation request to delete. */
    public static final int RESULT_SAVE_NOTE = 2;

    /** activity result: operation request to delete. */
    public static final int RESULT_DELETE_NOTE = 3;

    protected static final int ACTIVITY_NOTE_VIEW = 1;
    protected static final int ACTIVITY_LOGIN = 2;
    protected static final int ACTIVITY_NOTES_LIST = 3;

    public static final String PREFS_NOTES = "GeoNote.Preferences.V1";
    protected static final String PREFS_NOTES_VALUES_JSON = "GeoNote.Preferences.V1.Notes";
    protected static final String APP_ID = "e3ec817cadded7a87ea28a89852d8011";
    protected static final int GEO_FENCE_RADIUS = 100;
    protected static final int CURRENT_NOTIFICATION_ID =0;

}
