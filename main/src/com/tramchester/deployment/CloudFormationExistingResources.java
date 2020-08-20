package com.tramchester.deployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;

public class CloudFormationExistingResources {
    private static final Logger logger = LoggerFactory.getLogger(CloudFormationExistingResources.class);

    private final CloudFormationClient client;

    public CloudFormationExistingResources(CloudFormationClient client) {
        this.client = client;
    }

    public String findPhysicalId(String stackName, String logicialId) {
        // check the stack exists first, so that specific error message is logged
        DescribeStacksRequest stackRequest = DescribeStacksRequest.builder().stackName(stackName).build();
        DescribeStacksResponse results = client.describeStacks(stackRequest);

        if (!results.hasStacks()) {
            logger.error("No stack with name '" + stackName + "' was found");
            return "";
        }

        if (results.stacks().size()>1) {
            logger.warn("Ambiguous stackname, found multiple instances: " + results.stacks().size());
        }

        Stack found = results.stacks().get(0);

        return findPhysicalId(found, logicialId);
    }

    private String findPhysicalId(Stack stack, String logicialId) {
        DescribeStackResourcesRequest request = DescribeStackResourcesRequest.builder().
                logicalResourceId(logicialId).stackName(stack.stackName()).build();
        DescribeStackResourcesResponse results = client.describeStackResources(request);

        if (!results.hasStackResources()) {
            logger.error("No resource with logicial id '"+logicialId+"' was found.");
        }

        if (results.stackResources().size()>1) {
            logger.warn("Ambiguous logicialId, found multiple instances: " + results.stackResources().size());
        }

        StackResource found = results.stackResources().get(0);

        return found.physicalResourceId();
    }
}
