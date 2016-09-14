SELECT  t0.executedOver as executedOver,
		COALESCE(t1.frequently_used_overall,0) as frequently_used_overall,
		COALESCE(t2.frequently_used_in_project,0) as frequently_used_in_project,
		COALESCE(t3.frequently_used_by_user,0) as frequently_used_by_user,
		COALESCE(t4.frequently_used_by_user_in_project,0) as frequently_used_by_user_in_project,
		COALESCE(t5.user_favorites,0) as user_favorites,
		COALESCE(t5.user_unfavorites,0) as user_unfavorites,
		COALESCE(t6.project_favorites,0) as project_favorites,
		COALESCE(t6.project_unfavorites,0) as project_unfavorites,
		COALESCE(t7.favorites_overall,0) as favorites_overall,
		COALESCE(t7.unfavorites_overall,0) as unfavorites_overall,
		COALESCE(t8.frequently_selected_project,0) as frequently_selected_project,
		COALESCE(t9.frequently_selected_overall,0) as frequently_selected_overall,
		COALESCE(t10.hiddens_for_user,0) as hiddens_for_user,
		COALESCE(t10.unhiddens_for_user,0) as unhiddens_for_user,
		COALESCE(t11.hiddens_for_project,0) as hiddens_for_project,
		COALESCE(t11.unhiddens_for_project,0) as unhiddens_for_project
