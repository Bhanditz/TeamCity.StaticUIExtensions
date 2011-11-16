/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.staticUIExtensions;

import jetbrains.buildServer.staticUIExtensions.model.*;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PlaceId;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 16.11.11 18:50
 */
public class ConfigurationReader {
  private final PagePlacesCollector myCollector;

  public ConfigurationReader(@NotNull final PagePlacesCollector collector) {
    myCollector = collector;
  }

  @NotNull
  public Collection<Rule> parseConfiguration(@NotNull final File config) throws ConfigurationException {
    try {
      final Element root = FileUtil.parseDocument(config);
      return processXml(root);
    } catch (JDOMException e) {
      throw new ConfigurationException("Failed to parse configuration file: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new ConfigurationException("Failed to read configuration file: " + e.getMessage(), e);
    }
  }

  @NotNull
  private Collection<Rule> processXml(@NotNull final Element root) throws ConfigurationException {
    final List rules = root.getChildren("rule");
    final Collection<Rule> result = new ArrayList<Rule>();
    for (Object rule : rules) {
      final Element xmlRule = (Element) rule;

      final String html = xmlRule.getAttributeValue("html-file");
      final String js = xmlRule.getAttributeValue("js-file");
      final String css = xmlRule.getAttributeValue("css-file");

      final StaticContent content = new StaticContent(html, js, css);
      if (!content.isValid()) {
        throw new ConfigurationException("Rule does not contain any file to include. " + xmlRule.toString());
      }

      final String placeId = xmlRule.getAttributeValue("place-id");

      final PlaceId place = myCollector.findByName(placeId);
      if (placeId == null) {
        throw new ConfigurationException("Rule contains unparseable place-id: " + placeId);
      }

      final List<UrlMatcher> matchers = new ArrayList<UrlMatcher>();
      for (Object url : xmlRule.getChildren("url")) {
        final Element xmlUrl = (Element) url;

        final List<UrlMatcher> childMatch = new ArrayList<UrlMatcher>();

        final String startsWith = xmlUrl.getAttributeValue("starts");
        if (!StringUtil.isEmptyOrSpaces(startsWith)) {
          childMatch.add(new StartsWithMatcher(startsWith.trim()));
        }

        final String equals = xmlUrl.getAttributeValue("equals");
        if (!StringUtil.isEmptyOrSpaces(equals)) {
          childMatch.add(new EqualsMatcher(equals.trim()));
        }

        if (childMatch.isEmpty()) {
          throw new ConfigurationException("No url matching rules found: " + xmlUrl);
        }

        matchers.add(new AndMatcher(childMatch));
      }

      result.add(new Rule(new OrMatcher(matchers), place, content));
    }
    return result;
  }
}
