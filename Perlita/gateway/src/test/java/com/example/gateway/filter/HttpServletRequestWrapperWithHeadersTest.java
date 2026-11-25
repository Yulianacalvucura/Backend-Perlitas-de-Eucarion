package com.example.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HttpServletRequestWrapperWithHeadersTest {

    @Test
    void shouldDiscardClientSentIdentityHeadersAndUseGatewayValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Username", "hacker");
        request.addHeader("X-User-Roles", "ADMIN");
        request.addHeader("x-user-roles", "ADMIN"); // case variation

        HttpServletRequestWrapperWithHeaders wrapper = new HttpServletRequestWrapperWithHeaders(request);
        wrapper.addHeader("X-User-Username", "usuario-real");
        wrapper.addHeader("X-User-Roles", "VENDEDOR");

        assertEquals("usuario-real", wrapper.getHeader("X-User-Username"));
        assertEquals("VENDEDOR", wrapper.getHeader("X-User-Roles"));
        assertEquals("VENDEDOR", wrapper.getHeader("x-user-roles"));

        List<String> roles = Collections.list(wrapper.getHeaders("X-User-Roles"));
        assertEquals(1, roles.size());
        assertEquals("VENDEDOR", roles.get(0));

        List<String> names = Collections.list(wrapper.getHeaderNames());
        long usernameHeaders = names.stream().filter(n -> n.equalsIgnoreCase("X-User-Username")).count();
        long roleHeaders = names.stream().filter(n -> n.equalsIgnoreCase("X-User-Roles")).count();
        assertEquals(1, usernameHeaders);
        assertEquals(1, roleHeaders);
        assertFalse(names.stream().anyMatch("hacker"::equals));
    }
}
