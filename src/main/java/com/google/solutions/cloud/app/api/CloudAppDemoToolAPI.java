package com.google.solutions.cloud.app.api;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.users.User;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.solutions.cloud.Constants;
import com.google.solutions.cloud.compute.ComputeService;
import com.google.solutions.cloud.demo.info.DemoInfo;
import com.google.solutions.cloud.demo.info.DemoStatus;
import com.google.solutions.cloud.deployment.DeploymentManager;
import com.google.solutions.cloud.deployment.DeploymentTemplate;
import com.google.solutions.cloud.deployment.SingleInstanceDeployment;
import com.google.solutions.cloud.persistence.DatastoreDemoInfoPersistence;

import java.util.ConcurrentModificationException;
import java.util.List;

import javax.inject.Named;

/** An endpoint class we are exposing */
@Api(name = "cloudAppDemoTool",
     version = "v1",
     scopes = {Constants.EMAIL_SCOPE},
     clientIds = {Constants.WEB_CLIENT_ID},
     namespace = @ApiNamespace(ownerDomain = "cloud.solutions.google.com",
     ownerName = "cloud.solutions.google.com",
     packagePath=""))
public class CloudAppDemoToolAPI {

  private final DatastoreDemoInfoPersistence demoInfoPersistence =
      new DatastoreDemoInfoPersistence(DatastoreServiceFactory.getDatastoreService());
  private final DeploymentManager deploymentManager =
      new DeploymentManager(new ComputeService());

  @ApiMethod(name = "createSingleInstanceDemo")
  public DemoInfo createSingleInstanceDemo(@Named("description") String description,
      User user) throws UnauthorizedException {
    String username = checkLoginAndGetAbbreviatedNickname(user);

    int suffix = this.demoInfoPersistence.reserveInstanceNames(username, 1);
    String instanceName = String.format("%s-%d", username, suffix);
    DeploymentTemplate deploymentTemplate = SingleInstanceDeployment
        .makeDefaultTemplate(instanceName);

    DemoInfo initialDemoInfo = new DemoInfo()
        .setDescription(description)
        .setDeploymentTemplate(deploymentTemplate);

    return this.demoInfoPersistence.createNewDemo(username, initialDemoInfo);
  }

  @ApiMethod(name = "getDemoInfo")
  public DemoInfo getDemoInfo(@Named("demoId") long demoId, User user)
      throws UnauthorizedException, NotFoundException {
    String username = checkLoginAndGetAbbreviatedNickname(user);

    Optional<DemoInfo> demoInfoOpt = this.demoInfoPersistence.get(username, demoId);
    if (!demoInfoOpt.isPresent()) {
      throw new NotFoundException(String.format("{ demoId: %d, username: %s }",
          demoId, username));
    }

    DemoInfo demoInfo = demoInfoOpt.get();
    DemoStatus status = demoInfo.getStatus();
    switch(status) {
      case CREATED:
      case DELETING:
        break;
      case LAUNCHED:
        demoInfo.getDeploymentTemplate().updateDemoInfo(this.deploymentManager, demoInfo);
        break;
      default:
        throw new IllegalStateException("invalid demo status: " + status);
    }

    return demoInfo;
  }

  @ApiMethod(name = "listActiveDemos")
  public List<DemoInfo> listActiveDemos(User user) throws UnauthorizedException {
    String username = checkLoginAndGetAbbreviatedNickname(user);
    return this.demoInfoPersistence.findAllActiveDemosForUser(username);
  }

  @ApiMethod(name = "launchDemo")
  public void launchDemo(@Named("demoId") long demoId, User user)
      throws UnauthorizedException, NotFoundException {
    String username = checkLoginAndGetAbbreviatedNickname(user);
    DemoInfo demoInfo = this.getDemoInfo(demoId, user);
    DemoStatus status = demoInfo.getStatus();

    switch(status) {
      case DELETING:
        throw new ConcurrentModificationException(String.format(
            "cannot launch demo %d for user %s -- already delet(ed/ing)",
            demoId, username));
      case CREATED:
        demoInfo.getDeploymentTemplate().launch(this.deploymentManager);
        // if the call below were to fail randomly, that's ok -- the client
        // could just retry this call until it succeeds (yes, the 'insert'
        // calls to the compute API will fail, but that won't be visible here,
        // since we aren't polling that status in this method)
        this.demoInfoPersistence.updateStatus(username, demoId, DemoStatus.LAUNCHED);
      // intentional fall through from CREATED to LAUNCHED
      case LAUNCHED:
        // NOTE: in the case of a single instance deployment demo, launch is
        //       an idempotent operation. But since the launch mechanism is at
        //       least nominally more general than that, we won't make the
        //       assumption that all deployments must be idempotent here
        break;
      default:
        throw new IllegalStateException("invalid demo status: " + status);
    }
  }

  @ApiMethod(name = "teardownDemo")
  public void teardownDemo(@Named("demoId") long demoId, User user)
        throws UnauthorizedException, NotFoundException {
    String username = checkLoginAndGetAbbreviatedNickname(user);
    DemoInfo demoInfo = this.getDemoInfo(demoId, user);
    DemoStatus status = demoInfo.getStatus();

    // assumption: deletion is idempotent
    switch(status) {
      case CREATED:
      case DELETING:
      case LAUNCHED:
        demoInfo.getDeploymentTemplate().teardown(this.deploymentManager);
        this.demoInfoPersistence.updateStatus(username, demoId, DemoStatus.DELETING);
        break;
      default:
        throw new IllegalStateException("invalid demo status: " + status);
    }
  }

  // FIXME: delete this method...just for testing
  @ApiMethod(name = "deleteDemoInfo")
  @VisibleForTesting
  public void deleteDemoInfo(@Named("demoId") long demoId, User user)
      throws UnauthorizedException {
    String username = checkLoginAndGetAbbreviatedNickname(user);
    this.demoInfoPersistence.delete(username, demoId);
  }

  private static String checkLoginAndGetAbbreviatedNickname(User user) throws UnauthorizedException {
    if (user == null) {
      throw new UnauthorizedException("must be authenticated to access this service!");
    }
    return getNicknameWithoutAuthDomain(user);
  }

  private static String getNicknameWithoutAuthDomain(User user) {
    checkNotNull(user);

    String nickname = user.getNickname();
    int atIndex = nickname.indexOf('@', 0);
    return atIndex <= 0 ? nickname : nickname.substring(0, atIndex);
  }
}