/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.servlet;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class AbstractHTTPServletTest extends Assert {

    private final HttpServletResponse httpResponse = EasyMock.createMock(HttpServletResponse.class);

    @Test
    public void testStaticResourcesIgnoreQueryWithRedirectQueryCheck() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("static-resources-list", ".*\\.html");
        params.put("redirect-query-check", "true");
        TestServlet servlet = createServlet(params);

        // "/services/?.html": the query string must not make "/" look like a static resource
        servlet.handleRequest(createRequest("/", ".html"), httpResponse);

        assertNull(servlet.staticPath);
        assertTrue(servlet.invoked);
    }

    @Test
    public void testStaticResourcesMatchPathInfo() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("static-resources-list", ".*\\.html");
        params.put("redirect-query-check", "true");
        TestServlet servlet = createServlet(params);

        servlet.handleRequest(createRequest("/index.html", "a=b"), httpResponse);

        assertEquals("/index.html", servlet.staticPath);
        assertFalse(servlet.invoked);
    }

    @Test
    public void testRedirectQueryCheckStillMatchesQuery() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("redirects-list", ".*\\.html");
        params.put("redirect-servlet-name", "default");
        params.put("redirect-query-check", "true");
        TestServlet servlet = createServlet(params);

        servlet.handleRequest(createRequest("/", ".html"), httpResponse);

        assertEquals("/", servlet.redirectPath);
        assertFalse(servlet.invoked);
    }

    private static TestServlet createServlet(Map<String, String> params) throws ServletException {
        ServletConfig config = EasyMock.createNiceMock(ServletConfig.class);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            EasyMock.expect(config.getInitParameter(entry.getKey())).andReturn(entry.getValue()).anyTimes();
        }
        EasyMock.replay(config);
        TestServlet servlet = new TestServlet();
        servlet.init(config);
        return servlet;
    }

    private static HttpServletRequest createRequest(String pathInfo, String queryString) {
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfo).anyTimes();
        EasyMock.expect(request.getQueryString()).andReturn(queryString).anyTimes();
        EasyMock.replay(request);
        return request;
    }

    private static class TestServlet extends AbstractHTTPServlet {
        private static final long serialVersionUID = 1L;

        private String staticPath;
        private String redirectPath;
        private boolean invoked;

        @Override
        protected Bus getBus() {
            return null;
        }

        @Override
        protected void invoke(HttpServletRequest request, HttpServletResponse response) {
            invoked = true;
        }

        @Override
        protected void serveStaticContent(HttpServletRequest request, HttpServletResponse response,
                                          String pathInfo) {
            staticPath = pathInfo;
        }

        @Override
        protected void redirect(HttpServletRequest request, HttpServletResponse response, String pathInfo) {
            redirectPath = pathInfo;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
            // not used as a filter in these tests
        }
    }
}
