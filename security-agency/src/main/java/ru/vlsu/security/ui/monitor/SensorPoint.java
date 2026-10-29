package ru.vlsu.security.ui.monitor;

import ru.vlsu.security.monitoring.ObjectKind;
import ru.vlsu.security.monitoring.SensorType;

import java.util.List;

/** Положение кнопки датчика на схеме 592×355 (центр бывшей радиокнопки WinForms). */
public record SensorPoint(SensorType sensor, double x, double y) {

    public static List<SensorPoint> forKind(ObjectKind kind) {
        return kind == ObjectKind.CAR
                ? List.of(new SensorPoint(SensorType.HOOD, 58, 85),
                new SensorPoint(SensorType.FRONT_DOOR, 332, 99),
                new SensorPoint(SensorType.REAR_DOOR, 438, 123),
                new SensorPoint(SensorType.TRUNK, 58, 259),
                new SensorPoint(SensorType.IGNITION, 318, 273),
                new SensorPoint(SensorType.BATTERY, 254, 302))
                : List.of(new SensorPoint(SensorType.FIRE, 98, 46),
                new SensorPoint(SensorType.GAS_LEAK, 406, 61),
                new SensorPoint(SensorType.MOTION, 18, 144),
                new SensorPoint(SensorType.DOOR_OPEN, 303, 178),
                new SensorPoint(SensorType.GLASS_BREAK, 506, 160),
                new SensorPoint(SensorType.WATER_LEAK, 506, 207),
                new SensorPoint(SensorType.PANIC, 177, 259));
    }
}
