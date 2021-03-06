package org.nutz.mvc;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nutz.lang.Strings;
import org.nutz.mvc.config.FilterNutConfig;

/**
 * 同 JSP/Serlvet 容器的挂接点
 * 
 * @author zozoh(zozohtnt@gmail.com)
 * @author juqkai(juqkai@gmail.com)
 * @author wendal(wendal1985@gmail.com)
 */
public class NutFilter implements Filter {

	private ActionHandler handler;

	private static final String IGNORE = "^.+\\.(jsp|png|gif|jpg|js|css|jspx|jpeg|swf)$";

	private Pattern ignorePtn;

	private boolean skipMode;
	
	private String selfName;

	public void init(FilterConfig conf) throws ServletException {
		Mvcs.setServletContext(conf.getServletContext());
		this.selfName = conf.getFilterName();
		Mvcs.set(selfName, null, null);
		
		FilterNutConfig config = new FilterNutConfig(conf);
		Mvcs.setNutConfig(config);
		// 如果仅仅是用来更新 Message 字符串的，不加载 Nutz.Mvc 设定
		// @see Issue 301
		String skipMode = Strings.sNull(conf.getInitParameter("skip-mode"), "false").toLowerCase();
		if (!"true".equals(skipMode)) {
			handler = new ActionHandler(config);
			String regx = Strings.sNull(config.getInitParameter("ignore"), IGNORE);
			if (!"null".equalsIgnoreCase(regx)) {
				ignorePtn = Pattern.compile(regx, Pattern.CASE_INSENSITIVE);
			}
		} else
			this.skipMode = true;
	}

	public void destroy() {
		Mvcs.resetALL();
		Mvcs.set(selfName, null, null);
		if(handler !=null)
			handler.depose();
		Mvcs.setServletContext(null);
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		Mvcs.resetALL();
		try {
			Mvcs.set(this.selfName, (HttpServletRequest) req, (HttpServletResponse) resp);
			if (!skipMode) {
				RequestPath path = Mvcs.getRequestPathObject((HttpServletRequest) req);
				if (null == ignorePtn || !ignorePtn.matcher(path.getUrl()).find()) {
					if (handler.handle((HttpServletRequest) req, (HttpServletResponse) resp))
						return;
				}
			}
			//更新 Request 必要的属性
			Mvcs.updateRequestAttributes((HttpServletRequest) req);
			// 本过滤器没有找到入口函数，继续其他的过滤器
			chain.doFilter(req, resp);
		} finally {
			Mvcs.resetALL();
		}
	}
}
