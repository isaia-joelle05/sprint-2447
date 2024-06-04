package utils;

import java.util.HashMap;

public class ModelView {
    String url;
    HashMap<String, Object> data;

    public ModelView(String url) {
        this.url = url;
        this.data = new HashMap<>();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void addObject(String key, Object value) {
        this.data.put(key, value);
    }
}
