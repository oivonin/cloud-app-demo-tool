package com.google.solutions.cloud.demo.info;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.solutions.cloud.deployment.DeploymentTemplate;

import java.util.Date;
import java.util.Map;

/**
 * "Java bean"-style class for holding information about a single demo
 * instance, within a particular demo app, specifically intended for usage
 * with GAE Datastore.
 */
public class DemoInfo {
  public static final String DEMO_KIND = "Demo";
  public static final String STATUS = "status";
  public static final String DESCRIPTION = "description";
  public static final String CREATION_TIME = "creationTime";
  public static final String DEPLOYMENT_TEMPLATE = "deploymentTemplate";

  private Long demoId;
  private DemoStatus status;
  private String description;
  private Date creationTime;
  private DeploymentTemplate deploymentTemplate;

  // TODO: enum this...
  private String deploymentStatus;
  private Map<String, String> fullDeploymentMetadata;
  // ...also note that the fields above are not saved to DataStore, as they are
  // synthetic fields, populated a) by default b) based on compute API calls

  public Long getDemoId() {
    return this.demoId;
  }

  public DemoStatus getStatus() {
    return this.status;
  }

  public String getDescription() {
    return this.description;
  }

  public Date getCreationTime() {
    return this.creationTime;
  }

  public DeploymentTemplate getDeploymentTemplate() {
    return this.deploymentTemplate;
  }

  public String getDeploymentStatus() {
    return this.deploymentStatus;
  }

  public Map<String, String> getFullDeploymentMetadta() {
    return this.fullDeploymentMetadata;
  }

  public DemoInfo setDemoId(Long newDemoId) {
    this.demoId = newDemoId;
    return this;
  }

  public DemoInfo setStatus(DemoStatus newStatus) {
    this.status = newStatus;
    return this;
  }

  public DemoInfo setDescription(String newDescription) {
    this.description = newDescription;
    return this;
  }

  public DemoInfo setCreationTime(Date newCreationTime) {
    this.creationTime = newCreationTime;
    return this;
  }

  public DemoInfo setDeploymentTemplate(DeploymentTemplate newDeploymentTemplate) {
    this.deploymentTemplate = newDeploymentTemplate;
    return this;
  }

  public DemoInfo setDeploymentStatus(String newDeploymentStatus) {
    this.deploymentStatus = newDeploymentStatus;
    return this;
  }

  public DemoInfo setFullDeploymentMetadta(Map<String, String> newFullDeploymentMetadata) {
    this.fullDeploymentMetadata = newFullDeploymentMetadata;
    return this;
  }


  public Entity toDatastoreEntity(Key parentKey) {
    checkNotNull(parentKey);

    Entity e;
    if (this.demoId != null) {
      e = new Entity(DEMO_KIND, this.demoId, parentKey);
    } else {
      e = new Entity(DEMO_KIND, parentKey);
    }

    // indexed
    e.setProperty(STATUS, this.status.toString());
    e.setProperty(CREATION_TIME, this.creationTime);
    // unindexed
    e.setUnindexedProperty(DEPLOYMENT_TEMPLATE, this.deploymentTemplate.toEmbeddedEntity());
    e.setUnindexedProperty(DESCRIPTION, this.description);

    return e;
  }

  @SuppressWarnings("unchecked")
  public static DemoInfo fromDatastoreEntity(Entity e) {
    checkNotNull(e);

    return new DemoInfo()
        .setDemoId(e.getKey().getId())
        .setStatus(DemoStatus.valueOf((String) e.getProperty(STATUS)))
        .setCreationTime((Date) e.getProperty(CREATION_TIME))
        .setDescription((String) e.getProperty(DESCRIPTION))
        .setDeploymentTemplate(DeploymentTemplate.fromEmbeddedEntity(
            (EmbeddedEntity) e.getProperty(DEPLOYMENT_TEMPLATE)));
  }
}
