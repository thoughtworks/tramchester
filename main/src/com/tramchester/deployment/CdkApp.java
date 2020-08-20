package com.tramchester.deployment;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

import java.io.IOException;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder().
                account(System.getenv("CDK_DEFAULT_ACCOUNT")).
                region(System.getenv("CDK_DEFAULT_REGION")).build();

        StackProps stackProps = StackProps.builder().env(env).build();

        CloudFormationExistingResources existingResources = new CloudFormationExistingResources(CloudFormationClient.create());

        new CdkStack(app, "TramchesterServers", stackProps, existingResources);

        app.synth();
    }
}
