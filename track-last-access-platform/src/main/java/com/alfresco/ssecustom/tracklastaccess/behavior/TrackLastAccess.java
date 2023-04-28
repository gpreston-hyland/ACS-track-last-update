package com.alfresco.ssecustom.tracklastaccess.behavior;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.nodelocator.CompanyHomeNodeLocator;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.transaction.TransactionListener;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.apache.log4j.Logger;

import com.alfresco.ssecustom.tracklastaccess.model.TrackLastAccessModel;

public class TrackLastAccess 
	implements 
			   ContentServicePolicies.OnContentReadPolicy
			   ,NodeServicePolicies.OnAddAspectPolicy
			   ,NodeServicePolicies.OnRemoveAspectPolicy
			   ,NodeServicePolicies.OnUpdatePropertiesPolicy
			   {

	private Logger logger = Logger.getLogger(TrackLastAccess.class);
	
	private ServiceRegistry serviceRegistry;
	private PolicyComponent policyComponent;
	private NodeService nodeService;
//	private TransactionListener transactionListener;
	
//	private static final String KEY_RELATED_NODES = ContentReadTransactionListener.class.getName() + ".relatedNodes";
	private static final String ASSN_DATA_NODE_NAME = "AccessData";
		
	private QName trkaccASP = createQName(TrackLastAccessModel.ASPECT_TRKACC_TRACKACCESS);
	private QName trkaccASSN = createQName(TrackLastAccessModel.ASSN_TRKACC_ACCESSDATAASSOC);
	private QName trkaccTYPE = createQName(TrackLastAccessModel.TYPE_TRKACC_LASTACCESSDATA);
	
	
	private QName accOn = createQName(TrackLastAccessModel.PROP_ACCESSED_ON);
	private QName accBy = createQName(TrackLastAccessModel.PROP_ACCESSED_BY);
	
	public void init() {
		logger.debug("**** Inside init() for track last access behaviors");
				
		// The Add/Remove Aspect are bound for ** ALL ** aspects -- need logic to validate affected node has the trkacc aspect.
		// The add/remove aspect behaviors do not fire update properties, but modifiedAt date is still updated. It
		// 		must be a system function
		
		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
				this, 
				new JavaBehaviour(this,"onAddAspect", NotificationFrequency.TRANSACTION_COMMIT));

		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnRemoveAspectPolicy.QNAME,
				this, 
				new JavaBehaviour(this,"onRemoveAspect", NotificationFrequency.TRANSACTION_COMMIT));

		// Content Read and Update Properties are bound only to nodes with the trkacc aspect
		
		this.policyComponent.bindClassBehaviour(ContentServicePolicies.OnContentReadPolicy.QNAME,
				trkaccASP, 
				new JavaBehaviour(this,"onContentRead", NotificationFrequency.TRANSACTION_COMMIT));

		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME,
				trkaccASP, 
				new JavaBehaviour(this,"onUpdateProperties", NotificationFrequency.TRANSACTION_COMMIT));

		//
