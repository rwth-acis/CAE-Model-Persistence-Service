select Wireframe.wireframeId
from `Wireframe`, `Model`, `ModelToModelAttributes`, `ModelAttributes`, `ModelToWireframe`
where `Wireframe`.`wireframeId` = `ModelToWireframe`.`wireframeId`
and `ModelToWireframe`.modelId = Model.modelId
and Model.modelId = ModelToModelAttributes.modelId;