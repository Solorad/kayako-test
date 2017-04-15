package com.morenkov.postfunction;

import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.index.IssueIndexingService;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Post function that create according with preferences task or subtask.
 * If no assignee was set, don't need for "Create subtasks" screen
 *
 */
public class CreateTasksFunction extends AbstractJiraFunctionProvider {
    private static final Logger log = LoggerFactory.getLogger(CreateTasksFunction.class);

    private final IssueFactory issueFactory;
    private final IssueManager issueManager;
    private final SubTaskManager subtaskManager;
    private final IssueLinkManager issueLinkManager;
    private final CustomFieldManager customFieldManager;
    private final IssueIndexingService issueIndexingService;
    private final UserManager userManager;
    private final IssueTypeManager issueTypeManager;
    private final TemplateRenderer templateRenderer;


    private static final Pattern customFieldPattern = Pattern.compile("^%customfield_[0-9]{1,7}%$");

    public CreateTasksFunction(IssueFactory issueFactory, IssueManager issueManager, SubTaskManager subtaskManager,
                               IssueLinkManager issueLinkManager, CustomFieldManager customFieldManager,
                               IssueIndexingService issueIndexingService, UserManager userManager,
                               IssueTypeManager issueTypeManager, TemplateRenderer templateRenderer) {
        this.issueFactory = issueFactory;
        this.issueManager = issueManager;
        this.subtaskManager = subtaskManager;
        this.issueLinkManager = issueLinkManager;
        this.customFieldManager = customFieldManager;
        this.issueIndexingService = issueIndexingService;
        this.userManager = userManager;
        this.issueTypeManager = issueTypeManager;
        this.templateRenderer = templateRenderer;
    }

    /**
     * We don't explicitly reindex new or parent issue due to
     * IssueLinkManager#createIssueLink() reindex parent and child issues.
     * This function is called explicitly for direct link or internally for subtasks
     * via SubtaskManager#createSubTaskIssueLink(parent, newIssue, caller) creation reindex parent and child issues
     */
    public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
        try {
            log.debug("args='{}'", args);
            MutableIssue parent = getIssue(transientVars);
//            if (!checkCondition(args, parent, isConditionEnabled)) {
//                return;
//            }
//            String issueLinkType = (String) args.get(ISSUE_LINK_TYPE);
//            String standardFieldList = (String) args.get(INHERITED_STANDARD_FIELDS);
//
//            Collection<StandardField> fieldsToCopy = parseStandardFields(standardFieldList);
//
//            ApplicationUser caller = getCallerUser(transientVars, args);
//            if (Objects.equals("none", args.get(ASSIGNEE))) {
//                createUnassignedIssue(transientVars, args, parent, issueLinkType, fieldsToCopy, caller);
//            } else {
//                createMultipleIssues(transientVars, args, parent, issueLinkType,fieldsToCopy, caller);
//            }
        } catch (Exception ex) {
            throw new WorkflowException("Exception during task creation", ex);
        }
    }
}
