package i5.las2peer.services.modelPersistenceService.model.wireframe;

import org.w3c.dom.Node;

import java.io.Serializable;

public class Geometry implements Serializable{
    private String x;
    private String y;
    private String height;
    private String width;

    Geometry(Node x, Node y, Node width, Node height){
        this.x = x != null  ? x.getNodeValue() : "0";
        this.y = y != null ?  y.getNodeValue() : "0";
        this.width  = width != null ? width.getNodeValue() : "200";
        this.height = height != null ? height.getNodeValue() : "200";
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
