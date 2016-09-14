SELECT *
FROM
(
	SELECT
		descriptor,
		(	COALESCE(hides_for_user,0) - COALESCE(unhides_for_user,0) 	) as hidden_for_user,
		(	COALESCE(hides_for_project,0) - COALESCE(unhides_for_project,0)		) as hidden_for_project
	FROM
	(
		SELECT
			descriptors.executedOver as descriptor,
			hides_for_user.hides_count as hides_for_user,
			unhides_for_user.unhides_count as unhides_for_user,

			hides_for_project.hides_count as hides_for_project,
			unhides_for_project.unhides_count as unhides_for_project
		FROM
		(
			SELECT DISTINCT executedOver
			FROM :interactions_table_name
		)
		as descriptors

		LEFT JOIN
		(
			SELECT executedOver, COUNT(*) as hides_count
			FROM :interactions_table_name
			WHERE
				performedBy = ':utilizadorzinho'
			AND
				originallyRecommendedFor LIKE ':projectozinho%'
			AND
				interactionType LIKE 'hide_descriptor_from_quick_list_for_user'
			GROUP BY executedOver
		) as hides_for_user
		on descriptors.executedOver = hides_for_user.executedOver

		LEFT JOIN
		(
			SELECT executedOver, COUNT(*) as unhides_count
			FROM :interactions_table_name
			WHERE
				performedBy = ':utilizadorzinho'
			AND
				originallyRecommendedFor LIKE ':projectozinho%'
			AND
				interactionType LIKE 'unhide_descriptor_from_quick_list_for_user'
			GROUP BY executedOver
		) as unhides_for_user
		on descriptors.executedOver = unhides_for_user.executedOver

		LEFT JOIN
		(
			SELECT executedOver, COUNT(*) as hides_count
			FROM :interactions_table_name
			WHERE
				performedBy = ':utilizadorzinho'
			AND
				originallyRecommendedFor LIKE ':projectozinho%'
			AND
				interactionType LIKE 'hide_descriptor_from_quick_list_for_project'
			GROUP BY executedOver
		) as hides_for_project
		on descriptors.executedOver = hides_for_project.executedOver

		LEFT JOIN
		(
			SELECT executedOver, COUNT(*) as unhides_count
			FROM :interactions_table_name
			WHERE
				performedBy = ':utilizadorzinho'
			AND
				originallyRecommendedFor LIKE ':projectozinho%'
			AND
				interactionType LIKE 'unhide_descriptor_from_quick_list_for_project'
			GROUP BY executedOver
		) as unhides_for_project
		on descriptors.executedOver = unhides_for_project.executedOver

	) as results1
)
as results
WHERE
	results.hidden_for_user > 0
OR
	results.hidden_for_project > 0
