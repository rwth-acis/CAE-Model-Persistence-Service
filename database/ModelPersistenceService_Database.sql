--
-- Database:  commedit 
--
-- --------------------------------------------------------

--
-- Table structure for table model.
--
CREATE TABLE commedit.model (
  modelId VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  TYPE ENUM('MICROSERVICE', 'FRONTEND_COMPONENT', 'APPLICATION'),
  CONSTRAINT moldelPK PRIMARY KEY (modelId)
);

--
-- Table structure for table node.
--
CREATE TABLE commedit.node (
  nodeId VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  type VARCHAR(255) NOT NULL,
  pleft INT NOT NULL,
  pTop INT NOT NULL,
  pWidth INT NOT NULL,
  pHeight INT NOT NULL,
  pZIndex INT NOT NULL, 
  CONSTRAINT nodePK PRIMARY KEY (nodeId)
);

--
-- Table structure for table edge.
--
CREATE TABLE commedit.edge (
  edgeId VARCHAR(255) NOT NULL,
  sourceNode VARCHAR(255) NOT NULL,
  targetNode VARCHAR(255) NOT NULL,
  labelValue VARCHAR(255) NOT NULL,
  type VARCHAR(255) NOT NULL,
  CONSTRAINT edgePK PRIMARY KEY (edgeId)
);

--
-- Table structure for table attributes.
--
CREATE TABLE commedit.attribute (
  attributeId VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  value TEXT NOT NULL,
  CONSTRAINT attributePK PRIMARY KEY (attributeId)
);

--
-- Table structure for table edgeToModel.
--
CREATE TABLE commedit.edgeToModel (
  edgeId VARCHAR(255) NOT NULL,
  modelId VARCHAR(255) NOT NULL,
  CONSTRAINT edgeToModelPK PRIMARY KEY (edgeId),
  CONSTRAINT edgeFK FOREIGN KEY (edgeId) REFERENCES commedit.Edge(edgeId),
  CONSTRAINT edgeModelFK FOREIGN KEY (modelId) REFERENCES commedit.Model(modelId)
);

--
-- Table structure for table nodeToModel.
--
CREATE TABLE commedit.nodeToModel (
  nodeId VARCHAR(255) NOT NULL,
  modelId VARCHAR(255) NOT NULL,
  CONSTRAINT nodeToModelPK PRIMARY KEY (nodeId),
  CONSTRAINT nodeFK FOREIGN KEY (nodeId) REFERENCES commedit.Node(nodeId),
  CONSTRAINT nodeModelFK FOREIGN KEY (modelId) REFERENCES commedit.Model(modelId)
);

--
-- Table structure for table attributeToNode.
--
CREATE TABLE commedit.attributeToNode (
  attributeId VARCHAR(255) NOT NULL,
  nodeId VARCHAR(255) NOT NULL,
  CONSTRAINT attributeToNodePK PRIMARY KEY (attributeId),
  CONSTRAINT attributeToNodeFK FOREIGN KEY (attributeId) REFERENCES commedit.Attribute(attributeId),
  CONSTRAINT nodeFK FOREIGN KEY (nodeId) REFERENCES commedit.Node(nodeId)
);

--
-- Table structure for table attributeToEdge.
--
CREATE TABLE commedit.attributeToEdge (
  attributeId VARCHAR(255) NOT NULL,
  edgeId VARCHAR(255) NOT NULL,
  CONSTRAINT attributeToEdgePK PRIMARY KEY (attributeId),
  CONSTRAINT attributetoEdgeFK FOREIGN KEY (attributeId) REFERENCES commedit.Attribute(attributeId),
  CONSTRAINT edgeFK FOREIGN KEY (edgeId) REFERENCES commedit.Edge(edgeId)
);

--
-- Table structure for table attributeToModel.
--
CREATE TABLE commedit.attributeToModel (
  attributeId VARCHAR(255) NOT NULL,
  modelId VARCHAR(255) NOT NULL,
  CONSTRAINT attributeToModelPK PRIMARY KEY (attributeId),
  CONSTRAINT attributeToModelFK FOREIGN KEY (attributeId) REFERENCES commedit.Attribute(attributeId),
  CONSTRAINT modelFK FOREIGN KEY (modelId) REFERENCES commedit.Model(modelId)
);