package com.google.solutions.cloud.deployment;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.model.Instance;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Text;
import com.google.common.base.Throwables;
import com.google.solutions.cloud.compute.ComputeInstanceHelper;
import com.google.solutions.cloud.demo.info.DemoInfo;

public class SingleInstanceDeployment extends DeploymentTemplate {
  public static final String INSTANCE_TEMPLATE = "instanceTemplate";

  Instance instanceTemplate;

  public Instance getInstanceTemplate() {
    return this.instanceTemplate;
  }

  public SingleInstanceDeployment setInstanceTemplate(Instance newInstanceTemplate) {
    this.instanceTemplate = newInstanceTemplate;
    return this;
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.deployment.DeploymentTemplate#getDeploymentType()
   */
  @Override
  public DeploymentType getDeploymentType() {
    return DeploymentType.SINGLE_INSTANCE;
  }

  public static SingleInstanceDeployment makeDefaultTemplate(String name) {
    return new SingleInstanceDeployment()
        .setInstanceTemplate(ComputeInstanceHelper.makeDefaultInstance(name));
  }

  public static SingleInstanceDeployment fromEmbeddedEntity(EmbeddedEntity e) {
    checkNotNull(e);

    SingleInstanceDeployment sid = new SingleInstanceDeployment();

    // deserialize the embedded Instance
    Instance instanceTemplate;
    try {
      instanceTemplate = JacksonFactory.getDefaultInstance().fromString(
          ((Text) e.getProperty(INSTANCE_TEMPLATE)).getValue(),
          Instance.class);
    } catch (Exception e1) {
      throw Throwables.propagate(e1);
    }

    return sid.setInstanceTemplate(instanceTemplate);
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.deployment.DeploymentTemplate#toEmbeddedEntity()
   */
  @Override
  public EmbeddedEntity toEmbeddedEntity() {
    String serializedInstanceTemplate;
    try {
      serializedInstanceTemplate = JacksonFactory.getDefaultInstance()
          .toPrettyString(this.instanceTemplate);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    EmbeddedEntity e = new EmbeddedEntity();
        e.setProperty(TYPE, this.getDeploymentType().toString());
        e.setUnindexedProperty(INSTANCE_TEMPLATE, new Text(serializedInstanceTemplate));
    return e;
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.deployment.DeploymentTemplate#launch(com.google.solutions.cloud.deployment.DeploymentManager)
   */
  @Override
  public void launch(DeploymentManager deploymentManager) {
    checkNotNull(deploymentManager);
    deploymentManager.launch(this);
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.deployment.DeploymentTemplate#teardown(com.google.solutions.cloud.deployment.DeploymentManager)
   */
  @Override
  public void teardown(DeploymentManager deploymentManager) {
    checkNotNull(deploymentManager);
    deploymentManager.teardown(this);
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.deployment.DeploymentTemplate#updateDemoInfo(com.google.solutions.cloud.demo.info.DemoInfo)
   */
  @Override
  public void updateDemoInfo(DeploymentManager deploymentManager, DemoInfo demoInfo) {
    checkNotNull(demoInfo);
    deploymentManager.updateDemoInfo(this, demoInfo);
  }
}
