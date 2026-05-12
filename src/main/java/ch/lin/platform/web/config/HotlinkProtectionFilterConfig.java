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
package ch.lin.platform.web.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.lin.platform.web.filter.HotlinkProtectionFilter;

/**
 * Configuration class to register the HotlinkProtectionFilter.
 * <p>
 * This configuration is only activated if 'platform.security.hotlink.enabled'
 * is set to 'true'. It allows applications to specify their own url-patterns
 * for hotlink protection without altering the filter logic.
 */
@Configuration
@ConditionalOnProperty(prefix = "platform.security.hotlink", name = "enabled", havingValue = "true")
public class HotlinkProtectionFilterConfig {

    /**
     * A list of origin patterns that are permitted to access the resources.
     * Injected from the {@code platform.security.hotlink.allowed-origins}
     * property.
     */
    @Value("${platform.security.hotlink.allowed-origins:*}")
    private String[] allowedOrigins;

    /**
     * If true, requests without a Referer or Origin header will be blocked.
     * Injected from the {@code platform.security.hotlink.strict-mode} property.
     */
    @Value("${platform.security.hotlink.strict-mode:false}")
    private boolean strictMode;

    // Default interception path is /thumbnails/* (Servlet URL pattern), but provides flexibility for microservices to override
    @Value("${platform.security.hotlink.url-patterns:/thumbnails/*}")
    private String[] urlPatterns;

    /**
     * Creates and configures the {@link FilterRegistrationBean} for the
     * {@link HotlinkProtectionFilter}.
     * <p>
     * This bean definition reads configuration properties to instantiate the
     * filter and set its target URL patterns and execution order.
     *
     * @return A configured {@link FilterRegistrationBean} instance.
     */
    @Bean
    public FilterRegistrationBean<HotlinkProtectionFilter> hotlinkProtectionFilterRegistration() {
        FilterRegistrationBean<HotlinkProtectionFilter> registrationBean = new FilterRegistrationBean<>();

        // Manually pass parameters to the Filter
        HotlinkProtectionFilter filter = new HotlinkProtectionFilter(Arrays.asList(allowedOrigins), strictMode);

        // Register the Filter with the Spring Boot embedded Servlet Container
        registrationBean.setFilter(filter);

        // Specify which URLs this Filter should intercept
        registrationBean.setUrlPatterns(Arrays.asList(urlPatterns));
        registrationBean.setOrder(100); // Adjust the execution order if there are multiple Filters

        return registrationBean;
    }
}
