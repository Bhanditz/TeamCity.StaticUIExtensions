/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.controllers.*;
import jetbrains.buildServer.staticUIExtensions.web.StaticPageContentController;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.springframework.mock.web.MockServletContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Constructor;

@Test
public class StaticPageContentControllerTest extends BaseControllerTestCase {

  private AuthorizationInterceptor auth;
  private WebControllerManager web;
  private ServletContext context;
  private long myRequestDate;


  @Override
  protected BaseController createController() {
    Mockery m = new Mockery();

    context = new MockServletContext();


    Configuration config = new Configuration() {
      @NotNull
      public File getConfigurationXml() {
        throw new UnsupportedOperationException();
      }

      @Nullable
      public File mapIncludeFilePath(@NotNull String path) {
        return null;
      }

      @NotNull
      public String getAccessToken() {
        throw new UnsupportedOperationException();
      }

      @NotNull
      public File getIncludeFilesBase() {
        return FileUtil.getCanonicalFile(new File("./tests/testData/"));
      }
    };
    web = m.mock(WebControllerManager.class);
    auth = m.mock(AuthorizationInterceptor.class);

    myRequest = new MockRequest() {
      public ServletContext getServletContext() {
        return context;
      }

      @Override
      public long getDateHeader(String string) {
        return myRequestDate;
      }
    };


    m.checking(new Expectations() {{
      allowing(web);
      allowing(auth);
    }});

    return new StaticPageContentController(auth, web, config, getHttpDownloadProcessor());
  }

  private HttpDownloadProcessor getHttpDownloadProcessor() {
    final Class<HttpDownloadProcessor> clazz = HttpDownloadProcessor.class;
    // TC 8.0: Constructor without parameters.
    try {
      final Constructor<HttpDownloadProcessor> constructor = clazz.getConstructor();
      return constructor.newInstance();
    } catch (Exception ignored) {
    }
    // TC 8.1 and probably newer versions: jetbrains.buildServer.artifacts.DigestCalculator required.
    try {
      final Class<?> dcc = HttpDownloadProcessor.class.getClassLoader().loadClass("jetbrains.buildServer.artifacts.DigestCalculator");
      final Constructor<HttpDownloadProcessor> constructor = clazz.getConstructor(dcc);

      final Class<?> sdcc = dcc.getClassLoader().loadClass("jetbrains.buildServer.artifacts.impl.SimpleDigestCalculator");
      final Object calculator = sdcc.newInstance();

      return constructor.newInstance(calculator);
    } catch (Exception ignored) {
    }
    // It's not good.
    fail("This tests works with TeamCity 8.0, 8.1. Please check api of your TeamCity and this tests sources.");
    return null;
  }

  @BeforeMethod
  public void cleanup() {
    myRequestDate = -1;
  }

  @Test
  public void testSimpleHtml() throws Exception {
    myRequest.setRequestURI("bs", "/app/static_content/w1/widget.html");
    doGet();
    assertNotNull(myResponse.getContentType());
    assertNotNull(myResponse.getCharacterEncoding());
    assertNotNull(myResponse.getHeaders("Content-Length"));
    assertNotNull(myResponse.getHeader("ETag"));
    assertEquals("text/html", myResponse.getContentType());
    assertContains(myResponse.getReturnedContent(), "Static page content (from w1/widget.html)");
  }

  @Test
  public void testJs() throws Exception {
    myRequest.setRequestURI("bs", "/app/static_content/w2/js/my.js");
    doGet();
    assertNotNull(myResponse.getContentType());
    assertNotNull(myResponse.getCharacterEncoding());
    assertFalse("text/html".equals(myResponse.getContentType()));
    String returnedContent = myResponse.getReturnedContent();
    assertTrue(returnedContent.contains("alert('test alert');"));
  }

  @Test
  public void testRequestWithParameters() throws Exception {
    myRequest.setRequestURI("bs", "/app/static_content/w1/widget.html?p1=1&p2=2");
    doGet();
    assertEquals("text/html", myResponse.getContentType());
    assertContains(myResponse.getReturnedContent(), "Static page content (from w1/widget.html)");
  }

  @Test
  public void testWrongFileRequest() throws Exception {
    myRequest.setRequestURI("bs", "/app/static_content/w1/widget1.html");
    doGet();
    assertEquals(404, myResponse.getStatus());
    assertTrue(StringUtil.isEmpty(myResponse.getReturnedContent()));
  }

  @Test
  public void testNotModifiedRequest() throws Exception {
    myRequest.setRequestURI("bs", "/app/static_content/w1/widget.html");
    myRequestDate = System.currentTimeMillis() - 10;
    doGet();
    assertEquals(304, myResponse.getStatus());
    assertTrue(StringUtil.isEmpty(myResponse.getReturnedContent()));
  }

  @Test
  public void testIllegalAccess() throws Exception {
    myRequest.setRequestURI("bs", "/app/static_content/../w1/widget.html");
    doGet();
    assertEquals(404, myResponse.getStatus());
    assertTrue(StringUtil.isEmpty(myResponse.getReturnedContent()));
  }
}
