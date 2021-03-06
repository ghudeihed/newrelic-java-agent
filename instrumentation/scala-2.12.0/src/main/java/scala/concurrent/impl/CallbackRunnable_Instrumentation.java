/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package scala.concurrent.impl;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.scala.ScalaUtils;
import com.nr.agent.instrumentation.scala.WrappedTry;
import scala.Function1;
import scala.util.Try;

import java.util.concurrent.atomic.AtomicInteger;

import static com.nr.agent.instrumentation.scala.ScalaUtils.scalaFuturesAsSegments;

@Weave(originalName = "scala.concurrent.impl.CallbackRunnable")
public class CallbackRunnable_Instrumentation<T> {
    private final Function1<Try<?>, Object> onComplete = Weaver.callOriginal();

    private Try<T> value = Weaver.callOriginal();

    /**
     * Override the setter for "value" so we can replace it with our wrapped version
     */
    public void value_$eq(Try<T> original) {
        Weaver.callOriginal();

        AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
        if (tokenAndRefCount == null) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null) {
                AgentBridge.TokenAndRefCount tokenAndRef = new AgentBridge.TokenAndRefCount(tx.getToken(),
                        AgentBridge.getAgent().getTracedMethod(), new AtomicInteger(1));
                value = new WrappedTry<>(value, tokenAndRef);
            }
        } else {
            value = new WrappedTry<>(value, tokenAndRefCount);
            tokenAndRefCount.refCount.incrementAndGet();
        }
    }

    @Trace(excludeFromTransactionTrace = true)
    public void run() {
        Segment segment = null;
        WrappedTry wrapped = null;
        boolean remove = false;

        if (value instanceof WrappedTry) {
            wrapped = (WrappedTry) value;

            // If we are here and there is no activeToken in progress we are the first one so we set this boolean in
            // order to correctly remove the "activeToken" from the thread local after the original run() method executes
            remove = AgentBridge.activeToken.get() == null;
            AgentBridge.activeToken.set(wrapped.tokenAndRefCount);
            if (scalaFuturesAsSegments && remove) {
                Transaction tx = AgentBridge.getAgent().getTransaction(false);
                if (tx != null) {
                    segment = tx.startSegment("Scala", "Callback");
                    segment.setMetricName("Scala", "Callback", ScalaUtils.nameScalaFunction(onComplete.getClass().getName()));
                }
            }
            value = wrapped.original;
        }

        try {
            Weaver.callOriginal();
        } finally {
            if (wrapped != null) {
                if (segment != null) {
                    segment.end();
                }

                if (remove) {
                    AgentBridge.activeToken.remove();
                }

                if (wrapped.tokenAndRefCount.refCount.decrementAndGet() == 0) {
                    wrapped.tokenAndRefCount.token.expire();
                    wrapped.tokenAndRefCount.token = null;
                }
            }
        }
    }

}
