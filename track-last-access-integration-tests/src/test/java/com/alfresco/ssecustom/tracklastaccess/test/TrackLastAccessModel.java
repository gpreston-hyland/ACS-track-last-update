package com.alfresco.ssecustom.tracklastaccess.test;

public interface TrackLastAccessModel {
	
	// Namespaces
	public static final String NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL = "http://www.alfresco.com/model/ssecustom/content/1.0";
	public static final String NAME_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL = "trackLastAccess";
		
	// Types
	public static final String TYPE_TRKACC_LASTACCESSDATA = "lastAccessData";
	
	// Aspects
	public static final String ASPECT_TRKACC_TRACKACCESS = "trackAccess";
	
	//Properties
	
	public static final String PROP_ACCESSED_BY = "accessedBy";
	public static final String PROP_ACCESSED_ON = "accessedOn";
	
	//Associations
	public static final String ASSN_TRKACC_ACCESSDATAASSOC = "accessDataAssoc";

}
