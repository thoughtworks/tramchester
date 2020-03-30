package com.tramchester.deployment;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.s3.Bucket;

public class CdkStack extends Stack {
    public CdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Bucket.Builder.create(this, "CDKTestBucket")
            .versioned(true).build();
    }
}
