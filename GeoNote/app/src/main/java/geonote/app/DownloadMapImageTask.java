package geonote.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;

public class DownloadMapImageTask extends AsyncTask<Double, Void, Bitmap> {
    ImageView bmImage;

    String getMapURLFormat = "http://maps.googleapis.com/maps/api/staticmap"
            + "?zoom=18" +
            "&size=%dx%d" +
            "&markers=size:mid|color:red|"
            + "%s"
            + ","
            + "%s"
            + "&sensor=false";

    public DownloadMapImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    @Override
    protected Bitmap doInBackground(Double... params) {
        String url = String.format(
                getMapURLFormat,
                params[0].intValue(),
                params[1].intValue(),
                params[2].toString(),
                params[3].toString());

        System.out.format("URL for google maps static content: " + url);
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(url).openStream();
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

