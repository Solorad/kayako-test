package com.luxoft.luxproject.integration.admin;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;

/**
 * Application properties action, really here just for WebSudo and permissions check
 *
 * @since v4.4
 */
@WebSudoRequired
public class IntegrationAdmin extends JiraWebActionSupport {

    @Override
    protected String doExecute() throws Exception {
        return super.doExecute();
    }
}
