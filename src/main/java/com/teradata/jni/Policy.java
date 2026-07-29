package com.teradata.jni;

import java.util.Arrays;

public final class Policy {
    static final int MAX_SUB_POLICIES = 5;

    int sub_policy_num;
    SubPolicy[] sub_policy;

    public Policy() {
        this.sub_policy = new SubPolicy[0];
        this.sub_policy_num = 0;
    }

    private Policy(SubPolicy[] subPolicies) {
        this.sub_policy = subPolicies;
        this.sub_policy_num = subPolicies.length;
    }

    public static Policy of(SubPolicy... subPolicies) {
        if (subPolicies == null) {
            throw new IllegalArgumentException("subPolicies must not be null");
        }
        if (subPolicies.length > MAX_SUB_POLICIES) {
            throw new IllegalArgumentException("at most " + MAX_SUB_POLICIES + " sub policies are supported");
        }
        SubPolicy[] copy = Arrays.copyOf(subPolicies, subPolicies.length);
        for (SubPolicy subPolicy : copy) {
            if (subPolicy == null) {
                throw new IllegalArgumentException("sub policy must not be null");
            }
        }
        return new Policy(copy);
    }

    public SubPolicy[] getSubPolicies() {
        return Arrays.copyOf(sub_policy, sub_policy.length);
    }

    void validateForLength(int inputLength) {
        for (SubPolicy subPolicy : sub_policy) {
            if (subPolicy.position > inputLength ||
                    subPolicy.length > inputLength - subPolicy.position) {
                throw new IllegalArgumentException("sub policy exceeds input byte length: position="
                        + subPolicy.position + ", length=" + subPolicy.length
                        + ", inputLength=" + inputLength);
            }
        }
    }
}
