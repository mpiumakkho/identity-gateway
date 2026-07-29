package com.identitygateway.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void keepsValidIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/health");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).isEqualTo("request-123");
        verify(chain).doFilter(request, response);
    }

    @Test
    void createsRequestIdWhenIncomingValueIsMissingOrInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/health");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "invalid request id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .isNotBlank()
                .isNotEqualTo("invalid request id");
        verify(chain).doFilter(request, response);
    }
}