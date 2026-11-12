package com.matriz;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Keeps implicit framework methods from extending the API contract, after authentication (ADR-004). */
@Configuration(proxyBeanMethods = false)
class ApiMethodsConfiguration implements WebMvcConfigurer {

	private final ObjectProvider<RequestMappingHandlerMapping> mappings;

	ApiMethodsConfiguration(
			@Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> mappings) {
		this.mappings = mappings;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new HandlerInterceptor() {
			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
					throws Exception {
				if (!Set.of("HEAD", "OPTIONS").contains(request.getMethod())) {
					return true;
				}
				Set<String> allowed = new LinkedHashSet<>();
				ApiMethodsConfiguration.this.mappings.getObject().getHandlerMethods().keySet().forEach((mapping) -> {
					var paths = mapping.getPathPatternsCondition();
					if (paths != null && paths.getMatchingCondition(request) != null) {
						mapping.getMethodsCondition().getMethods().stream().map(RequestMethod::name).forEach(allowed::add);
					}
				});
				if (!allowed.isEmpty() && !allowed.contains(request.getMethod())) {
					throw new HttpRequestMethodNotSupportedException(request.getMethod(), allowed);
				}
				return true;
			}
		}).addPathPatterns("/api/**");
	}

}
