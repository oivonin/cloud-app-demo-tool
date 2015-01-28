package com.google.solutions.cloud.app.api;

import static com.google.common.base.Preconditions.checkArgument;
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
import com.google.solutions.cloud.demo.info.DemoInfo;
import com.google.solutions.cloud.persistence.DatastoreDemoInfoPersistence;
import com.google.solutions.cloud.resource.ComputeInstance;
import com.google.solutions.cloud.resource.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Named;

/** An endpoint class we are exposing */
@Api(name = "myApi",
     version = "v1",
     namespace = @ApiNamespace(ownerDomain = "cloud.solutions.google.com",
     ownerName = "cloud.solutions.google.com",
     packagePath=""))
public class CloudAppDemoToolAPI {

  private final DatastoreDemoInfoPersistence demoInfoPersistence =
      new DatastoreDemoInfoPersistence(DatastoreServiceFactory.getDatastoreService());

  @ApiMethod(name = "createDemo")
  public DemoInfo createDemo(@Named("description") String description,
      User user) throws UnauthorizedException {
    String username = checkLoginAndGetAbbreviatedNickname(user);
    DemoInfo initialDemoInfo = new DemoInfo().setDescription(description);
    return this.demoInfoPersistence.createNewDemo(username, initialDemoInfo);
  }

  @ApiMethod(name = "getDemoInfo")
  public DemoInfo getDemoInfo(@Named("demoId") long demoId, User user)
      throws UnauthorizedException, NotFoundException {
    String username = checkLoginAndGetAbbreviatedNickname(user);

    Optional<DemoInfo> demoInfo = this.demoInfoPersistence.get(username, demoId);
    if (!demoInfo.isPresent()) {
      throw new NotFoundException(String.format("{ demoId: %d, username: %s }",
          demoId, username));
    }
    return demoInfo.get();
  }

  @ApiMethod(name = "listActiveDemos")
  public List<DemoInfo> listActiveDemos(User user) throws UnauthorizedException {
    String username = checkLoginAndGetAbbreviatedNickname(user);
    return this.demoInfoPersistence.findAllActiveDemosForUser(username);
  }

  @ApiMethod(name = "createInstances")
  public Collection<ComputeInstance> createInstances(@Named("numInstances") int numInstances,
      @Named("demoId") long demoId,
      User user) throws UnauthorizedException {
    checkArgument(numInstances > 0, "numInstances must be positive");
    String username = checkLoginAndGetAbbreviatedNickname(user);

    int baseSuffix = this.demoInfoPersistence.reserveInstanceNames(username, numInstances);
    List<ComputeInstance> instances = new ArrayList<>(numInstances);
    for (int i = 0; i < numInstances; i++) {
      String name = String.format("%s-%d", username, baseSuffix + i);
      ComputeInstance instance = new ComputeInstance()
          .setCreationTime(new Date())
          .setName(name)
          .setZone(Constants.ZONE);
      instances.add(instance);
    }

    Collection<Resource> whyGodWhy = (Collection<Resource>)(Object) instances;
    this.demoInfoPersistence.saveResources(username, demoId, whyGodWhy);

    System.err.println(instances);

    return instances;
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