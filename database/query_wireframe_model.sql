select Wireframe.wireframeId, `wireframeXML`, Model.modelId, modelName
from `Wireframe`, `Model`, `ModelToModelAttributes`, `ModelAttributes`, `ModelToWireframe`
where `Wireframe`.`wireframeId` = `ModelToWireframe`.`wireframeId`
and `ModelToWireframe`.modelId = Model.modelId
and Model.modelId = ModelToModelAttributes.modelId
and ModelToModelAttributes.modelAttributesName = ModelAttributes.modelName;