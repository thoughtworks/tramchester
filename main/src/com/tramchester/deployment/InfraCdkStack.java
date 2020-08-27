package com.tramchester.deployment;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.autoscaling.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfraCdkStack extends Stack {
    private static final Logger logger = LoggerFactory.getLogger(InfraCdkStack.class);
    private final String cfnProject;
    private final String cfnEnv;
    private final IVpc vpc;
    private static final String certARN = "arn:aws:acm:eu-west-1:300752856189:certificate/f88ff983-4e3e-421b-9ca1-2c35e848ae81";

    public InfraCdkStack(final Construct parentScope, final String id, final StackProps props,
                          String cfnProject, String cfnEnv) {
        super(parentScope, id, props);
        this.cfnProject = cfnProject;
        this.cfnEnv = cfnEnv;
        this.vpc = getVpc();

        createStack(this);
    }

    private void createStack(InfraCdkStack scope) {
        ApplicationLoadBalancer loadBalancer = new ApplicationLoadBalancer(this, "LB",
                ApplicationLoadBalancerProps.builder().vpc(vpc).internetFacing(true).build());

        HealthCheck healthCheck = HealthCheck.builder().
                healthyThresholdCount(2).
                interval(Duration.minutes(10)).
                timeout(Duration.minutes(5)).
                port("8080").protocol(Protocol.HTTP).
                build();

        // todo certificates
        List<IListenerCertificate> certs = Collections.singletonList(ListenerCertificate.fromArn(certARN));
        ApplicationListener httpsListener = loadBalancer.addListener("httpsListener",
                BaseApplicationListenerProps.builder().
                        port(443).certificates(certs).
                        open(true).build());
        ApplicationListener httpListener = loadBalancer.addListener("httpListener",
                BaseApplicationListenerProps.builder().port(80).open(true).build());

        AutoScalingGroup autoScalingGroup = AutoScalingGroup.Builder.create(this, "serverGroup").
                instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO)).
                maxCapacity(1).
                updateType(UpdateType.REPLACING_UPDATE).
                desiredCapacity(1).
                build();

        List<IApplicationLoadBalancerTarget> targets = Collections.singletonList(autoScalingGroup);

        httpsListener.addTargets("serversHttps",
                AddApplicationTargetsProps.builder().
                        port(8080).protocol(ApplicationProtocol.HTTP).healthCheck(healthCheck).
                        targets(targets).
                        build());

        httpListener.addTargets("serversHttp",
                AddApplicationTargetsProps.builder().
                        port(8080).protocol(ApplicationProtocol.HTTP).healthCheck(healthCheck).
                        targets(targets).
                        build());

    }


    @NotNull
    private IVpc getVpc() {
        @NotNull VpcLookupOptions lookupOptions = new VpcLookupOptions() {
            @Override
            public @NotNull Map<String, String> getTags() {
                HashMap<String, String> tags = new HashMap<>();
                tags.put("CFN_ASSIST_ENV", cfnEnv);
                tags.put("CFN_ASSIST_PROJECT",cfnProject);
                return tags;
            }
        };

        IVpc vpc = Vpc.fromLookup(this, "existingVPC", lookupOptions);
        logger.info("Found VPC " + vpc.getVpcId());
        return vpc;
    }
}
