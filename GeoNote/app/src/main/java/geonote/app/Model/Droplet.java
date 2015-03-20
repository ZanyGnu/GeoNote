package geonote.app.Model;

import com.google.api.client.util.Key;

import java.io.Serializable;

public class Droplet  implements Serializable {
    @Key
    public String Name;

    @Key
    public String Content;

    public Droplet(String name, String content)
    {
        this.Name = name;
        this.Content = content;
    }

}
