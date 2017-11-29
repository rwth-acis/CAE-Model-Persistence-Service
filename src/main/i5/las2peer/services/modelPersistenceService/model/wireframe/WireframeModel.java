package i5.las2peer.services.modelPersistenceService.model.wireframe;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.node.SimpleNode;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.HashMap;

public class WireframeModel implements Serializable {
    private HashMap<String, UIControl> uiControls;
    private String width;
    private String height;
    private String id;

    public WireframeModel(String xml){
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document doc = dBuilder.parse(is);

            //Wireframe meta
            Node meta = doc.getElementsByTagName("WireframeMeta").item(0);
            NamedNodeMap metaAttrs = meta.getAttributes();
            id = metaAttrs.getNamedItem("id") != null ? metaAttrs.getNamedItem("id").getNodeValue() : "";
            width = metaAttrs.getNamedItem("width") != null ? metaAttrs.getNamedItem("width").getNodeValue() : "";
            height = metaAttrs.getNamedItem("height") != null ? metaAttrs.getNamedItem("height").getNodeValue() : "";

            //ui controls
            uiControls = new HashMap<>();
            NodeList uiObjs = doc.getElementsByTagName("uiObj");
            for(int i=0; i<uiObjs.getLength(); i++) {
                Node uiObj = uiObjs.item(i);
                ArrayList<UIControlAttribute> attrsToAdd = new ArrayList<>();

                NamedNodeMap attrs = uiObj.getAttributes();
                for (int j = 0; j < uiObj.getAttributes().getLength(); j++) {
                    Node attr = attrs.item(j);
                    String name = attr.getNodeName();
                    UIControlAttribute uiAttr;
                    if (name.contains("_")) {
                        uiAttr = new UIControlAttribute(attr.getNodeName(), attr.getNodeValue());
                        attrsToAdd.add(uiAttr);
                    }
                }

                Geometry geometry = null;
                //UIControl uiParent = null;
                NodeList children = uiObj.getChildNodes();
                for (int k = 0; k < children.getLength(); k++) {
                    Node child = children.item(k);
                    String name = child.getNodeName();
                    if (!name.equals("tagRoot")) {
                        //Create reference to the parent, actually not necessary because of hasChild-edge
                        /*Node parent = child.getAttributes().getNamedItem("parent");
                        if (parent != null && uiControls.containsKey(parent.getNodeValue())) {
                            uiParent = uiControls.get(parent.getNodeValue());
                        }*/

                        //Create the geometry object
                        Node geo = child.getFirstChild();
                        NamedNodeMap geoAttrs = geo.getAttributes();
                        geometry = new Geometry(geoAttrs.getNamedItem("x"), geoAttrs.getNamedItem("y"), geoAttrs.getNamedItem("width"), geoAttrs.getNamedItem("height"));
                    }
                }
                UIControl uiControl;
                String id = attrs.getNamedItem("id").getNodeValue();
                if (geometry != null) {
                    uiControl = new UIControl(id, attrs.getNamedItem("uiType").getNodeValue(), geometry, attrsToAdd);
                    //if (uiParent != null)
                    //    uiControl.setParent(uiParent);
                    Node label = attrs.getNamedItem("label");
                    if(label != null)
                        uiControl.setLabel(label.getNodeValue());
                    uiControls.put(id, uiControl);
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }



    public String getId(){
        return id;
    }

    public String getWidth(){
        return width;
    }

    public String getHeight() {
        return height;
    }

    public SimpleModel extendSimpleModel(SimpleModel simpleModel){
        ArrayList<SimpleNode> nodes = simpleModel.getNodes();
        for(SimpleNode node : nodes){
            if(uiControls.containsKey(node.getId())) {
                UIControl uiControl = uiControls.get(node.getId());
                ArrayList<SimpleEntityAttribute> attrs = node.getAttributes();
                ArrayList<UIControlAttribute> uiAttrs = uiControl.getAttributes();

                SimpleEntityAttribute simpleAttr;
                for(UIControlAttribute attr : uiAttrs){
                    simpleAttr = attr.toSimpleEntityAttribute("uiAttr_" +  attr.getName());
                    attrs.add(simpleAttr);
                }

                //geometry
                simpleAttr = new SimpleEntityAttribute("uiGeo_X",  "x", String.valueOf(uiControl.getGeometry().getX()));
                attrs.add(simpleAttr);
                simpleAttr = new SimpleEntityAttribute("uiGeo_Y",  "y", String.valueOf(uiControl.getGeometry().getY()));
                attrs.add(simpleAttr);
                simpleAttr = new SimpleEntityAttribute("uiGeo_Height",  "height", String.valueOf(uiControl.getGeometry().getHeight()));
                attrs.add(simpleAttr);
                simpleAttr = new SimpleEntityAttribute("uiGeo_Width",  "width", String.valueOf(uiControl.getGeometry().getWidth()));
                attrs.add(simpleAttr);

                //label
                simpleAttr = new SimpleEntityAttribute("uiLabel", "label", String.valueOf(uiControl.getLabel()));
                attrs.add(simpleAttr);
            }
        }
        return simpleModel;
    }
}