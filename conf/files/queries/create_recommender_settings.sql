CREATE TABLE `dendro_recommender`.`recommendation_settings` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `error` DOUBLE NULL,
  `normalizationSetting` INT NULL,
  `ratingSetting` INT NULL,
  `evaluationTimestamp` DATETIME NULL,
  `evaluationRuns` INT NULL,
  PRIMARY KEY (`id`));
