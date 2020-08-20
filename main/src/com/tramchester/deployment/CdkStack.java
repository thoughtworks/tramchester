package com.tramchester.deployment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.Role;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CdkStack extends Stack {
    private static final Logger logger = LoggerFactory.getLogger(CdkStack.class);
    private final CloudFormationExistingResources existingResources;
    private final String cfnProject;
    private final String cfnEnv;
    private final String releaseNumber;

    public CdkStack(final Construct parentScope, final String id, final StackProps props, CloudFormationExistingResources existingResources) {
        super(parentScope, id, props);
        this.existingResources = existingResources;
        CdkStack scope;
        scope = this;

        cfnEnv = getEnvOrStop("CFN_ASSIST_ENV");
        cfnProject = getEnvOrStop("CFN_ASSIST_PROJECT");
        releaseNumber = getEnvOrStop("RELEASE_NUMBER");

        createStack(scope);
    }

    private void createStack(CdkStack scope) {
        ISubnet webSubnetA = getSubnet();

        Tags.of(scope).add("CFN_ASSIST_BUILD_NUMBER", releaseNumber);
        Tags.of(scope).add("CFN_ASSIST_ENV", cfnEnv);
        Tags.of(scope).add("CFN_ASSIST_PROJECT", cfnProject);

        @NotNull VpcLookupOptions lookupOptions = new VpcLookupOptions() {
            @Override
            public @Nullable Map<String, String> getTags() {
                HashMap<String, String> tags = new HashMap<>();
                tags.put("CFN_ASSIST_ENV", cfnEnv);
                tags.put("CFN_ASSIST_PROJECT",cfnProject);
                return tags;
            }
        };

        IVpc vpc = Vpc.fromLookup(this, "existingVPC", lookupOptions);
        logger.info("Found VPC " + vpc.getVpcId());

        // TODO find from existing stack
        IRole role = Role.fromRoleArn(scope, "role", "arn:aws:iam::300752856189:role/tramchester/ProdGreen/tramchesterBProdGreen010createInstanc-InstanceRole-1TXB1XQFBGP6");

        CfnWaitConditionHandle waitConditionHandle = new CfnWaitConditionHandle(scope, "webDoneWaitHandle");

        @Nullable CfnWaitConditionProps waitProps = CfnWaitConditionProps.builder().
                handle(waitConditionHandle.getRef()).count(1).timeout("1200").build();

        CfnWaitCondition waitCondition = new CfnWaitCondition(scope, "waitForWebServerCallBack", waitProps);

        UserData userData = UserData.custom(createUserData(waitConditionHandle.getRef(), "tramchester2dist"));

        @Nullable AmazonLinuxImageProps props = AmazonLinuxImageProps.builder().
                generation(AmazonLinuxGeneration.AMAZON_LINUX_2).build();
        IMachineImage machineImage = MachineImage.latestAmazonLinux(props);
        List<String> awsCdkWorkaround = Collections.singletonList("workaround");
        List<ISubnet> subnets = Collections.singletonList(webSubnetA);
        SubnetSelection subnetSelection = SubnetSelection.builder().subnets(subnets).availabilityZones(awsCdkWorkaround).build();

        ISecurityGroup securityGroup = getSecurityGroup(scope);

        InstanceProps instanceProps = InstanceProps.builder().
                vpc(vpc).
                keyName(getKeyName()).
                instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO)).
                machineImage(machineImage).
                role(role).
                userData(userData).
                vpcSubnets(subnetSelection).
                securityGroup(securityGroup).
                build();

        Instance instance = new Instance(scope, "TramWebServerA", instanceProps);
        Tags.of(instance).add("Name", "tramchesterWebA");
        Tags.of(instance).add("CFN_ASSIST_TYPE", "web");
    }

    @NotNull
    private String getKeyName() {
        return cfnProject+"_"+cfnEnv;
    }

    @NotNull
    private ISecurityGroup getSecurityGroup(CdkStack scope) {
        String securityGroupdId = getPhysicalId("003webSubnetAclAndSG", "sgWeb");
        ISecurityGroup securityGroup = SecurityGroup.fromSecurityGroupId(scope, "webSg", securityGroupdId);
        return securityGroup;
    }

    @NotNull
    private ISubnet getSubnet() {
        String subnetId = getPhysicalId("001subnets", "webSubnetZoneA");

        // workaround bug in Subnet.fromSubnetId https://github.com/aws/aws-cdk/issues/8301
        @NotNull SubnetAttributes attributes = SubnetAttributes.builder().
                subnetId(subnetId).
                availabilityZone("eu-west-1a").build();
        ISubnet webSubnetA = Subnet.fromSubnetAttributes(this, "webSubnetA", attributes);
        logger.info("Found subnets " + webSubnetA.getSubnetId() + " with AZ " + webSubnetA.getAvailabilityZone());
        return webSubnetA;
    }

    private String getPhysicalId(String shortName, String logicalId) {
        return existingResources.findPhysicalId(formFullStackName(shortName), logicalId);
    }

    private String formFullStackName(String shortName) {
        return cfnProject+cfnEnv+shortName;
    }

    private String createUserData(@NotNull String webDoneWaitHandle, String bucketName) {
        String baseUrl = "https://s3-eu-west-1.amazonaws.com";
        return "#include"+"\n"+
                baseUrl + "/" + bucketName +  "/" + releaseNumber + "/cloudInitAWSLinux.txt" + "\n" +
                baseUrl + "/" + bucketName +  "/" + releaseNumber + "/setupTramWebServerAWSLinux.sh" + "\n" +
                "# WAITURL=" + webDoneWaitHandle + "\n" +
                "# ENV=" + cfnEnv + "\n" +
                "# ARTIFACTSURL="  + baseUrl + "/" + bucketName + "\n" +
                "# BUILD=" + releaseNumber + "\n" +
                "# TFGMAPIKEY=" + getEnvOrStop("TFGMAPIKEY") + "\n";
    }

    private String getEnvOrStop(String name) {
        String result = System.getenv(name);
        if (result==null || result.isEmpty()) {
            logger.error("Missing mandatory env var " + name);
            System.exit(-1);
        }
        return result;
    }

}
