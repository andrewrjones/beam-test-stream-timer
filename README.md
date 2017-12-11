An example of a ParDo with Timers that hangs when running under TestStream.

When running `gradle test -d` you will see the following:

```
17:02:29.631 [DEBUG] [TestEventLogger] com.andrewjones.beam.TimerTest > testTimer STANDARD_OUT
17:02:29.631 [DEBUG] [TestEventLogger]     KV{hello, 100}
17:02:29.662 [DEBUG] [TestEventLogger]     KV{hello, 200}
```

Then the test hangs. Changing:

```
.addElements(KV.of("hello", 100))
.addElements(KV.of("hello", 200))
```

To:

```
.addElements(KV.of("hello", 100), KV.of("hello", 200))
```

Seems to work. Changing one of the keys will also work.