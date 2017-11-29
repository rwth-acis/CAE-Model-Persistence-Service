CREATE TABLE commedit.Wireframe (
  wireframeId INT NOT NULL AUTO_INCREMENT,
  wireframeXML LONGTEXT NOT NULL,
  CONSTRAINT wireframePK PRIMARY KEY (wireframeId)
);

CREATE TABLE commedit.ModelToWireframe (
  id INT NOT NULL AUTO_INCREMENT,
  modelId INT NOT NULL,
  wireframeId INT NOT NULL,
  CONSTRAINT ModelToWireframePK PRIMARY KEY (id),
  CONSTRAINT ModelToWireframeModelFK FOREIGN KEY (modelId) REFERENCES commedit.Model(modelId) ON DELETE CASCADE,
  CONSTRAINT ModelToWireframeModelWireframeFK FOREIGN KEY (wireframeId) REFERENCES commedit.Wireframe(wireframeId) ON DELETE CASCADE
);