//		this.transactionListener = new ContentReadTransactionListener();

	}
	
	@Override
	public void onAddAspect(NodeRef noderef, QName aspectTypeQName) {

		logger.debug("**** Inside onAddAspect: " + aspectTypeQName.getLocalName());
		
		if(aspectTypeQName.isMatch(trkaccASP)) {
			// create the child association type
			Map<QName,Serializable> props = new HashMap<QName,Serializable>();
			props.put(ContentModel.PROP_NAME, ASSN_DATA_NODE_NAME);
			props.put(accBy, "test");
			props.put(accOn, new Date());
			
			try {
				logger.debug("Creating child data access node");
				
				
				@SuppressWarnings("unused")
				ChildAssociationRef association = nodeService.createNode(
						noderef, trkaccASSN, createQName(ASSN_DATA_NODE_NAME) , trkaccTYPE, props);
				
				updateTrackAccessAspect(noderef);
				
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
			
		}
		else {
			if(nodeService.hasAspect(noderef, trkaccASP)) {
				try {
					updateTrackAccessAspect(noderef);
				} catch (Exception e) {
					logger.debug(e.getMessage());
				}
			}
		}
		
		logger.debug("**** Exit onAddAspect");
	}

	//Aspect Removal doesn't fire the onUpdateNode event
	@Override
	public void onRemoveAspect(NodeRef noderef, QName aspectTypeQName) {
		
		logger.debug("**** Inside onRemoveAspect: " + aspectTypeQName.getLocalName());
		
		if(aspectTypeQName.isMatch(trkaccASP)) {

			try {
				NodeRef child = nodeService.getChildByName(noderef, trkaccASSN, ASSN_DATA_NODE_NAME);
				
				if (child != null) {
					//remove association
					logger.debug("***** Removing access data child");
					nodeService.removeChild(noderef, child);
				}
				
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
		}
		else {
			try {	
				if (nodeService.hasAspect(noderef,trkaccASP)) 
				{
					updateTrackAccessAspect(noderef);
				}
			} catch (Exception e) {
				logger.debug(e.getMessage());
			}
		}

	}		

	@Override
	public void onContentRead(NodeRef noderef) {
		
		logger.debug("**** Inside onContentRead");
		updateTrackAccessAspect(noderef);
        
//		// Bind listener to current transaction
//        AlfrescoTransactionSupport.bindListener(transactionListener);
//        
//        List<NodeRef> nodes = new ArrayList<NodeRef>();
//        nodes.add(noderef);
//       
//        // Transactions involving several nodes need resource updating
//        List<NodeRef> existingNodes = AlfrescoTransactionSupport.getResource(KEY_RELATED_NODES);
//        if (existingNodes == null) {
//            existingNodes = nodes;
//        } else {
//            existingNodes.addAll(nodes);
//        }
//
//        // Put resources to be used in transaction listener
//        AlfrescoTransactionSupport.bindResource(KEY_RELATED_NODES, existingNodes);
        
	}	

	@Override
	public void onUpdateProperties(NodeRef noderef, Map<QName, Serializable> before, Map<QName, Serializable> after) {

		logger.debug("**** Inside onUpdateProperties");

		updateTrackAccessAspect(noderef);
		
	}		

	
	//******************************************************
	//
	//**************** Helper functions
	//
	//******************************************************
	
	private void updateTrackAccessAspect(NodeRef noderef) {
		
		logger.debug("**** Inside updateTrackAccessAspect");
		
		// Get Child association & update the properties on it
		NodeRef child = null;
		
		try {
			child = getDataNode(noderef);
		
			if (child != null) {
				
				String username = getCurrentUser();
				if (username != null) {
					
					logger.debug("******* Do property update");
					try {
						Map<QName,Serializable> newprops = nodeService.getProperties(child);
						
						newprops.put(accBy,username);
						newprops.put(accOn,new Date());
						
						nodeService.setProperties(child, newprops);
						logger.debug("======= Props Updated");
					} catch (Exception e) {
						logger.debug("************* Error: " + e.getMessage());
					}				
				}
			}
			else {
				logger.debug("****** Unable to locate AccessData child for: " + noderef.getId());
			}
		} catch (Exception e) {
			//Do nothing - content reads by solr won't have security context & call will fail
		}
	}

	private NodeRef getDataNode(NodeRef noderef) {
		NodeRef n = null;
		
		logger.debug("**** In getDataNode");
		try {
			List<ChildAssociationRef> children = nodeService.getChildAssocs(noderef);
			
			for (ChildAssociationRef child : children) {
				logger.debug("****** child:" + child.getTypeQName().getLocalName());
				if(child.getTypeQName().isMatch(trkaccASSN)) {
					return child.getChildRef();
				}
			}
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}
		
		return n;
	}
	
    /**
     * Create a QName for the content model
     *
     * @param localname the local content model name without namespace specified
     * @return the full  QName including namespace
     */
	private QName createQName(String localname) {
		return QName.createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL + localname);
	}

	
//    /**
//     * Create a new node, such as a file or a folder, with passed in type and properties
//     *
//     * @param name the name of the file or folder
//     * @param type the content model type
//     * @param properties the properties from the content model
//     * @return the Node Reference for the newly created node
//     */
//    private NodeRef createNode(String name, QName type, Map<QName, Serializable> properties) {
//        NodeRef parentFolderNodeRef = getCompanyHomeNodeRef();
//        QName associationType = ContentModel.ASSOC_CONTAINS;
//        QName associationQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
//                QName.createValidLocalName(name));
//        properties.put(ContentModel.PROP_NAME, name);
//        ChildAssociationRef parentChildAssocRef = getServiceRegistry().getNodeService().createNode(
//                parentFolderNodeRef, associationType, associationQName, type, properties);
//
//        return parentChildAssocRef.getChildRef();
//    }
    
    /**
     * Get the node reference for the /Company Home top folder in Alfresco.
     * Use the standard node locator service.
     *
     * @return the node reference for /Company Home
     */
    private NodeRef getCompanyHomeNodeRef() {
        return serviceRegistry.getNodeLocatorService().getNode(CompanyHomeNodeLocator.NAME, null, null);
    }
    
    private String getCurrentUser() {
		logger.debug("**** Inside getCurrentUser");
		String username = null;
		// Get services
		AuthenticationService authService = (AuthenticationService)serviceRegistry.getAuthenticationService();
		try {
			if (authService != null) {
				if (!authService.isCurrentUserTheSystemUser()) {
					username = authService.getCurrentUserName();
					logger.debug("**** username:" + username);

				} else {
					logger.debug("****** IS THE SYSTEM USER ******");
				}
			}
		} catch(Exception e) {
			// Ignore errors - non-user access (solr, etc) is on different thread & doesn't have security context & getCurrentUser name fails
//			logger.debug(e.getMessage());
		}
		return username;
	}

	Map<QName,Serializable> mapDiff(Map<QName,Serializable> m1, Map<QName,Serializable> m2) {
		logger.debug("**** Inside mapDiff");
		
		Map<QName,Serializable> map = new HashMap<QName,Serializable>();
		
		for (Map.Entry<QName,Serializable> entry : m1.entrySet()) {
//			logger.debug("M1:"+entry.getKey().getLocalName() + "--" + entry.getValue());
//			logger.debug("M2:"+entry.getKey().getLocalName() + "--" + m2.get(entry.getKey()));
			
			if(entry.getValue() != null) {
				if(!entry.getValue().equals(m2.get(entry.getKey()))) {
					map.put(entry.getKey(), m2.get(entry.getKey()));
				}				
			}
			else {
				if(m2.get(entry.getKey()) != null) {
					map.put(entry.getKey(), m2.get(entry.getKey()));
				}
			}
		}
		
		return map;
	}
	
	void printPropMap(String nameStr, Map<QName,Serializable> map) {
		logger.debug("**** Inside printPropMap ** " + nameStr + " **");
		
		for (var entry:map.entrySet()) {
			logger.debug("**** -- " + entry.getKey().getLocalName() + ":" + entry.getValue());
		}
	}
	

	//******************************************************
	//
	//**************** Transactional Class for onContent Read
	//
	//******************************************************

//	private class ContentReadTransactionListener
//		extends TransactionListenerAdapter implements TransactionListener {
//		
//	
//		@Override
//		public void afterCommit() {
//			logger.debug("**** In afterCommit");
//			
//			List<NodeRef> nodes = AlfrescoTransactionSupport.getResource(KEY_RELATED_NODES);
//			
//			for (NodeRef noderef:nodes) {
//				try {
//					if (noderef != null) {
//						logger.debug(noderef);	
//						updateTrackAccessAspect(noderef);
//					}
//				} catch (Exception e) {
//					logger.debug(e.getMessage());
//				}
//			}	
//			
//		}
//		
//	}
	
	//******************************************************
	//
	//**************** Getters & Setters
	//
	//******************************************************

	
	public PolicyComponent getPolicyComponent() {
		return policyComponent;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

}
