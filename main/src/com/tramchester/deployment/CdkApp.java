package com.tramchester.deployment;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

import static com.tramchester.deployment.CdkStack.getEnvOrStop;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder().
                account(System.getenv("CDK_DEFAULT_ACCOUNT")).
                region(System.getenv("CDK_DEFAULT_REGION")).build();

        StackProps stackProps = StackProps.builder().env(env).build();

        CloudFormationExistingResources existingResources = new CloudFormationExistingResources(CloudFormationClient.create());

        String cfnEnv = getEnvOrStop("ENV");
        String releaseNumber = getEnvOrStop("RELEASE_NUMBER");
        String cfnProject = "tramchesterB";

        String stackId = String.format("%s%s%sservers", cfnProject, releaseNumber, cfnEnv);


        new CdkStack(app, stackId, stackProps, existingResources, cfnProject, cfnEnv, releaseNumber);

        app.synth();
    }
}
