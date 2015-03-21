package geonote.app.Tasks;

import android.os.AsyncTask;

import java.util.List;

import geonote.app.Droplet.DropletServer;
import geonote.app.Droplet.Model.Droplet;

public class SaveDropletTask extends AsyncTask<SaveDropletTask.SaveDropletTaskParam, String, String> {

    static public class SaveDropletTaskParam
    {
        List<Droplet> Droplets;
        String UserName;
        public SaveDropletTaskParam(String userName, List<Droplet> droplets)
        {
            this.Droplets = droplets;
            this.UserName = userName;
        }
    }

    DropletServer dropletServer = new DropletServer();

    @Override
    protected String doInBackground(SaveDropletTaskParam... params) {
        try {
            dropletServer.putDroplets(params[0].UserName, params[0].Droplets);
        } catch (Exception e) {
            System.out.println("Unhandled exception trying to get droplets: " + e.toString());
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    /**
     * After completing background task show the data in UI
     * Use runOnUiThread(new Runnable()) to update UI from background
     * **/
    protected void onPostExecute(String param) {

    }
}