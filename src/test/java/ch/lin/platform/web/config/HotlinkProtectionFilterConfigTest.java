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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import ch.lin.platform.web.filter.HotlinkProtectionFilter;

class HotlinkProtectionFilterConfigTest {

    // Use WebApplicationContextRunner to simulate the startup of a Spring Boot Web environment
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(HotlinkProtectionFilterConfig.class);

    @Test
    void shouldRegisterFilter_WhenEnabledIsTrue() {
        contextRunner
                .withPropertyValues(
                        "platform.security.hotlink.enabled=true",
                        "platform.security.hotlink.allowed-origins=http://localhost:3000",
                        "platform.security.hotlink.strict-mode=true",
                        "platform.security.hotlink.url-patterns=/images/**,/videos/**"
                )
                .run(context -> {
                    // Verify that the Configuration class is successfully loaded
                    assertThat(context).hasSingleBean(HotlinkProtectionFilterConfig.class);
                    // Verify that the FilterRegistrationBean is created
                    assertThat(context).hasSingleBean(FilterRegistrationBean.class);

                    @SuppressWarnings("unchecked")
                    FilterRegistrationBean<HotlinkProtectionFilter> registrationBean
                            = context.getBean(FilterRegistrationBean.class);

                    // Verify that the registered Filter is indeed our HotlinkProtectionFilter
                    assertThat(registrationBean.getFilter()).isInstanceOf(HotlinkProtectionFilter.class);
                    // Verify that urlPatterns are correctly read from properties and set to the RegistrationBean
                    assertThat(registrationBean.getUrlPatterns()).containsExactlyInAnyOrder("/images/**", "/videos/**");
                    // Verify the execution priority
                    assertThat(registrationBean.getOrder()).isEqualTo(100);
                });
    }

    @Test
    void shouldNotRegisterFilter_WhenEnabledIsFalse() {
        contextRunner
                .withPropertyValues("platform.security.hotlink.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(HotlinkProtectionFilterConfig.class));
    }

    @Test
    void shouldNotRegisterFilter_WhenEnabledIsMissing() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(HotlinkProtectionFilterConfig.class));
    }
}