FROM
(
	SELECT DISTINCT executedOver
	FROM :interactions_table_name
) t0
LEFT JOIN
(
SELECT executedOver, COUNT(*) as frequently_used_overall
FROM :interactions_table_name
WHERE (interactionType = 'accept_smart_descriptor_in_metadata_editor' 
	OR interactionType = 'accept_favorite_descriptor_in_metadata_editor' 
	OR interactionType = 'fill_in_descriptor_from_manual_list_in_metadata_editor' 
	OR interactionType = 'fill_in_descriptor_from_quick_list_in_metadata_editor'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_user_favorite'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_project_favorite'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_user_and_project_favorite'
	OR interactionType = 'fill_in_descriptor_from_quick_list_in_metadata_editor'
	OR interactionType = 'fill_in_inherited_descriptor')
GROUP BY executedOver
) t1
ON t0.executedOver = t1.executedOver
LEFT JOIN
(
SELECT executedOver, COUNT(*) as frequently_used_in_project
FROM :interactions_table_name
WHERE originallyRecommendedFor LIKE ':projectozinho%'
	AND (interactionType = 'accept_smart_descriptor_in_metadata_editor' 
	OR interactionType = 'accept_favorite_descriptor_in_metadata_editor' 
	OR interactionType = 'fill_in_descriptor_from_manual_list_in_metadata_editor' 
	OR interactionType = 'fill_in_descriptor_from_quick_list_in_metadata_editor'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_user_favorite'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_project_favorite'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_user_and_project_favorite'
	OR interactionType = 'fill_in_descriptor_from_quick_list_in_metadata_editor'
	OR interactionType = 'fill_in_inherited_descriptor')
GROUP BY executedOver
) t2
ON t0.executedOver = t2.executedOver
LEFT JOIN
(
SELECT executedOver, COUNT(*) as  frequently_used_by_user
FROM :interactions_table_name
WHERE performedBy = ':utilizadorzinho'
	AND (interactionType = 'accept_smart_descriptor_in_metadata_editor'
	OR interactionType = 'accept_favorite_descriptor_in_metadata_editor'
	OR interactionType = 'fill_in_descriptor_from_manual_list_in_metadata_editor'
	OR interactionType = 'fill_in_descriptor_from_quick_list_in_metadata_editor'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_user_favorite'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_project_favorite'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_user_and_project_favorite'
	OR interactionType = 'fill_in_descriptor_from_quick_list_in_metadata_editor'
	OR interactionType = 'fill_in_inherited_descriptor')
GROUP BY executedOver
) t3
ON t0.executedOver = t3.executedOver
LEFT JOIN
(
SELECT executedOver, COUNT(*) as  frequently_used_by_user_in_project
FROM :interactions_table_name
WHERE performedBy = ':utilizadorzinho'
	AND originallyRecommendedFor LIKE ':projectozinho%'
	AND (interactionType = 'accept_smart_descriptor_in_metadata_editor'
	OR interactionType = 'accept_favorite_descriptor_in_metadata_editor'
	OR interactionType = 'fill_in_descriptor_from_manual_list_in_metadata_editor'
	OR interactionType = 'fill_in_descriptor_from_quick_list_in_metadata_editor'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_user_favorite'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_project_favorite'
	OR interactionType = 'fill_in_descriptor_from_manual_list_while_it_was_a_user_and_project_favorite'
	OR interactionType = 'fill_in_descriptor_from_quick_list_in_metadata_editor'
	OR interactionType = 'fill_in_inherited_descriptor')
GROUP BY executedOver
) t4
ON t0.executedOver = t4.executedOver
LEFT JOIN
(
SELECT t51.executedOver, t51.favorite_descriptor_from_quick_list_for_user as user_favorites, t52.unfavorite_descriptor_from_quick_list_for_user as user_unfavorites
FROM
	(
		SELECT DISTINCT executedOver
		FROM :interactions_table_name
	) t50
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as  favorite_descriptor_from_quick_list_for_user
	FROM :interactions_table_name
	WHERE performedBy = ':utilizadorzinho'
		AND interactionType = 'favorite_descriptor_from_quick_list_for_user'
	GROUP BY executedOver
	) t51
	ON t50.executedOver = t51.executedOver
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as  unfavorite_descriptor_from_quick_list_for_user
	FROM :interactions_table_name
	WHERE performedBy = ':utilizadorzinho'
		AND interactionType = 'unfavorite_descriptor_from_quick_list_for_user'
	GROUP BY executedOver
	) t52
	ON t50.executedOver = t52.executedOver
) t5
ON t0.executedOver = t5.executedOver
LEFT JOIN
(
SELECT t61.executedOver, t61.favorite_descriptor_from_quick_list_for_project as project_favorites, t62.unfavorite_descriptor_from_quick_list_for_project as project_unfavorites
FROM
	(
		SELECT DISTINCT executedOver
		FROM :interactions_table_name
	) t60
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as  favorite_descriptor_from_quick_list_for_project
	FROM :interactions_table_name
	WHERE originallyRecommendedFor LIKE ':projectozinho%'
		AND interactionType = 'favorite_descriptor_from_quick_list_for_user'
	GROUP BY executedOver
	) t61
	ON t60.executedOver = t61.executedOver
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as  unfavorite_descriptor_from_quick_list_for_project
	FROM :interactions_table_name
	WHERE originallyRecommendedFor LIKE ':projectozinho%'
		AND interactionType = 'unfavorite_descriptor_from_quick_list_for_user'
	GROUP BY executedOver
	) t62
	ON t60.executedOver = t62.executedOver
) t6
ON t0.executedOver = t6.executedOver
LEFT JOIN
(
SELECT t71.executedOver, t71.favorite as favorites_overall, t72.unfavorite as unfavorites_overall
FROM
	(
		SELECT DISTINCT executedOver
		FROM :interactions_table_name
	) t70
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as favorite
	FROM :interactions_table_name
	WHERE interactionType = 'favorite_descriptor_from_quick_list_for_user'
		OR interactionType = 'favorite_descriptor_from_quick_list_for_project'
	GROUP BY executedOver
	) t71
	ON t70.executedOver = t71.executedOver
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as  unfavorite
	FROM :interactions_table_name
	WHERE interactionType = 'unfavorite_descriptor_from_quick_list_for_user'
		OR interactionType = 'unfavorite_descriptor_from_quick_list_for_project'
	GROUP BY executedOver
	) t72
	ON t70.executedOver = t72.executedOver
) t7
ON t0.executedOver = t7.executedOver
LEFT JOIN
(
SELECT executedOver, COUNT(*) as frequently_selected_project
FROM :interactions_table_name
WHERE originallyRecommendedFor LIKE ':projectozinho%'
	AND (interactionType = 'accept_descriptor_from_quick_list'
	OR interactionType = 'accept_descriptor_from_manual_list'
	OR interactionType = 'accept_descriptor_from_autocomplete')
GROUP BY executedOver
) t8
ON t0.executedOver = t8.executedOver
LEFT JOIN
(
SELECT executedOver, COUNT(*) as frequently_selected_overall
FROM :interactions_table_name
WHERE (interactionType = 'accept_descriptor_from_quick_list'
	OR interactionType = 'accept_descriptor_from_manual_list'
	OR interactionType = 'accept_descriptor_from_autocomplete')
GROUP BY executedOver
) t9
ON t0.executedOver = t9.executedOver
LEFT JOIN
(
SELECT t101.executedOver, t101.hidden as hiddens_for_user, t102.unhidden as unhiddens_for_user
FROM
	(
		SELECT DISTINCT executedOver
		FROM :interactions_table_name
	) t100
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as hidden
	FROM :interactions_table_name
	WHERE performedBy = ':utilizadorzinho'
		AND interactionType = 'hide_descriptor_from_quick_list_for_user'
	GROUP BY executedOver
	) t101
	ON t100.executedOver = t101.executedOver
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as unhidden
	FROM :interactions_table_name
	WHERE performedBy = ':utilizadorzinho'
		AND interactionType = 'unhide_descriptor_from_quick_list_for_user'
	GROUP BY executedOver
	) t102
	ON t100.executedOver = t102.executedOver
) t10
ON t0.executedOver = t10.executedOver
LEFT JOIN
(
SELECT t111.executedOver, t111.hidden as hiddens_for_project, t112.unhidden as unhiddens_for_project
FROM
	(
		SELECT DISTINCT executedOver
		FROM :interactions_table_name
	) t110
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as hidden
	FROM :interactions_table_name
	WHERE originallyRecommendedFor LIKE ':projectozinho%'
		AND interactionType = 'hide_descriptor_from_quick_list_for_project'
	GROUP BY executedOver
	) t111
	ON t110.executedOver = t111.executedOver
	LEFT JOIN
	(SELECT executedOver, COUNT(*) as unhidden
	FROM :interactions_table_name
	WHERE originallyRecommendedFor LIKE ':projectozinho%'
		AND interactionType = 'unhide_descriptor_from_quick_list_for_project'
	GROUP BY executedOver
	) t112
	ON t110.executedOver = t112.executedOver
) t11
ON t0.executedOver = t11.executedOver
;