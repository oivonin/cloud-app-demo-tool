package com.google.solutions.cloud;

/**
 * Contains the client IDs and scopes for allowed clients consuming your API.
 */
public class Constants {
  public static final String WEB_CLIENT_ID = "replace this with your web client ID";
  public static final String ANDROID_CLIENT_ID = "replace this with your Android client ID";
  public static final String IOS_CLIENT_ID = "replace this with your iOS client ID";
  public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;

  public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";

  // TODO: is it reasonable to have users edit the properties below directly?
  //       or does it need to be "injected" through some "config"?
  public static final String PROJECT_ID = "cloud-app-demo-tool";
  public static final String MACHINE_TYPE = "n1-standard-2";
  public static final String ZONE = "us-central1-a";
  public static final String IMAGE = "ubuntu-14-10";
}
