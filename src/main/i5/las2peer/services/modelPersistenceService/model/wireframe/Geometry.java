package i5.las2peer.services.modelPersistenceService.model.wireframe;

import org.w3c.dom.Node;

import java.io.Serializable;

public class Geometry implements Serializable{
    private String x;
    private String y;
    private String height;
    private String width;

    Geometry(Node x, Node y, Node width, Node height){
        this.x = x.getNodeValue();
        this.y = y.getNodeValue();
        this.width = width.getNodeValue();
        this.height = height.getNodeValue();
    }

    String getX(){
        return x;
    }

    String getY(){
        return y;
    }

    public String getHeight(){
        return height;
    }

    public String getWidth(){
        return width;
    }
}
