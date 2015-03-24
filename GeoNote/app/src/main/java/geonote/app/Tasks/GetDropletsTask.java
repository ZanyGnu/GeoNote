package geonote.app.Tasks;

import android.os.AsyncTask;

import java.util.List;

import geonote.app.Droplet.DropletServer;
import geonote.app.Droplet.Model.Droplet;

public class GetDropletsTask extends AsyncTask<String, String, List<Droplet>> {

    DropletServer dropletServer = new DropletServer();

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    protected List<Droplet> doInBackground(String... args) {
        List<Droplet> droplets = null;

        try {

            droplets = dropletServer.getDroplets(args[0]);

        } catch (Exception e) {
            System.out.println("Unhandled exception trying to get droplets: " + e.toString());
        }

        return droplets;
    }

    protected void onPostExecute(String arg) {

    }
}