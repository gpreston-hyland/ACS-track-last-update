# Track Last Access
The `cm:accessed` property in the cm:ContentModel is used with Versions and Records Management. It isn't maintained otherwise. This AMP implements the Track Last Access aspect `trkacc:trackAccess` with a custom behavior to update the Accessed On and Accessed By properties for a document. The `trkacc:trackAccess` aspect creates a child association to node type `trkacc:lastAccessData`; a content-less node with to track Accessed By and Accessed On values. **The data for the last access information must can't be used as properties on the aspect since updating an aspect's properties would update the last modified information.**

#### Business Case
As a content owner I would like to take actions based on either last modified or last access date. The action could be declaring item as records, marking them as inactive, and so on. Last access is defined as the most recent metadata update, document view, or document download. Last access is ***not*** affected by a document appearing in query results.

## Implementation
*This project was created with the Alfresco SDK 4.5 AIO archetype. By default, the Alfresco 7.3.0 community versions are listed in pom.xml. Those have been updated to 7.3.1 to support my Mac Apple Silicon images. I also had to modify the [docker-compose.yml](docker/docker-compose.yml) to use newer, arm64 images.*

The custom model's behavior binds to four (4) behavior policies.
- org.alfresco.repo.content.ContentServicePolicies - `onContentRead`
- org.alfresco.repo.node.NodeServicePolicies - `onAddAspect`
- org.alfresco.repo.node.NodeServicePolicies - `onRemoveAspect`
- org.alfresco.repo.node.NodeServicePolicies - `onUpdateProperties`

The Content Service Policy is for Content reads (display and download). It also fires when a rendition is generated or it get indexed (Solr for now). Those events are in a different security context and required special handling to ignore.

The Node Service Policies are in place to mirror times when the last modification is updated. 

Since aspects may or may not have properties, adding or removing them doesn't trigger the `onUpdateProperties` policy. The down side, at least as far a testing in the SDK, was they needed to be bound to **ANY** aspect, not just the one implemented here. On the initial `./run.sh build_start` or after running a `./run.sh purge` in the current configuration, some patching is done to the database. **Binding the custom behaviors to all aspects caused the system to not-start.** To work around the issue, temporarily update the lines in the [behavior/TrackLastAccess.java](track-last-access-platform/src/main/java/com/alfresco/ssecustom/tracklastaccess/behavior/TrackLastAccess.java) shown.

		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,
				this, 
				new JavaBehaviour(this,"onAddAspect", NotificationFrequency.TRANSACTION_COMMIT));

		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnRemoveAspectPolicy.QNAME,
				this, 
				new JavaBehaviour(this,"onRemoveAspect", NotificationFrequency.TRANSACTION_COMMIT));

Change the `this` parameter to `trkaccASP`. Save and run the build_start directive. Once the system starts normally and the first run patching completes, change the parameter back to `this`, save, and re-execute the build_start.

## Still to Do
I'd like to find a way to display the last access data from the associated node when viewing/editing the parent node's properties. I haven't found the magic for the [share-config-custom.xml](track-last-access-share/src/main/resources/META-INF/share-config-custom.xml).

