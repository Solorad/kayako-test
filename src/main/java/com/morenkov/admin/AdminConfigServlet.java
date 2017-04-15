package com.morenkov.admin;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.velocity.DefaultVelocityRequestContextFactory;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.morenkov.service.AdminConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet for generating admin page.
 *
 * @author emorenkov
 */
@Component
public class AdminConfigServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(AdminConfigServlet.class);

    private final AdminConfigService adminConfigService;
    private final ApplicationProperties jiraAppProperties;
    private final TemplateRenderer templateRenderer;
    private final JiraAuthenticationContext authContext;
    private final GlobalPermissionManager permissionManager;

    @Autowired
    public AdminConfigServlet(AdminConfigService adminConfigService,
                              @ComponentImport ApplicationProperties jiraAppProperties,
                              @ComponentImport TemplateRenderer templateRenderer,
                              @ComponentImport JiraAuthenticationContext authContext,
                              @ComponentImport GlobalPermissionManager permissionManager) {
        this.adminConfigService = adminConfigService;
        this.jiraAppProperties = jiraAppProperties;
        this.templateRenderer = templateRenderer;
        this.authContext = authContext;
        this.permissionManager = permissionManager;
    }

    /**
     * Servlet response for admin page.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (isAuthenticated(request, response)) {
            Map<String, Object> params = new HashMap<>();
            response.setContentType("text/html");
            params.put("apiUrl", adminConfigService.getProperty("apiUrl"));
            params.put("apiKey", adminConfigService.getProperty("apiKey"));
            params.put("apiSecret", adminConfigService.getProperty("apiSecret"));

            DefaultVelocityRequestContextFactory contextFactory =
                    new DefaultVelocityRequestContextFactory(jiraAppProperties);

            templateRenderer.render(getVelocityTemplate(), contextFactory.getDefaultVelocityParams(params, authContext),
                    response.getWriter());
        }
    }

    /**
     * POST method
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("doPost was started");
        String apiUrl = req.getParameter("apiUrl");
        String apiKey = req.getParameter("apiKey");
        String apiSecret = req.getParameter("apiSecret");
        log.debug("apiUrl = '{}'\napiKey='{}'\napiSecret='{}'",
                new Object[]{apiUrl, apiKey, apiSecret});

        adminConfigService.setProperty("apiUrl", apiUrl);
        adminConfigService.setProperty("apiKey", apiKey);
        adminConfigService.setProperty("apiSecret", apiSecret);

        doGet(req, resp);

        log.debug("Settings were set.");
    }

    /**
     * if user is not an admin -> redirects to login page
     */
    private boolean isAuthenticated(HttpServletRequest request, HttpServletResponse response) {
        ApplicationUser user = authContext.getLoggedInUser();
        if (user == null || !permissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user)) {
            final String encoding = jiraAppProperties.getEncoding();
            try {
                final String queryString = request.getQueryString();
                final String pathInfo = request.getPathInfo();
                final String destination = request.getServletPath() +
                        ((null != pathInfo) ? pathInfo : "") +
                        ((null != queryString) ? "?" + queryString : "");

                response.sendRedirect(request.getContextPath() + "/secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=" + URLEncoder
                        .encode(destination, encoding));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to redirect to /authenticate.action");
            }
            return false;
        }
        return true;
    }

    private String getVelocityTemplate() {
        return "templates/com/kayako/admin-page.vm";
    }
}
