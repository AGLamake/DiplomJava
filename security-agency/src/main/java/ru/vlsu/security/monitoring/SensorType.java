package ru.vlsu.security.monitoring;

import java.util.Arrays;
import java.util.List;

/** Датчики охранной системы и тексты событий журнала (из CreateLog WinForms-версии). */
public enum SensorType {
    HOOD(ObjectKind.CAR, "car_hood", "Капот",
            "Подкапотное пространство: Срабатывание датчика взлома №%d.", "high", false),
    FRONT_DOOR(ObjectKind.CAR, "car_front_door", "Передняя дверь",
            "Передние двери: Срабатывание датчика удара №%d.", "high", false),
    REAR_DOOR(ObjectKind.CAR, "car_rear_door", "Задняя дверь",
            "Задние двери: Срабатывание датчика удара №%d.", "high", false),
    TRUNK(ObjectKind.CAR, "car_trunk", "Багажник",
            "Багажник: Срабатывание датчика удара №%d.", "high", false),
    BATTERY(ObjectKind.CAR, "car_battery", "АКБ",
            "АКБ: Аккумулятор разряжен/отключен.", "medium", false),
    IGNITION(ObjectKind.CAR, "car_ignition", "Зажигание",
            "Зажигание: Двигатель заведен на охране №%d.", "critical", false),
    MOTION(ObjectKind.NDV, "motion", "ИК",
            "ИК: Срабатывание датчика движения №%d.", "high", false),
    DOOR_OPEN(ObjectKind.NDV, "door_open", "СМК",
            "СМК: Срабатывание датчика открытия двери №%d.", "high", false),
    GLASS_BREAK(ObjectKind.NDV, "glass_break", "ДРС",
            "ДРС: Срабатывание датчика разбития стекла №%d.", "high", false),
    FIRE(ObjectKind.NDV, "fire", "ДИП",
            "ДИП: Срабатывание комбинированного датчика пожарной сигнализации №%d.", "critical", false),
    PANIC(ObjectKind.NDV, "panic_button", "КТС",
            "КТС: Срабатывание тревожной кнопки №%d.", "critical", true),
    WATER_LEAK(ObjectKind.NDV, "water_leak", "ДУВ",
            "ДУВ: Срабатывание датчика протечки воды №%d.", "medium", false),
    GAS_LEAK(ObjectKind.NDV, "gas_leak", "ДУГ",
            "ДУГ: Срабатывание датчика утечки газа №%d.", "critical", false);

    private final ObjectKind kind;
    private final String code;
    private final String label;
    private final String template;
    private final String priority;
    private final boolean immediateDispatch;

    SensorType(ObjectKind kind, String code, String label, String template, String priority,
               boolean immediateDispatch) {
        this.kind = kind;
        this.code = code;
        this.label = label;
        this.template = template;
        this.priority = priority;
        this.immediateDispatch = immediateDispatch;
    }

    public ObjectKind kind() {
        return kind;
    }

    /** Значение alarm_events.event_type. */
    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String template() {
        return template;
    }

    public String priority() {
        return priority;
    }

    public boolean immediateDispatch() {
        return immediateDispatch;
    }

    public static List<SensorType> forKind(ObjectKind kind) {
        return Arrays.stream(values()).filter(s -> s.kind == kind).toList();
    }

    /** Подпись события по коду из БД; для неизвестных кодов — сам код. */
    public static String labelForCode(String code) {
        if ("manual_dispatch".equals(code)) {
            return "Ручной вызов";
        }
        return Arrays.stream(values()).filter(s -> s.code.equals(code)).map(SensorType::label).findFirst()
                .orElse(code);
    }
}
