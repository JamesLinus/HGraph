/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trend.hgraph.util;

import java.security.MessageDigest;

/**
 * @author scott_miao
 *
 */
public class SecurityUtils {

  public static String encrypt(String str, String encType) {
    String result = "";
    try {
      MessageDigest md = MessageDigest.getInstance(encType);
      md.update(str.getBytes());
      result = toHexString(md.digest());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  public static String getSha1(String str) {
    return encrypt(str, "SHA1");
  }

  private static String toHexString(byte[] in) {
    StringBuilder hexString = new StringBuilder();
    for (int i = 0; i < in.length; i++) {
      String hex = Integer.toHexString(0xFF & in[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

}
