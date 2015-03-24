package geonote.app.Tasks;

import android.os.AsyncTask;

import java.util.List;

import geonote.app.Droplet.DropletServer;
import geonote.app.Droplet.Model.Droplet;

public class GetDropletTask extends AsyncTask<GetDropletTask.GetDropletTaskParam, String, Droplet> {

    static public class GetDropletTaskParam
    {
        String UserName;
        String DropletName;

        public GetDropletTaskParam(String userName, String dropletName)
        {
            this.DropletName = dropletName;
            this.UserName = userName;
        }
    }

    DropletServer dropletServer = new DropletServer();

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    protected Droplet doInBackground(GetDropletTaskParam... args) {
        Droplet droplet = null;

        try {

            droplet = dropletServer.getDroplet(args[0].UserName, args[0].DropletName);

        } catch (Exception e) {
            System.out.println("Unhandled exception trying to get droplets: " + e.toString());
        }

        return droplet;
    }

    protected void onPostExecute(Droplet result) {

    }
}