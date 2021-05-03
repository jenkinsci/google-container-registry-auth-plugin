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

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.List;

import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.domains.SchemeSpecification;
import com.google.common.collect.ImmutableList;
import com.google.jenkins.plugins.credentials.oauth.GoogleOAuth2ScopeRequirement;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;

import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

/**
 * Module to abstract the instantiation of dependencies of the
 * {@link GoogleContainerRegistryCredential} plugin.
 */
public class GoogleContainerRegistryCredentialModule
    implements Serializable {
  private static final GoogleContainerRegistryScopeRequirement SCOPE =
      new GoogleContainerRegistryScopeRequirement();

  public GoogleContainerRegistryCredentialModule() {
  }

  /**
   * @param credentials the underlying {@link GoogleRobotCredentials} this
   *        plugin is wrapping.
   * @return the identity associated with the given
   *         {@code credentials}. Google Container Registry always expects
   *         this to be "_token".
   */
  public String getIdentity(GoogleRobotCredentials credentials) {
    return "_token";
  }

  public GoogleContainerRegistryCredentialModule forRemote(
      GoogleRobotCredentials credentials) throws GeneralSecurityException {
    return new ForRemote(this, credentials);
  }

  public GoogleOAuth2ScopeRequirement getRequirement() {
    return SCOPE;
  }

  /**
   * @param credentials the underlying {@link GoogleRobotCredentials} this
   *        plugin is wrapping.
   * @return an access token for the given {@code credentials}.
   */
  public Secret getToken(GoogleRobotCredentials credentials) {
    return credentials.getAccessToken(getRequirement());
  }

  /**
   * @param requirements provided {@link DomainRequirement} to check.
   * @return whether the credential could be applied to the given requirements.
   */
  public static boolean matches(List<DomainRequirement> requirements) {
    Jenkins jenkins = Jenkins.get();
    if (jenkins == null) {
      throw new IllegalStateException(
          "Jenkins has not been started, or was already shut down");
    }
    GoogleContainerRegistryCredentialGlobalConfig gcrGlobalConfig =
        GlobalConfiguration.all().get(
            GoogleContainerRegistryCredentialGlobalConfig.class);
    Domain gcrDomain = new Domain("GCR", "",
        ImmutableList.of(
            new SchemeSpecification("https"),
            new HostnameSpecification(
                gcrGlobalConfig.getGcrServer(), "")));
    return gcrDomain.test(requirements);
  }

  public static GoogleContainerRegistryScopeRequirement getScope() {
    return SCOPE;
  }

  /**
   * For {@link Serializable}
   */
  private static final long serialVersionUID = 1L;

  private static class ForRemote
      extends GoogleContainerRegistryCredentialModule {
    private final String identity;
    private final GoogleRobotCredentials credentials;

    public ForRemote(GoogleContainerRegistryCredentialModule parent,
        GoogleRobotCredentials credentials)
            throws GeneralSecurityException {
      this.identity = parent.getIdentity(credentials);
      this.credentials = credentials.forRemote(parent.getRequirement());
    }

    /** {@inheritDoc} */
    @Override
    public GoogleContainerRegistryCredentialModule forRemote(
        GoogleRobotCredentials credentials) {
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getIdentity(GoogleRobotCredentials credentials) {
      return identity;
    }

    /** {@inheritDoc} */
    @Override
    public Secret getToken(GoogleRobotCredentials credentials) {
      return super.getToken(this.credentials);
    }
  }
}
