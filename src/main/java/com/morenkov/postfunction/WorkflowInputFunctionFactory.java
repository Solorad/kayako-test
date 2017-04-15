package com.morenkov.postfunction;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgenii Morenkov
 */
public class WorkflowInputFunctionFactory  extends AbstractWorkflowPluginFactory
        implements WorkflowPluginFunctionFactory {
    private static final Logger log = LoggerFactory.getLogger(WorkflowInputFunctionFactory.class);

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> map) {

    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> map, AbstractDescriptor abstractDescriptor) {

    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> map, AbstractDescriptor abstractDescriptor) {

    }

    @Override
    public Map<String, ?> getDescriptorParams(Map<String, Object> map) {
        return new HashMap<>();
    }
}
