/*=============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Che-Hung Lin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *===========================================================================*/
package ch.lin.platform.web.filter;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class HotlinkProtectionFilterTest {

    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        filterChain = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        request.setRequestURI("/thumbnails/test.jpg");
        response = new MockHttpServletResponse();
    }

    @Test
    void doFilter_ShouldAllow_WhenNoSourceAndStrictModeDisabled() throws IOException, ServletException {
        // Arrange: Strict mode is FALSE
        HotlinkProtectionFilter filter = new HotlinkProtectionFilter(List.of("http://localhost:3000"), false);

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void doFilter_ShouldBlock_WhenNoSourceAndStrictModeEnabled() throws IOException, ServletException {
        // Arrange: Strict mode is TRUE
        HotlinkProtectionFilter filter = new HotlinkProtectionFilter(List.of("http://localhost:3000"), true);
        // Simulate a real request with IP and User-Agent
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        request.setRemoteAddr("192.168.1.100");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"").contains("Hotlinking is not allowed");
    }

    @Test
    void doFilter_ShouldAllow_WhenOriginMatchesAllowedList() throws IOException, ServletException {
        // Arrange
        HotlinkProtectionFilter filter = new HotlinkProtectionFilter(List.of("http://localhost:3000", "https://example.com"), true);
        request.addHeader("Origin", "https://example.com");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldAllow_WhenRefererMatchesAllowedListWithWildcard() throws IOException, ServletException {
        // Arrange: Support basic wildcard matching like "https://*.example.com"
        HotlinkProtectionFilter filter = new HotlinkProtectionFilter(List.of("https://*.example.com"), true);
        // Not sending Origin, falling back to Referer
        request.addHeader("Referer", "https://sub.example.com/page");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldPreferOriginOverReferer() throws IOException, ServletException {
        // Arrange: Origin is allowed, Referer is NOT allowed
        HotlinkProtectionFilter filter = new HotlinkProtectionFilter(List.of("https://allowed.com"), true);
        request.addHeader("Origin", "https://allowed.com");
        request.addHeader("Referer", "https://blocked.com/page");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldBlock_WhenSourceDoesNotMatchAllowedList() throws IOException, ServletException {
        // Arrange
        HotlinkProtectionFilter filter = new HotlinkProtectionFilter(List.of("http://localhost:3000"), false);
        request.addHeader("Origin", "https://evil-site.com");
        request.addHeader("User-Agent", "Evil-Bot/1.0");
        // Simulate a request coming through a proxy (testing the extraction of the first IP)
        request.addHeader("X-Forwarded-For", "203.0.113.5, 198.51.100.1, 10.0.0.1");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"").contains("Hotlinking is not allowed");
    }

    @Test
    void doFilter_ShouldAllow_WhenAllowedOriginsIsAsterisk() throws IOException, ServletException {
        // Arrange: "*" allows any source
        HotlinkProtectionFilter filter = new HotlinkProtectionFilter(List.of("*"), true);
        request.addHeader("Origin", "https://random-site.com");

        // Act
        filter.doFilter(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }
}
