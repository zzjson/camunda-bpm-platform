/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
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
package org.camunda.bpm.engine.test.util.testcontainers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.utility.TestcontainersConfiguration;

public class TestcontainersHelper {

  public static String getRegistryUrl() {
    String registryUrl = (String) TestcontainersConfiguration.getInstance().getProperties().getOrDefault("docker.registry.url", "");
    if (StringUtils.isEmpty(registryUrl)) {
      return StringUtils.EMPTY;
    }
    return StringUtils.appendIfMissing(registryUrl, "/");
  }

  public static String resolvePostgreSQLImageName() {
    return resolveImageName("postgresql.container.image");
  }

  public static String resolveOracleImageName() {
    return resolveImageName("oracle.container.image");
  }

  public static String resolveMySQLImageName() {
    return resolveImageName("mysql.container.image");
  }

  public static String resolveMariaDBImageName() {
    return resolveImageName("mariadb.container.image");
  }

  public static String resolveCockroachImageName() {
    return resolveImageName("cockroach.container.image");
  }

  public static String resolveMSSQLImageName() {
    return resolveImageName("mssql.container.image");
  }

  public static String resolveDb2ImageName() {
    return resolveImageName("db2.container.image");
  }

  protected static String resolveImageName(String imageProperty) {
    String image = TestcontainersConfiguration.getInstance().getProperties().getProperty(imageProperty);
    if (image == null) {
      throw new IllegalStateException("An image to use for Oracle containers must be configured. " +
          "To do this, please place a file on the classpath named `testcontainers.properties`, " +
          "containing `" + imageProperty + "=IMAGE`, where IMAGE is a suitable image name and tag.");
    } else {
      return image;
    }
  }
}