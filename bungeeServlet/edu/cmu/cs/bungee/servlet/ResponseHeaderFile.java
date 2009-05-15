package edu.cmu.cs.bungee.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class ResponseHeaderFile implements Filter {

	private FilterConfig config;

//	@Override
	public void destroy() {
		config = null;
	}

	@SuppressWarnings("unchecked")
//	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletResponse httpResp = (HttpServletResponse) resp;

		Enumeration<String> e = config.getInitParameterNames();

		while (e.hasMoreElements()) {
			String headerName = e.nextElement();
			String headerValue = config.getInitParameter(headerName);
			System.out.println("doFilter " + headerName + " " + headerValue);
			httpResp.addHeader(headerName, headerValue);
		}
		chain.doFilter(req, resp);
	}

//	@Override
	public void init(FilterConfig arg0) throws ServletException {
		config = arg0;
	}

}
