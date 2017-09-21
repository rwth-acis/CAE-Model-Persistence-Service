package i5.las2peer.services.modelPersistenceService.model.wireframe;

import org.w3c.dom.Node;

import java.io.Serializable;

public class Geometry implements Serializable{
    private int x;
    private int y;
    private int height;
    private int width;

    Geometry(Node x, Node y, Node width, Node height){
        this.x = convertNode(x);
        this.y = convertNode(y);
        this.width = convertNode(width);
        this.height = convertNode(height);
    }

    private int convertNode(Node node){
        if(node != null){
            return Integer.valueOf(node.getNodeValue());
        }
        return 0;
    }

    int getX(){
        return x;
    }

    int getY(){
        return y;
    }

    public int getHeight(){
        return height;
    }

    public int getWidth(){
        return width;
    }
}
