package com.tramchester.deployment;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

import static com.tramchester.deployment.ServerCdkStack.getEnvOrStop;

public class CdkApp {

    private static final String CFN_PROJECT = "tramchesterB";

    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder().
                account(System.getenv("CDK_DEFAULT_ACCOUNT")).
                region(System.getenv("CDK_DEFAULT_REGION")).build();

        StackProps stackProps = StackProps.builder().env(env).build();

        CloudFormationExistingResources existingResources = new CloudFormationExistingResources(CloudFormationClient.create());

        String cfnEnv = getEnvOrStop("ENV");

        createServerStack(app, stackProps, existingResources, cfnEnv);

        new InfraCdkStack(app, "tramchesterInfra", stackProps, CFN_PROJECT, cfnEnv);

        app.synth();
    }

    private static void createServerStack(App app, StackProps stackProps, CloudFormationExistingResources existingResources, String cfnEnv) {
        String releaseNumber = getEnvOrStop("RELEASE_NUMBER");
        String stackId = String.format("%s%s%sservers", CFN_PROJECT, releaseNumber, cfnEnv);
        new ServerCdkStack(app, stackId, stackProps, existingResources, CFN_PROJECT, cfnEnv, releaseNumber);
    }
}
