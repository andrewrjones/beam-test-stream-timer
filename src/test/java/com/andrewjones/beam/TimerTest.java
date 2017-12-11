package com.andrewjones.beam;

import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.coders.VarIntCoder;
import org.apache.beam.sdk.state.*;
import org.apache.beam.sdk.testing.*;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TimerTest {
    @Rule
    public TestPipeline p = TestPipeline.create();

    @Test
    @Category({ValidatesRunner.class, UsesTimersInParDo.class, UsesTestStream.class})
    @SuppressWarnings("unchecked")
    public void testTimer() throws Exception {
        TestStream<KV<String, Integer>> createEvents = TestStream
                .create(KvCoder.of(StringUtf8Coder.of(), VarIntCoder.of()))
                // Test will hang if we add the elements separately, like this
                .addElements(KV.of("hello", 100))
                .addElements(KV.of("hello", 200))
                // This works fine
//                .addElements(KV.of("hello", 100), KV.of("hello", 200))
                .advanceWatermarkToInfinity();

        PCollection<Integer> output = p.apply(createEvents)
                .apply(ParDo.of(new WithTimers()));

        PAssert.that(output)
                .containsInAnyOrder(3, 42);

        p.run();
    }

    static class WithTimers extends DoFn<KV<String, Integer>, Integer> {
        private final String timerId = "myTimer";

        @TimerId(timerId)
        private final TimerSpec spec = TimerSpecs.timer(TimeDomain.EVENT_TIME);

        @ProcessElement
        public void processElement(ProcessContext context,
                                   @TimerId(timerId) Timer timer) {
            System.out.println(context.element());
            timer.set(Instant.now().plus(Duration.standardSeconds(1)));
            context.output(3);
        }

        @OnTimer(timerId)
        public void onTimer(OnTimerContext context) {
            context.output(42);
        }
    }
}
