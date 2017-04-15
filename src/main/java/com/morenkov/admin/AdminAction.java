package com.morenkov.admin;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.morenkov.rest.AdminPageResource;
import com.morenkov.service.AdminConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action for admin page.
 */
@WebSudoRequired
public class AdminAction extends JiraWebActionSupport {
    private static final Logger log = LoggerFactory.getLogger(AdminPageResource.class);

    private final AdminConfigService adminConfigService;

    private String apiUrl = "asd";
    private String apiKey;
    private String apiSecret;

    @Autowired
    public AdminAction(AdminConfigService adminConfigService) {
        this.adminConfigService = adminConfigService;
        apiUrl = adminConfigService.getProperty("apiUrl");
        apiKey = adminConfigService.getProperty("apiKey");
        apiSecret = adminConfigService.getProperty("apiSecret");
    }

    @Override
    protected String doExecute() throws Exception {
        return super.doExecute();
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiKey(String apiKey) {
        adminConfigService.setProperty("apiKey", apiKey);
        this.apiKey = apiKey;
    }

    public void setApiUrl(String apiUrl) {
        adminConfigService.setProperty("apiUrl", apiUrl);
        this.apiUrl = apiUrl;
    }

    public void setApiSecret(String apiSecret) {
        adminConfigService.setProperty("apiSecret", apiSecret);
        this.apiSecret = apiSecret;
    }
}
