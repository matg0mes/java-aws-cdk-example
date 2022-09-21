package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class Service01Stack extends Stack {
    public Service01Stack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);
        LogGroup logGroup = LogGroup.Builder
                .create(this, "Service01LogGroup")
                .logGroupName("Service01")
                .removalPolicy(RemovalPolicy.DESTROY).build();

        AwsLogDriverProps awsLogDriverProps = AwsLogDriverProps.builder()
                .logGroup(logGroup)
                .streamPrefix("Service01")
                .build();

        ApplicationLoadBalancedTaskImageOptions taskImageOptions = ApplicationLoadBalancedTaskImageOptions.builder()
                .containerName("curso_aws_project_01")
                .image(ContainerImage.fromRegistry("hotmus/curso_aws_project_01:1.1.0"))
                .containerPort(8080)
                .logDriver(LogDriver.awsLogs(awsLogDriverProps))
                .build();

        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.
                Builder.create(this, "ALB01")
                .serviceName("service-01")
                .cluster(cluster)
                .cpu(512)
                .desiredCount(2)
                .memoryLimitMiB(1024)
                .listenerPort(8080)
                .assignPublicIp(true)
                .taskImageOptions(taskImageOptions)
                .publicLoadBalancer(true)
                .build();


        HealthCheck healthCheck = new HealthCheck.Builder()
                .path("/actuator/health")
                .port("8080")
                .healthyHttpCodes("200")
                .build();

        service01.getTargetGroup().configureHealthCheck(healthCheck);

        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build());

        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());
    }
}
