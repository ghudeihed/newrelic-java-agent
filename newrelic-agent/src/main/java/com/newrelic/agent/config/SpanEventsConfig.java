/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

public class SpanEventsConfig extends BaseConfig {

    public static final int DEFAULT_MAX_SPAN_EVENTS_PER_HARVEST = 1000;
    public static final int DEFAULT_TARGET_SAMPLES_STORED = 10;
    public static final boolean DEFAULT_CROSS_PROCESS_ONLY = false;

    public static final String COLLECT_SPAN_EVENTS = "collect_span_events";
    private static final String ROOT = "newrelic.config.";
    private static final String SPAN_EVENTS = "span_events.";
    private static final String SYSTEM_PROPERTY_ROOT = ROOT + SPAN_EVENTS;
    public static final String ENABLED = "enabled";
    private static final String TARGET_SAMPLES_STORED = "target_samples_stored";
    private static final String CROSS_PROCESS_ONLY = "cross_process_only";
    private static final boolean DEFAULT_COLLECT_SPANS = false;

    // Span event system properties with root
    public static final String SYSTEM_PROPERTY_SPAN_EVENTS_ENABLED = SYSTEM_PROPERTY_ROOT + ENABLED;
    public static final String SPAN_EVENTS_ENABLED = SPAN_EVENTS + ENABLED;
    // EnvironmentFacade variables
    public static final String ENABLED_ENV_KEY = "NEW_RELIC_SPAN_EVENTS_ENABLED";

    private final boolean dtEnabled;
    private final int maxSamplesStored;
    private final boolean enabled;
    private final int targetSamplesStored;
    private final boolean crossProcessOnly;

    public SpanEventsConfig(Map<String, Object> props, boolean dtEnabled) {
        super(props, SYSTEM_PROPERTY_ROOT);
        this.dtEnabled = dtEnabled;
        this.maxSamplesStored = DEFAULT_MAX_SPAN_EVENTS_PER_HARVEST;
        this.enabled = initEnabled(maxSamplesStored);
        this.targetSamplesStored = getProperty(TARGET_SAMPLES_STORED, DEFAULT_TARGET_SAMPLES_STORED);
        this.crossProcessOnly = getProperty(CROSS_PROCESS_ONLY, DEFAULT_CROSS_PROCESS_ONLY);
    }

    private boolean initEnabled(int maxSamplesStored) {
        Boolean configEnabled = getProperty(ENABLED, dtEnabled) && dtEnabled;
        boolean collectSpanEventsFromCollector = getProperty(COLLECT_SPAN_EVENTS, DEFAULT_COLLECT_SPANS);
        return maxSamplesStored > 0 && configEnabled && collectSpanEventsFromCollector;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxSamplesStored() {
        return maxSamplesStored;
    }

    public int getTargetSamplesStored() {
        return targetSamplesStored;
    }

    public boolean isCrossProcessOnly() {
        return crossProcessOnly;
    }

}
