package i5.las2peer.services.modelPersistenceService.model.wireframe;

import i5.cae.simpleModel.SimpleEntityAttribute;

import java.io.Serializable;

public class UIControlAttribute implements Serializable{
    private String name;
    private String value;

    UIControlAttribute(String name, String value){
        this.name = name.substring(1);
        this.value = value;
    }

    public String getName(){
        return name;
    }

    public String getValue(){
        return value;
    }

    SimpleEntityAttribute toSimpleEntityAttribute(String id){
        return new SimpleEntityAttribute(id, name, value);
    }
}
