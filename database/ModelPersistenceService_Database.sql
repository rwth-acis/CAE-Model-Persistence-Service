--
-- Database:  commedit 
-- Creates the CAE datatabase structure needed to store SyncMeta's application models.
-- --------------------------------------------------------

--
-- Table structure for table Model.
--
CREATE TABLE IF NOT EXISTS commedit.Model (
  modelId INT NOT NULL AUTO_INCREMENT,
  CONSTRAINT modelPK PRIMARY KEY (modelId)
);

--
-- Table structure for table Node.
--
CREATE TABLE IF NOT EXISTS commedit.Node (
  nodeId INT NOT NULL AUTO_INCREMENT,
  syncMetaId VARCHAR(255) NOT NULL,
  type VARCHAR(255) NOT NULL,
  pLeft INT NOT NULL,
  pTop INT NOT NULL,
  pWidth INT NOT NULL,
  pHeight INT NOT NULL,
  pZIndex INT NOT NULL, 
  CONSTRAINT nodePK PRIMARY KEY (nodeId)
);

--
-- Table structure for table Edge.
-- Note that there exist not FK references to the source and target
-- node because we stay independent of semantics.
--
CREATE TABLE IF NOT EXISTS commedit.Edge (
  edgeId INT NOT NULL AUTO_INCREMENT,
  syncMetaId VARCHAR(255) NOT NULL,
  sourceNode VARCHAR(255) NOT NULL,
  targetNode VARCHAR(255) NOT NULL,
  labelValue VARCHAR(255) NOT NULL,
  type VARCHAR(255) NOT NULL,
  CONSTRAINT edgePK PRIMARY KEY (edgeId)
);

--
-- Table structure for table Attribute.
-- The id is given by the database, while the SyncMetaId
-- is not unique but shared for all attributes of the same type.
--
CREATE TABLE IF NOT EXISTS commedit.Attribute (
  attributeId INT NOT NULL AUTO_INCREMENT,
  syncMetaId VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  value TEXT NOT NULL,
  CONSTRAINT attributePK PRIMARY KEY (attributeId)
);

--
-- Table structure for table EdgeToModel.
--
CREATE TABLE IF NOT EXISTS commedit.EdgeToModel (
  id INT NOT NULL AUTO_INCREMENT,
  edgeId INT NOT NULL,
  modelId INT NOT NULL,
  CONSTRAINT edgeToModelPK PRIMARY KEY (id),
  CONSTRAINT edgeToModelEdgeFK FOREIGN KEY (edgeId) REFERENCES commedit.Edge(edgeId) ON DELETE CASCADE,
  CONSTRAINT edgetoModelModelFK FOREIGN KEY (modelId) REFERENCES commedit.Model(modelId) ON DELETE CASCADE
);

--
-- Table structure for table NodeToModel.
--
CREATE TABLE IF NOT EXISTS commedit.NodeToModel (
  id INT NOT NULL AUTO_INCREMENT,
  nodeId INT NOT NULL,
  modelId INT NOT NULL,
  CONSTRAINT nodeToModelPK PRIMARY KEY (id),
  CONSTRAINT nodeToModelNodeFK FOREIGN KEY (nodeId) REFERENCES commedit.Node(nodeId) ON DELETE CASCADE,
  CONSTRAINT nodeToModelModelFK FOREIGN KEY (modelId) REFERENCES commedit.Model(modelId) ON DELETE CASCADE
);

--
-- Table structure for table AttributeToNode.
--
CREATE TABLE IF NOT EXISTS commedit.AttributeToNode (
  id INT NOT NULL AUTO_INCREMENT,
  attributeId INT NOT NULL,
  nodeId INT NOT NULL,
  CONSTRAINT attributeToNodePK PRIMARY KEY (id),
  CONSTRAINT attributeToNodeAttributeFK FOREIGN KEY (attributeId) REFERENCES commedit.Attribute(attributeId) ON DELETE CASCADE,
  CONSTRAINT attributeToNodeNodeFK FOREIGN KEY (nodeId) REFERENCES commedit.Node(nodeId) ON DELETE CASCADE
);

--
-- Table structure for table AttributeToEdge.
--
CREATE TABLE IF NOT EXISTS commedit.AttributeToEdge (
  id INT NOT NULL AUTO_INCREMENT,
  attributeId INT NOT NULL,
  edgeId INT NOT NULL,
  CONSTRAINT attributeToEdgePK PRIMARY KEY (id),
  CONSTRAINT attributetoEdgeAttributeFK FOREIGN KEY (attributeId) REFERENCES commedit.Attribute(attributeId) ON DELETE CASCADE,
  CONSTRAINT attributeToEdgeEdgeFK FOREIGN KEY (edgeId) REFERENCES commedit.Edge(edgeId) ON DELETE CASCADE
);

--
-- Table structure for table AttributeToModel.
--
CREATE TABLE IF NOT EXISTS commedit.AttributeToModel (
  id INT NOT NULL AUTO_INCREMENT,
  attributeId INT NOT NULL,
  modelId INT NOT NULL,
  CONSTRAINT attributeToModelPK PRIMARY KEY (id),
  CONSTRAINT attributeToModelAttributeFK FOREIGN KEY (attributeId) REFERENCES commedit.Attribute(attributeId) ON DELETE CASCADE,
  CONSTRAINT attributeToModelModelFK FOREIGN KEY (modelId) REFERENCES commedit.Model(modelId) ON DELETE CASCADE
);

--
-- Table structure for table VersionedModel.
--
CREATE TABLE IF NOT EXISTS commedit.VersionedModel (
  id INT NOT NULL AUTO_INCREMENT,
  CONSTRAINT versionedModelPK PRIMARY KEY (id)
);

--
-- Table structure for table Commit.
--
CREATE TABLE IF NOT EXISTS commedit.Commit (
  id INT NOT NULL AUTO_INCREMENT,
  message VARCHAR(255) NOT NULL,
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT commitPK PRIMARY KEY (id)
);

--
-- Table structure for table CommitToVersionedModel.
--
CREATE TABLE IF NOT EXISTS commedit.CommitToVersionedModel (
  id INT NOT NULL AUTO_INCREMENT,
  versionedModelId INT NOT NULL,
  commitId INT NOT NULL,
  CONSTRAINT commitToVersionedModelPK PRIMARY KEY (id),
  CONSTRAINT commitToVersionedModelVersionedModelFK FOREIGN KEY (versionedModelId) REFERENCES commedit.VersionedModel(id) ON DELETE CASCADE,
  CONSTRAINT commitToVersionedModelCommitFK FOREIGN KEY (commitId) REFERENCES commedit.Commit(id) ON DELETE CASCADE
);

--
-- Table structure for table CommitToModel.
--
CREATE TABLE IF NOT EXISTS commedit.CommitToModel (
  id INT NOT NULL AUTO_INCREMENT,
  commitId INT NOT NULL,
  modelId INT NOT NULL,
  CONSTRAINT commitToModelPK PRIMARY KEY (id),
  CONSTRAINT commitToModelCommitFK FOREIGN KEY (commitId) REFERENCES commedit.Commit(id) ON DELETE CASCADE,
  CONSTRAINT commitToModelModelFK FOREIGN KEY (modelId) REFERENCES commedit.Model(modelId) ON DELETE CASCADE
);