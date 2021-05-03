/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jenkins.plugins.googlecontainerregistryauth;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentialsModule;
import com.thoughtworks.xstream.XStream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.util.Secret;
import jenkins.model.Jenkins;

/**
 * Tests for {@link GoogleContainerRegistryCredential}.
 */
public class GoogleContainerRegistryCredentialTest {
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  /**
   */
  @NameWith(value = Namer.class, priority = 50)
  private abstract static class FakeGoogleRobotCredentials
      extends GoogleRobotCredentials {
    public FakeGoogleRobotCredentials(String a) {
      super(a, new GoogleRobotCredentialsModule());
    }
  }

  /**
   */
  public static class Namer
      extends CredentialsNameProvider<FakeGoogleRobotCredentials> {
    public String getName(FakeGoogleRobotCredentials c) {
      return NAME;
    }
  }

  @Mock
  private FakeGoogleRobotCredentials credentials;

  private GoogleContainerRegistryCredential underTest;

  private static class FakeModule
      extends GoogleContainerRegistryCredentialModule {
    public FakeModule() {
    }

    @Override
    public String getIdentity(GoogleRobotCredentials credentials) {
      return USERNAME;
    }

    @Override
    public GoogleContainerRegistryCredentialModule forRemote(
        GoogleRobotCredentials credentials) {
      return new OtherFakeModule();
    }
  }

  private static class OtherFakeModule
      extends GoogleContainerRegistryCredentialModule {

    public OtherFakeModule() {
    }

    public String getIdentity(GoogleRobotCredentials credentials) {
      return USERNAME;
    }

    @Override
    public GoogleContainerRegistryCredentialModule forRemote(
        GoogleRobotCredentials credentials) {
      return this;
    }
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(credentials.getId()).thenReturn(CREDENTIALS_ID);
    if (jenkins.jenkins != null) {
      SystemCredentialsProvider.getInstance().getCredentials().add(
          credentials);
    }

    underTest = new GoogleContainerRegistryCredential(CREDENTIALS_ID,
        new FakeModule());
  }

  @Test
  public void testDescription() throws Exception {
    assertEquals(NAME, underTest.getDescription());
  }

  @Test
  public void testUsername() throws Exception {
    assertEquals(USERNAME, underTest.getUsername());
  }

  @Test
  public void testPassword() throws Exception {
    when(credentials.getAccessToken(isA(GoogleOAuth2ScopeRequirement.class)))
        .thenReturn(Secret.fromString(PASSWORD));

    assertEquals(PASSWORD, Secret.toString(underTest.getPassword()));
  }

  @Test
  public void testNameProvider() {
    assertThat(CredentialsNameProvider.name(underTest),
        containsString(NAME));
  }

  @Test
  public void testWriteObject() throws Exception {
    XStream xstream = new XStream();
    String xml = xstream.toXML(underTest);

    assertThat(xml, containsString("-OtherFakeModule"));
  }

  @Test
  public void testWriteObjectThenReadObjectSameModule() throws Exception {
    underTest = new GoogleContainerRegistryCredential(CREDENTIALS_ID,
        new OtherFakeModule());

    XStream xstream = new XStream();
    String xml = xstream.toXML(underTest);
    System.out.println(xml);

    GoogleContainerRegistryCredential deserialized =
        (GoogleContainerRegistryCredential) xstream.fromXML(xml);
    String reserializedXml = xstream.toXML(deserialized);
    System.out.println(reserializedXml);

    assertNotSame(underTest, deserialized);
    assertTrue(underTest.module instanceof OtherFakeModule);
    assertTrue(deserialized.module instanceof OtherFakeModule);
    assertEquals(underTest.getId(), deserialized.getId());
    assertEquals(underTest.getCredentialsId(), deserialized.getCredentialsId());
    assertEquals(xml, reserializedXml);
  }

  @Test
  public void testWriteObjectThenReadObjectModuleChangeButOnMaster()
      throws Exception {
    XStream xstream = new XStream();
    String xml = xstream.toXML(underTest);

    // Contains OtherFakeModule, in case it is
    // deserialized remotely.
    assertThat(xml, containsString("-OtherFakeModule"));

    GoogleContainerRegistryCredential deserialized =
        (GoogleContainerRegistryCredential) xstream.fromXML(xml);

    assertNotSame(underTest, deserialized);
    assertTrue(underTest.module instanceof FakeModule);
    // With JenkinsRule, we are still a FakeModule
    assertTrue(deserialized.module instanceof FakeModule);
  }

  @Test
  @WithoutJenkins
  public void testWriteObjectThenReadObjectModuleChangeNotOnMaster()
      throws Exception {
    XStream xstream = new XStream();
    String xml = xstream.toXML(underTest);

    GoogleContainerRegistryCredential deserialized =
        (GoogleContainerRegistryCredential) xstream.fromXML(xml);

    assertNotSame(underTest, deserialized);
    assertTrue(underTest.module instanceof FakeModule);
    // WithoutJenkins so we are an OtherFakeModule
    assertTrue(deserialized.module instanceof OtherFakeModule);
  }

  @Test
  public void testDescriptorNotRegistered() {
    assertTrue(Jenkins.get().getDescriptor(
        GoogleContainerRegistryCredential.class) == null);
  }

  private static final String NAME = "foo-bar Container Registry Account";
  private static final String CREDENTIALS_ID = "foobar-cred-id";
  private static final String USERNAME = "_token";
  private static final String PASSWORD = "foobar-password";
}
