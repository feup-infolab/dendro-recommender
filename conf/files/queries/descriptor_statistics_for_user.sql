SELECT 	overall_use_count.descriptor
		overall_use_count, 
		recent_use_count,
		user_use_count,
		last_use,
		times_accept_descriptor_from_quick_list, 
		times_accept_descriptor_from_manual_list,
		times_accept_favorite_descriptor_in_metadata_editor,
		times_accept_descriptor_from_autocomplete,
		times_hide_descriptor_from_quick_list_for_project,
		times_hide_descriptor_from_quick_list_for_user,
		times_unhide_descriptor_from_quick_list_for_user,
		times_reject_descriptor_from_metadata_editor,
		times_favorite_descriptor_from_quick_list_for_user,
		times_unfavorite_descriptor_from_quick_list_for_user,
		times_favorite_descriptor_from_quick_list_for_project,
		times_unfavorite_descriptor_from_quick_list_for_project,
		times_reject_ontology_from_quick_list,
		times_browse_to_next_page_in_descriptor_list,
		times_browse_to_previous_page_in_descriptor_list,
		times_unfavorite_descriptor_from_quick_list_for_user,
		times_select_ontology_manually,
		times_select_descriptor_from_manual_list
FROM 
(
	(
		select 	executedOver as descriptor, 
				count(*) as overall_use_count
		from interactions
		group by executedOver
	) as overall_use_count
	left join
	(
		select 	executedOver as descriptor, 
				count(*) as user_use_count
		from interactions
		where performedBy = ?
		group by executedOver
		order by created
	) as user_use_count
	ON overall_use_count.descriptor =  user_use_count.descriptor
	left join 
	(
		select executedOver as descriptor,
				count(*) 
				from ( 
					select * 
					from interactions
					where performedBy = ?
					order by created desc
					limit 50
				)
		as recent_use_count
		group by executedOver
		
	) as recent_use_count
	ON overall_use_count.descriptor =  recent_use_count.descriptor
	left join
	(
		select executedOver as descriptor,
			   max(modified) as last_use
		from interactions
		group by executedOver
	) as last_use
	ON overall_use_count.descriptor =  last_use.descriptor
	left join 
	(
		select executedOver as descriptor,
			   count(*) as times_accept_descriptor_from_quick_list
		from interactions
		where interactionType = 'accept_descriptor_from_quick_list'
		group by executedOver
	) as times_accept_descriptor_from_quick_list
	ON overall_use_count.descriptor =  times_accept_descriptor_from_quick_list.descriptor
	left join 
	(
		select executedOver as descriptor,
			   count(*) as times_accept_descriptor_from_manual_list
		from interactions
		where interactionType = 'accept_descriptor_from_manual_list'
		group by executedOver
	) as times_accept_descriptor_from_manual_list
	ON overall_use_count.descriptor =  times_accept_descriptor_from_manual_list.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_accept_favorite_descriptor_in_metadata_editor
		from interactions
		where interactionType = 'accept_favorite_descriptor_in_metadata_editor'
		group by executedOver	
	) as times_accept_favorite_descriptor_in_metadata_editor
	ON overall_use_count.descriptor =  times_accept_favorite_descriptor_in_metadata_editor.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_accept_descriptor_from_autocomplete
		from interactions
		where interactionType = 'accept_descriptor_from_autocomplete'
		group by executedOver	
	) as times_accept_descriptor_from_autocomplete
	ON overall_use_count.descriptor =  times_accept_descriptor_from_autocomplete.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_hide_descriptor_from_quick_list_for_project
		from interactions
		where interactionType = 'hide_descriptor_from_quick_list_for_project'
		group by executedOver	
	) as times_hide_descriptor_from_quick_list_for_project
	ON overall_use_count.descriptor =  times_hide_descriptor_from_quick_list_for_project.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_hide_descriptor_from_quick_list_for_user
		from interactions
		where interactionType = 'hide_descriptor_from_quick_list_for_user'
		group by executedOver	
	) as times_hide_descriptor_from_quick_list_for_user
	ON overall_use_count.descriptor =  times_hide_descriptor_from_quick_list_for_user.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_unhide_descriptor_from_quick_list_for_user
		from interactions
		where interactionType = 'unhide_descriptor_from_quick_list_for_user'
		group by executedOver	
	) as times_unhide_descriptor_from_quick_list_for_user
	ON overall_use_count.descriptor =  times_unhide_descriptor_from_quick_list_for_user.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_reject_descriptor_from_metadata_editor
		from interactions
		where interactionType = 'reject_descriptor_from_metadata_editor'
		group by executedOver	
	) as times_reject_descriptor_from_metadata_editor
	ON overall_use_count.descriptor =  times_reject_descriptor_from_metadata_editor.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_favorite_descriptor_from_quick_list_for_user
		from interactions
		where interactionType = 'favorite_descriptor_from_quick_list_for_user'
		group by executedOver	
	) as times_favorite_descriptor_from_quick_list_for_user
	ON overall_use_count.descriptor =  times_favorite_descriptor_from_quick_list_for_user.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_unfavorite_descriptor_from_quick_list_for_user
		from interactions
		where interactionType = 'unfavorite_descriptor_from_quick_list_for_user'
		group by executedOver	
	) as times_unfavorite_descriptor_from_quick_list_for_user
	ON overall_use_count.descriptor =  times_unfavorite_descriptor_from_quick_list_for_user.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_favorite_descriptor_from_quick_list_for_project
		from interactions
		where interactionType = 'favorite_descriptor_from_quick_list_for_project'
		group by executedOver	
	) as times_favorite_descriptor_from_quick_list_for_project
	ON overall_use_count.descriptor =  times_favorite_descriptor_from_quick_list_for_project.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_unfavorite_descriptor_from_quick_list_for_project
		from interactions
		where interactionType = 'unfavorite_descriptor_from_quick_list_for_project'
		group by executedOver	
	) as times_unfavorite_descriptor_from_quick_list_for_project
	ON overall_use_count.descriptor =  times_unfavorite_descriptor_from_quick_list_for_project.descriptor	
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_reject_ontology_from_quick_list
		from interactions
		where interactionType = ''
		group by executedOver	
	) as times_reject_ontology_from_quick_list
	ON overall_use_count.descriptor =  times_reject_ontology_from_quick_list.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_browse_to_next_page_in_descriptor_list
		from interactions
		where interactionType = ''
		group by executedOver	
	) times_browse_to_next_page_in_descriptor_list
	ON overall_use_count.descriptor =  times_browse_to_next_page_in_descriptor_list.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_browse_to_previous_page_in_descriptor_list
		from interactions
		where interactionType = ''
		group by executedOver	
	) as times_browse_to_previous_page_in_descriptor_list
	ON overall_use_count.descriptor =  times_browse_to_previous_page_in_descriptor_list.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_select_ontology_manually
		from interactions
		where interactionType = ''
		group by executedOver	
	) as times_select_ontology_manually
	ON overall_use_count.descriptor =  times_select_ontology_manually.descriptor
	left join 
	(
		select executedOver as descriptor,
	   	count(*) as times_select_descriptor_from_manual_list
		from interactions
		where interactionType = ''
		group by executedOver	
	) as times_select_descriptor_from_manual_list
	ON overall_use_count.descriptor =  times_select_descriptor_from_manual_list.descriptor
)


