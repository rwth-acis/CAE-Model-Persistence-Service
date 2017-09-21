package i5.las2peer.services.modelPersistenceService.model.wireframe;
import java.io.Serializable;
import java.util.ArrayList;

public class UIControl implements Serializable {
    private String id;
    private String type;
    private UIControl parent;
    private Geometry geometry;
    private String label;
    private ArrayList<UIControlAttribute> attributes;



    UIControl(String id, String type, Geometry geometry, ArrayList<UIControlAttribute> attributes){
        this.id = id;
        this.type = type;
        this.geometry = geometry;
        this.attributes = attributes;
    }

    public String getId(){
        return id;
    }

    public String getType(){
        return type;
    }

    Geometry getGeometry(){
        return geometry;
    }

    void setParent(UIControl parent){
        this.parent = parent;
    }

    public void setLabel(String label){
        this.label = label;
    }

    public String getLabel(){
        return label;
    }

    public void add(UIControlAttribute attr){
        attributes.add(attr);
    }

    public void setAttributes(ArrayList<UIControlAttribute> attrs){
        attributes = attrs;
    }

    public ArrayList<UIControlAttribute> getAttributes(){
        return attributes;
    }
}
