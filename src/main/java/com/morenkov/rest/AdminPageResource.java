package com.morenkov.rest;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.morenkov.service.AdminConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("/config")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Component
public class AdminPageResource {

    private static final Logger log = LoggerFactory.getLogger(AdminPageResource.class);

    private final AdminConfigService adminConfigService;
    private final I18nResolver i18n;

    @Autowired
    public AdminPageResource(AdminConfigService adminConfigService,
                             @ComponentImport I18nResolver i18n) {
        this.adminConfigService = adminConfigService;
        this.i18n = i18n;
    }

    @POST
    public Response createPropertyConfig(@FormParam("apiUrl") String apiUrl,
                                         @FormParam("apiKey") String apiKey,
                                         @FormParam("apiSecret") String apiSecret) {
        log.debug("createPropertyConfig for url='{}', key='{}', secret='{}' has started.", apiUrl, apiKey, apiSecret);

        adminConfigService.setProperty("apiUrl", apiUrl);
        adminConfigService.setProperty("apiKey", apiKey);
        adminConfigService.setProperty("apiSecret", apiSecret);
        return Response.ok().build();
    }
}