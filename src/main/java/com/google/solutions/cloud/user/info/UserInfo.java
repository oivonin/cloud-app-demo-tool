package com.google.solutions.cloud.user.info;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.appengine.api.datastore.Entity;

public class UserInfo {
  public static final String USER_KIND = "User";
  public static final String CURRENT_RESOURCE_NAME_SUFFIX = "currentResourceNameSuffix";

  private String username;
  private int currentResourceNameSuffix;

  public String getUserName() {
    return this.username;
  }

  public int getCurrentResourceNameSuffix() {
    return this.currentResourceNameSuffix;
  }

  public UserInfo setUserName(String newUserName) {
    this.username = newUserName;
    return this;
  }

  public UserInfo setCurrentResourceNameSuffix(int newCurrentResourceNameSuffix) {
    this.currentResourceNameSuffix = newCurrentResourceNameSuffix;
    return this;
  }

  public Entity toDatastoreEntity() {
    checkState(this.username != null && !this.username.isEmpty(),
        "cannot create a Datastore Entity for User instance without username");

    Entity e = new Entity(USER_KIND, this.username);
    e.setProperty(CURRENT_RESOURCE_NAME_SUFFIX, this.currentResourceNameSuffix);
    return e;
  }

  public static UserInfo fromDatastoreEntity(Entity e) {
    checkNotNull(e);

    return new UserInfo()
        .setUserName(e.getKey().getName())
        .setCurrentResourceNameSuffix((int)(long) e.getProperty(CURRENT_RESOURCE_NAME_SUFFIX));
  }
}
