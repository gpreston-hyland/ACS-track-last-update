package com.alfresco.ssecustom.tracklastaccess.test;

//import com.alfresco.ssecustom.tracklastaccess.test.TrackLastAccessModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.rad.test.AbstractAlfrescoIT;
import org.alfresco.rad.test.AlfrescoTestRunner;
import org.alfresco.repo.nodelocator.CompanyHomeNodeLocator;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.alfresco.service.namespace.QName.createQName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(value = AlfrescoTestRunner.class)
public class LastAccessModelIT extends AbstractAlfrescoIT {
	NodeRef nodeRef;
	
	final QName PROP_ACC_BY = QName.createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL,
				TrackLastAccessModel.PROP_ACCESSED_BY);
	final QName PROP_ACC_ON = QName.createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL,
			TrackLastAccessModel.PROP_ACCESSED_ON);

    @Test
    public void testCustomContentModelPresence() {
        Collection<QName> allContentModels = getServiceRegistry().getDictionaryService().getAllModels();
        QName customContentModelQName = createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL,
        		TrackLastAccessModel.NAME_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL);
        assertTrue("Track Last Access content model " + customContentModelQName.toString() +
                " is not present", allContentModels.contains(customContentModelQName));
    }
    
    @Test
    public void testCreateAccessDataType() {
    	NodeService nodeService = getServiceRegistry().getNodeService();

        QName type = createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL, 
        		TrackLastAccessModel.TYPE_TRKACC_LASTACCESSDATA);
//        String textContent = "Test document";
        
        Map<QName,Serializable> nodeProps = new HashMap<QName,Serializable>();
        nodeProps.put(PROP_ACC_BY, "someUser");
        nodeProps.put(PROP_ACC_ON, new Date());

        
        this.nodeRef = createNode("TestNode"+ Long.toString(System.currentTimeMillis()),type,nodeProps);
        
        assertEquals("someUser",nodeService.getProperty(nodeRef,PROP_ACC_BY));
        
    }
    
    /* ******************************** */
    /*  HELPER METHODS FROM SDK SAMPLE  */
    /* ******************************** */

    /**
     * Create a new node, such as a file or a folder, with passed in type and properties
     *
     * @param name the name of the file or folder
     * @param type the content model type
     * @param properties the properties from the content model
     * @return the Node Reference for the newly created node
     */
    NodeRef createNode(String name, QName type, Map<QName, Serializable> properties) {
        NodeRef parentFolderNodeRef = getCompanyHomeNodeRef();
        QName associationType = ContentModel.ASSOC_CONTAINS;
        QName associationQName = createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
                QName.createValidLocalName(name));
        properties.put(ContentModel.PROP_NAME, name);
        ChildAssociationRef parentChildAssocRef = getServiceRegistry().getNodeService().createNode(
                parentFolderNodeRef, associationType, associationQName, type, properties);

        return parentChildAssocRef.getChildRef();
    }
    
    /**
     * Get the node reference for the /Company Home top folder in Alfresco.
     * Use the standard node locator service.
     *
     * @return the node reference for /Company Home
     */
    NodeRef getCompanyHomeNodeRef() {
        return getServiceRegistry().getNodeLocatorService().getNode(CompanyHomeNodeLocator.NAME, null, null);
    }
    
    @After
    public void teardown() {
        // Clean up node
        if (nodeRef != null) {
            getServiceRegistry().getNodeService().deleteNode(nodeRef);
        }
    }
 
}
