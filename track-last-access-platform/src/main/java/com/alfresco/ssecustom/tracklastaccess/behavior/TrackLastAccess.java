package com.alfresco.ssecustom.tracklastaccess.behavior;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
//import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
//import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
//import org.alfresco.service.cmr.repository.ContentData;
//import org.alfresco.service.cmr.repository.ContentService;
//import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
//import org.alfresco.service.namespace.NamespaceService;
//import org.alfresco.service.cmr.security.PersonService;
//import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.transaction.TransactionListener;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.apache.log4j.Logger;

import com.alfresco.ssecustom.tracklastaccess.model.TrackLastAccessModel;

public class TrackLastAccess 
	implements 
			   ContentServicePolicies.OnContentReadPolicy
			   ,NodeServicePolicies.OnAddAspectPolicy
//			   ,NodeServicePolicies.OnUpdateNodePolicy
			   ,NodeServicePolicies.OnRemoveAspectPolicy
			   ,NodeServicePolicies.OnUpdatePropertiesPolicy
			   {

	private Logger logger = Logger.getLogger(TrackLastAccess.class);
	
	private ServiceRegistry serviceRegistry;
	private PolicyComponent policyComponent;
	private NodeService nodeService;
	private TransactionListener transactionListener;
	
	private static final String KEY_RELATED_NODES = ContentReadTransactionListener.class.getName() + ".relatedNodes";
		
	private QName trkacc = QName.createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL, TrackLastAccessModel.ASPECT_TRKACC_TRACKACCESS);
	private QName accOn = QName.createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL,	TrackLastAccessModel.PROP_ACCESSED_ON);
	private QName accBy = QName.createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL,	TrackLastAccessModel.PROP_ACCESSED_BY);
	
	public void init() {
		logger.debug("**** Inside init() for track last access behaviors");
				
		this.policyComponent.bindClassBehaviour(ContentServicePolicies.OnContentReadPolicy.QNAME,
				trkacc, 
				new JavaBehaviour(this,"onContentRead", NotificationFrequency.TRANSACTION_COMMIT));

		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
				this, 
				new JavaBehaviour(this,"onAddAspect", NotificationFrequency.TRANSACTION_COMMIT));

		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME,
				trkacc, 
				new JavaBehaviour(this,"onUpdateProperties", NotificationFrequency.TRANSACTION_COMMIT));

		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnRemoveAspectPolicy.QNAME,
				this, 
				new JavaBehaviour(this,"onRemoveAspect", NotificationFrequency.TRANSACTION_COMMIT));
		
		this.transactionListener = new ContentReadTransactionListener();

	}
	
	@Override
	public void onContentRead(NodeRef noderef) {
		
		logger.debug("**** Inside onContentRead");
        
		// Bind listener to current transaction
        AlfrescoTransactionSupport.bindListener(transactionListener);
        
        List<NodeRef> nodes = new ArrayList<NodeRef>();
        nodes.add(noderef);
       
        // Transactions involving several nodes need resource updating
        List<NodeRef> existingNodes = AlfrescoTransactionSupport.getResource(KEY_RELATED_NODES);
        if (existingNodes == null) {
            existingNodes = nodes;
        } else {
            existingNodes.addAll(nodes);
        }

        // Put resources to be used in transaction listener
        AlfrescoTransactionSupport.bindResource(KEY_RELATED_NODES, existingNodes);
        
	}	

	@Override
	public void onAddAspect(NodeRef noderef, QName aspectTypeQName) {

		logger.debug("**** Inside onAddAspect");

		try {
			logger.debug("****** Aspect Added:" + aspectTypeQName.getLocalName());
	
			if (trkacc.isMatch(aspectTypeQName) || nodeService.hasAspect(noderef, trkacc)) {
				updateTrackAccessAspect(noderef);
			}
		} catch (Exception e) {
			return;
		}

		
	}

	//Aspect Removal doesn't fire the onUpdateNode event
	@Override
	public void onRemoveAspect(NodeRef noderef, QName aspectTypeQName) {
		
		logger.debug("**** Inside onRemoveAspect");
		try {
			logger.debug("****** Aspect Removed:" + aspectTypeQName.getLocalName());
	
			if (!trkacc.isMatch(aspectTypeQName) && nodeService.hasAspect(noderef,trkacc)) 
			{
				updateTrackAccessAspect(noderef);
			}
		} catch (Exception e) {
			return;
		}

	}		

	@Override
	public void onUpdateProperties(NodeRef noderef, Map<QName, Serializable> before, Map<QName, Serializable> after) {

		logger.debug("**** Inside onUpdateProperties");
		
		try {
			Map<QName, Serializable> diff = mapDiff(before,after);
			printPropMap("Updated",diff);
			if(!(diff.containsKey(accOn) || diff.containsKey(accBy))) {
				updateTrackAccessAspect(noderef);
			} else {
				logger.debug("====== No updates");
			}
		} catch (Exception e) {

			logger.debug(e.getMessage());
		}
		
	}		

	
	//******************************************************
	//
	//**************** Helper functions
	//
	//******************************************************
	
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
	
	private void updateTrackAccessAspect(NodeRef noderef) {
		
		logger.debug("**** Inside updateTrackAccessAspect");
		
		String username = getCurrentUser();
		if (username != null) {
			
			logger.debug("******* Do property update");
			try {
				Map<QName,Serializable> newprops = nodeService.getProperties(noderef);
				
				newprops.put(accBy,username);
				newprops.put(accOn,new Date());
				
				nodeService.setProperties(noderef, newprops);
				logger.debug("======= Props Updated");
			} catch (Exception e) {
				logger.debug("************* Error: " + e.getMessage());
			}
			
		}
		
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

	//******************************************************
	//
	//**************** Transactional Class for onContent Read
	//
	//******************************************************

	private class ContentReadTransactionListener
		extends TransactionListenerAdapter implements TransactionListener {
		
	
		@Override
		public void afterCommit() {
			logger.debug("**** In afterCommit");
			
			List<NodeRef> nodes = AlfrescoTransactionSupport.getResource(KEY_RELATED_NODES);
			
			for (NodeRef noderef:nodes) {
				try {
					if (noderef != null) {
						logger.debug(noderef);	
						updateTrackAccessAspect(noderef);
					}
				} catch (Exception e) {
					logger.debug(e.getMessage());
				}
			}	
			
		}
		
	}
	
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
