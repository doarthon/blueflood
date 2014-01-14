package com.rackspacecloud.blueflood.inputs.handlers;

import com.google.gson.Gson;
import com.google.gson.internal.LazilyParsedNumber;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.rackspacecloud.blueflood.inputs.handlers.wrappers.Bundle;
import com.rackspacecloud.blueflood.io.NumericSerializer;
import com.rackspacecloud.blueflood.types.IMetric;
import com.rackspacecloud.blueflood.types.PreaggregatedMetric;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

public class StatsdHttpIngestionTests {
    
    private Bundle bundle;
    
    @Before
    public void buildBundle() throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("src/test/resources/sample_bundle.json")));
        String curLine = reader.readLine();
        while (curLine != null) {
            sb = sb.append(curLine);
            curLine = reader.readLine();
        }
        String json = sb.toString();
        bundle = HttpStatsDIngestionHandler.createBundle(json);
    }
    
    @Test(expected = NumberFormatException.class)
    public void testExpectedGsonConversionFailure() {
        new LazilyParsedNumber("2.321").longValue();
    }
    
    @Test
    public void testGsonNumberConversions() {
        Number doubleNum = new LazilyParsedNumber("2.321");
        Assert.assertEquals(Double.parseDouble("2.321"), HttpStatsDIngestionHandler.resolveNumber(doubleNum));
        
        Number longNum = new LazilyParsedNumber("12345");
        Assert.assertEquals(Long.parseLong("12345"), HttpStatsDIngestionHandler.resolveNumber(longNum));
    }
    
    @Test
    public void testCounters() {
        Collection<PreaggregatedMetric> counters = HttpStatsDIngestionHandler.convertCounters("1", 1, bundle.getCounters());
        Assert.assertEquals(6, counters.size());
        ensureSerializability(counters);
    }
    
    @Test
    public void testGauges() {
        Collection<PreaggregatedMetric> gauges = HttpStatsDIngestionHandler.convertGauges("1", 1, bundle.getGauges());
        Assert.assertEquals(4, gauges.size());
        ensureSerializability(gauges);
    }
     
    @Test
    public void testSets() {
        Collection<PreaggregatedMetric> sets = HttpStatsDIngestionHandler.convertSets("1", 1, bundle.getSets());
        Assert.assertEquals(2, sets.size());
        ensureSerializability(sets);
    }
    
    @Test
    public void testTimers() {
        Collection<PreaggregatedMetric> timers = HttpStatsDIngestionHandler.convertTimers("1", 1, bundle.getTimers());
        Assert.assertEquals(4, timers.size());
        ensureSerializability(timers);
    }
    
    // ok. while we're out it, let's test serialization. Just for fun. The reasoning is that these metrics
    // follow a different creation path that what we currently have in tests.
    private static void ensureSerializability(Collection<PreaggregatedMetric> metrics) {
        for (PreaggregatedMetric metric : metrics) {
            AbstractSerializer serializer = NumericSerializer.serializerFor(metric.getValue().getClass());
            serializer.toByteBuffer(metric.getValue());
        }
    }
}
