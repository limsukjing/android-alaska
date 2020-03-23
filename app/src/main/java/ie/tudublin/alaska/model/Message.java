package ie.tudublin.alaska.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.ibm.watson.assistant.v2.model.DialogNodeOutputOptionsElement;
import com.ibm.watson.assistant.v2.model.RuntimeResponseGeneric;

public class Message implements Serializable {
    private String id, message, url, title, description;
    private List<DialogNodeOutputOptionsElement> option = new ArrayList<>();
    public Type type;

    public Message() {
        this.type = Type.TEXT;
    }

    public Message(RuntimeResponseGeneric r, String type) {
        if (type.equals("option")) {
            this.message = "";
            this.title = r.title();
            this.description = r.description();
            this.option = r.options();
            this.id = "3";
            this.type = Type.OPTION;
        } else if (type.equals("image")){
            this.message = "";
            this.title = r.title();
            this.description = r.description();
            this.url = r.source();
            this.id = "4";
            this.type = Type.IMAGE;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<DialogNodeOutputOptionsElement> getOption() {
        return option;
    }

    public void setOption(List<DialogNodeOutputOptionsElement> option) {
        this.option = option;
    }

    public enum Type {
        TEXT,
        OPTION,
        IMAGE
    }
}
