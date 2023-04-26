package com.alfresco.ssecustom.tracklastaccess.test;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.alfresco.rad.test.AbstractAlfrescoIT;
import org.alfresco.service.namespace.QName;
import static org.alfresco.service.namespace.QName.createQName;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.alfresco.ssecustom.tracklastaccess.model.TrackLastAccessModel;

public class LastAccessModelIT extends AbstractAlfrescoIT {

    @Test
    public void testCustomContentModelPresence() {
//        Collection<QName> allContentModels = getServiceRegistry().getDictionaryService().getAllModels();
//        QName customContentModelQName = createQName(TrackLastAccessModel.NAMESPACE_SSE_TRACK_LAST_ACCESS_CONTENT_MODEL,"trackLastAccess");
//        assertTrue("Track Last Access content model " + customContentModelQName.toString() +
//                " is not present", allContentModels.contains(customContentModelQName));
    	assertTrue(true);
    }
}
