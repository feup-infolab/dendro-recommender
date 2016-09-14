-- DROP INDEX uri_text ON interactions;
CREATE INDEX uri_text ON interactions(uri(255));

-- DROP INDEX performedBy_text ON interactions;
CREATE INDEX performedBy_text ON interactions(performedBy(255));

-- DROP INDEX interaction_type_text ON interactions;
CREATE INDEX interaction_type_text ON interactions(interaction_type(255));

-- DROP INDEX executedOver_text ON interactions;
CREATE INDEX executedOver_text ON interactions(executedOver(255));

-- DROP INDEX originallyRecommendedFor_text ON interactions;
CREATE INDEX originallyRecommendedFor_text ON interactions(originallyRecommendedFor(255));

-- DROP INDEX rankingPosition_text ON interactions;
CREATE INDEX rankingPosition_text ON interactions(rankingPosition(255));