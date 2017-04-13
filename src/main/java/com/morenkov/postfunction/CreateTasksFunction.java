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
            boolean isConditionEnabled = Objects.equals("true", args.get(ENABLE_CONDITION));
            log.debug("isConditionEnabled = {}", isConditionEnabled);
            if (!checkCondition(args, parent, isConditionEnabled)) {
                return;
            }
            String issueLinkType = (String) args.get(ISSUE_LINK_TYPE);
            String standardFieldList = (String) args.get(INHERITED_STANDARD_FIELDS);

            Collection<StandardField> fieldsToCopy = parseStandardFields(standardFieldList);

            ApplicationUser caller = getCallerUser(transientVars, args);
            if (Objects.equals("none", args.get(ASSIGNEE))) {
                createUnassignedIssue(transientVars, args, parent, issueLinkType, fieldsToCopy, caller);
            } else {
                createMultipleIssues(transientVars, args, parent, issueLinkType,fieldsToCopy, caller);
            }
        } catch (Exception ex) {
            throw new WorkflowException("Exception during task creation", ex);
        }
    }


    /**
     * Create issue without any assignee.
     */
    private void createUnassignedIssue(Map transientVars, Map args, MutableIssue parent, String issueLinkType,
                                       Collection<StandardField> fieldsToCopy, ApplicationUser caller)
            throws WorkflowException, CreateException, IndexException {
        boolean useCommentAsDescription = useCommentAsDescription(parent);
        MutableIssue mutableIssue = createIssue(args, transientVars, parent, useCommentAsDescription, fieldsToCopy);
        createIssueWithAssignee(parent, issueLinkType, caller, mutableIssue, null);
    }

    /**
     * Legacy method that requires extra screen on transition 'Create tasks screen'.
     */
    private void createMultipleIssues(Map transientVars, Map args, MutableIssue parent, String issueLinkType,
                                      Collection<StandardField> fieldsToCopy, ApplicationUser caller)
            throws WorkflowException, CreateException, IndexException {
        Collection<String> usernames = getUsers(parent);
        if (usernames == null) {
            throw new WorkflowException(String.format("Error while executing Create Tasks function. Didn't you forget to set %s for this transition?",
                    SCREEN_NAME));
        }

        boolean useCommentAsDescription = useCommentAsDescription(parent);
        log.debug("usernames = '{}'", usernames);
        for (String assignee : usernames) {
            MutableIssue mutableIssue = createIssue(args, transientVars, parent, useCommentAsDescription, fieldsToCopy);
            ApplicationUser assigneeUser = userManager.getUserByName(assignee);
            createIssueWithAssignee(parent, issueLinkType, caller, mutableIssue, assigneeUser);
        }
    }

    private void createIssueWithAssignee(MutableIssue parent, String issueLinkType, ApplicationUser caller,
                                         MutableIssue mutableIssue, ApplicationUser assignee)
            throws CreateException, IndexException {
        Issue newIssue = issueManager.createIssueObject(caller, mutableIssue);

        if (newIssue.getIssueType() != null && newIssue.getIssueType().isSubTask()) {
            subtaskManager.createSubTaskIssueLink(parent, newIssue, caller);
        } else {
            addIssueLink(parent, issueLinkType, caller, newIssue);
        }
        mutableIssue.setAssignee(assignee);
        log.debug("we set assignee = '{}'", assignee);
        newIssue = issueManager.updateIssue(caller, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false);
        // if no IssueLinkManager#createIssueLink() was called - child issue may not be reindexed.
        if (StringUtils.isEmpty(issueLinkType)) {
            issueIndexingService.reIndex(newIssue);
        }
        log.debug("we have created new issue = '{}'", mutableIssue.getKey());
    }


    private MutableIssue createIssue(Map args, Map transientVars, MutableIssue parent,
                                     boolean useCommentAsDescription,
                                     Collection<StandardField> fieldsToCopy) throws WorkflowException, CreateException, IndexException {
        String summary = (String) args.get(SUMMARY);
        String issueTypeId = (String) args.get(ISSUE_TYPE);
        String reporter = (String) args.get(REPORTER);
        String customFieldList = (String) args.get(INHERITED_CUSTOM_FIELDS);
        validateNotEmpty(summary, issueTypeId, reporter);
        log.debug("customFieldList = '{}'", customFieldList);
        MutableIssue mutableIssue = createIssue(summary, parent, issueTypeId, "reporter".equals(reporter), fieldsToCopy);

        if (useCommentAsDescription) {
            mutableIssue.setDescription((String) transientVars.get(COMMENT_VARIABLE_NAME));
        }
        copyCustomFieldValues(parent, mutableIssue, getInheritedCustomFields(customFieldList));
        return mutableIssue;
    }

    private void addIssueLink(MutableIssue parent, String issueLinkType, ApplicationUser caller, Issue issue)
            throws CreateException {
        Long issueLinkTypeId = null;
        String direction = null;
        if (null != issueLinkType && !issueLinkType.isEmpty()) {
            String[] split = issueLinkType.split("-");
            issueLinkTypeId = Long.valueOf(split[0]);
            if (split.length > 1) {
                direction = split[1];
            }
        }
        if (issueLinkTypeId != null) {
            if ("inward".equals(direction)) {
                issueLinkManager.createIssueLink(parent.getId(), issue.getId(), issueLinkTypeId, null, caller);
            } else {
                issueLinkManager.createIssueLink(issue.getId(), parent.getId(), issueLinkTypeId, null, caller);
            }
        }
    }

    private boolean checkCondition(Map args, MutableIssue parent, boolean isConditionEnabled) {
        if (isConditionEnabled) {
            String condition = (String) args.get(CONDITION);
            Matcher isCustomFieldMatcher = customFieldPattern.matcher(condition);
            if (isCustomFieldMatcher.find()) {
                String cfId = condition.substring(1, condition.length() - 1);
                CustomField customField = customFieldManager.getCustomFieldObject(cfId);
                Object cfValue = parent.getCustomFieldValue(customField);
                log.debug("CustomField = {}, value = {}", customField, cfValue);
                Object conditionValue = args.get(CONDITION_VALUE);
                if (conditionValue == null && cfValue == null) {
                    log.debug("Condition and issue values are null");
                    return true;
                } else if (cfValue == null) {
                    log.debug("Parent issue custom field value is null");
                    return false;
                } else if (conditionValue == null) {
                    log.debug("Condition value is null when issue CF is not.");
                    return false;
                } else if (!Objects.equals(cfValue.toString(), conditionValue.toString())) {
                    log.debug("Values are not equals: '{}' and '{}'", cfValue.toString(), conditionValue.toString());
                    return false;
                }
            }
        }
        return true;
    }

    private void validateNotEmpty(String... args) throws WorkflowException {
        for (String arg : args) {
            if (arg == null || arg.isEmpty()) {
                throw new WorkflowException("One of the Create Tasks function parameters is not set");
            }
        }
    }

    private Collection<String> getUsers(Issue issue) {
        CustomField customField = customFieldManager.getCustomFieldObjectByName(USER_LIST_CUSTOM_FIELD_NAME);
        return (Collection<String>) issue.getCustomFieldValue(customField);
    }

    private Collection<CustomField> getInheritedCustomFields(String fieldList) {
        log.debug("Inherited fieldList = '{}'", fieldList);
        if (fieldList == null || fieldList.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        String[] customFieldIds = fieldList.split(",");
        List<CustomField> customFields = new ArrayList<>();
        for (String customFieldId : customFieldIds) {
            CustomField customFieldObject = customFieldManager.getCustomFieldObject(customFieldId);
            if (customFieldObject != null) {
                customFields.add(customFieldObject);
            }
        }
        log.debug("Inherited custom fields = {}", customFields);
        return customFields;
    }

    private MutableIssue createIssue(String summary, Issue parent, String issueTypeId,
                                     boolean isParentReporter, Collection<StandardField> fields)
            throws WorkflowException {
        MutableIssue issue = issueFactory.getIssue();
        issue.setIssueTypeId(issueTypeId);
        issue.setProjectObject(parent.getProjectObject());
        issue.setReporter(isParentReporter ? parent.getReporter() : parent.getAssignee());
        issue.setSummary(processSummary(summary, parent));
        IssueType issueType = issueTypeManager.getIssueType(issueTypeId);
        if (issueType.isSubTask()) {
            if (parent.getIssueType() != null && parent.getIssueType().isSubTask()) {
                throw new WorkflowException("Error while executing Create Tasks function. Cannot create subtask of subtask.");
            }
            issue.setParentObject(parent);
        }
        for (StandardField field : fields) {
            field.copyValue(parent, issue);
        }
        return issue;
    }

    private void copyCustomFieldValues(Issue src, MutableIssue dest, Collection<CustomField> customFields) {
        log.debug("Copying custom fields from parent task: '{}'", customFields);
        List<CustomField> availableCustomFields = customFieldManager.getCustomFieldObjects(dest);
        Set<String> availableCustomFieldIds = new HashSet<>();
        for (CustomField customField : availableCustomFields) {
            availableCustomFieldIds.add(customField.getId());
        }
        customFields.stream().filter(customField -> availableCustomFieldIds.contains(customField.getId()))
                .forEach(customField -> {
                    log.debug("Setting in child CF '{}'='{}'", customField, src.getCustomFieldValue(customField));
                    dest.setCustomFieldValue(customField, src.getCustomFieldValue(customField));
                });
    }

    private String processSummary(String summary, Issue parent) throws WorkflowException {
        Map<String, Object> params = ImmutableMap.of("parent", parent);
        return templateRenderer.renderFragment(summary, params);
    }

    private boolean useCommentAsDescription(Issue issue) {
        CustomField customField =
                customFieldManager.getCustomFieldObjectByName(COMMENT_CHECKBOX_CUSTOM_FIELD_NAME);
        return null != issue.getCustomFieldValue(customField);
    }

    private Collection<StandardField> parseStandardFields(String standardFieldString) {
        log.debug("Standard field list string: '{}'", standardFieldString);
        if (StringUtils.isEmpty(standardFieldString)) {
            return Collections.EMPTY_LIST;
        }
        List<StandardField> fields = new LinkedList<>();
        for (String fieldName : standardFieldString.split(",")) {
            fields.add(StandardField.valueOf(fieldName));
        }
        log.debug("Standard field list: '{}'", fields);
        return fields;
    }
}
