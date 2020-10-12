CREATE TABLE IF NOT EXISTS commedit.MetadataDoc (
  `versionedModelId` int(11) NOT NULL,
  `docType` varchar(45) NOT NULL,
  `docString` text,
  `docInput` text,
  `timeEdited` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `timeDeployed` datetime DEFAULT NULL,
  `urlDeployed` varchar(255) DEFAULT NULL,
  `timeCreated` datetime DEFAULT NULL,
  `version` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`versionedModelId`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;