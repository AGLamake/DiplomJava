package ru.vlsu.security.monitoring;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensorTypeTest {

    @Test
    void forKind_counts() {
        assertEquals(6, SensorType.forKind(ObjectKind.CAR).size());
        assertEquals(7, SensorType.forKind(ObjectKind.NDV).size());
    }

    @Test
    void codes_unique() {
        assertEquals(SensorType.values().length, Arrays.stream(SensorType.values()).map(SensorType::code).distinct().count());
    }

    @Test
    void onlyPanicIsImmediate() {
        assertEquals(1, Arrays.stream(SensorType.values()).filter(SensorType::immediateDispatch).count());
        assertEquals(true, SensorType.PANIC.immediateDispatch());
    }
}
