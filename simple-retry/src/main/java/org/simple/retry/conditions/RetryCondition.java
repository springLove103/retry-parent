package org.simple.retry.conditions;


import org.simple.retry.RetryPolicyContext;

public interface RetryCondition {

    boolean meetState(RetryPolicyContext context);

    int escapeTime(RetryPolicyContext context);

}
