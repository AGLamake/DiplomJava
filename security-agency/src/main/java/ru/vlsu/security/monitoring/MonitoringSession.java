package ru.vlsu.security.monitoring;

/**
 * Логика мониторинга одного объекта (бывший timer1_Tick и обработчики датчиков MainMenu).
 * Не зависит от JavaFX и БД: всё внешнее делается через {@link MonitoringListener}.
 */
public class MonitoringSession {

    private static final int MULTIPLE_SENSORS = 3;
    private static final int NOT_CALLED = -1;

    private final ObjectKind kind;
    private final String objectLabel;
    private final String operatorName;
    private final String ownerName;
    private final int gbrEtaSeconds;
    private final int mchsEtaSeconds;
    private final MonitoringListener listener;

    private boolean armed;
    private boolean ignitionBlocked;
    private int sensorCount;
    private int gbrSecondsLeft = NOT_CALLED;
    private int mchsSecondsLeft = NOT_CALLED;

    public MonitoringSession(ObjectKind kind, String objectLabel, String operatorName, String ownerName,
                             boolean armed, int gbrEtaSeconds, int mchsEtaSeconds, MonitoringListener listener) {
        this.kind = kind;
        this.objectLabel = objectLabel;
        this.operatorName = operatorName;
        this.ownerName = ownerName;
        this.armed = armed;
        this.gbrEtaSeconds = gbrEtaSeconds;
        this.mchsEtaSeconds = mchsEtaSeconds;
        this.listener = listener;
    }

    private String suffix() {
        return kind == ObjectKind.CAR ? " Автомобиль [" + objectLabel + "]." : " Объект [" + objectLabel + "].";
    }

    private static int minutes(int seconds) {
        return (seconds + 59) / 60;
    }

    public boolean trigger(SensorType sensor) {
        if (!armed || sensor.kind() != kind) {
            return false;
        }
        sensorCount++;
        String text;
        if (sensor == SensorType.IGNITION && ignitionBlocked) {
            text = "Зажигание: Попытка включения зажигания №%d.".formatted(sensorCount);
        } else {
            text = sensor.template().formatted(sensorCount);
            if (sensor == SensorType.IGNITION) {
                listener.onStatus("Машина заведена");
            }
        }
        String message = text + suffix();
        listener.onJournal(message);
        listener.onAlarm(sensor, message);
        if (sensor.immediateDispatch()) {
            requestGbr();
        } else if (sensorCount == MULTIPLE_SENSORS && gbrSecondsLeft <= 0) {
            listener.onJournal("Множественное срабатывание датчиков: [" + sensorCount + "]." + suffix());
            requestGbr();
        }
        return true;
    }

    public boolean requestGbr() {
        if (gbrSecondsLeft > 0) {
            listener.onJournal("Группа быстрого реагирования уже в пути. Время до прибытия ["
                    + minutes(gbrSecondsLeft) + " минут]");
            return false;
        }
        if (!listener.onDispatchRequested()) {
            listener.onJournal("Нет свободных групп быстрого реагирования");
            return false;
        }
        gbrSecondsLeft = gbrEtaSeconds;
        listener.onJournal("ГБР: Группа Быстрого Реагирования отправлена [" + operatorName + "]." + suffix());
        listener.onGbrTimer(gbrSecondsLeft);
        return true;
    }

    public boolean callMchs() {
        if (kind != ObjectKind.NDV) {
            return false;
        }
        if (mchsSecondsLeft > 0) {
            listener.onJournal("МЧС уже в пути. Время до прибытия [" + minutes(mchsSecondsLeft) + " минут]");
            return false;
        }
        mchsSecondsLeft = mchsEtaSeconds;
        listener.onJournal("МЧС: МЧС вызвана на охраняемый объект [" + operatorName + "]." + suffix());
        return true;
    }

    public void setArmed(boolean value) {
        if (armed == value) {
            return;
        }
        armed = value;
        boolean car = kind == ObjectKind.CAR;
        if (value) {
            listener.onStatus(car ? "Машина поставлена на охрану" : "Объект поставлен на охрану");
            listener.onJournal("Охрана: " + (car ? "Машина поставлена" : "Объект поставлен")
                    + " на охрану сотрудником " + operatorName + " по обращению владельца договора ["
                    + ownerName + "]." + suffix());
        } else {
            sensorCount = 0;
            listener.onStatus(car ? "Машина снята с охраны" : "Объект снят с охраны");
            listener.onJournal("Охрана: Охрана снята сотрудником " + operatorName
                    + " по обращению владельца договора [" + ownerName + "]." + suffix());
        }
    }

    public void setIgnitionBlocked(boolean value) {
        if (kind != ObjectKind.CAR || ignitionBlocked == value) {
            return;
        }
        ignitionBlocked = value;
        if (value) {
            listener.onStatus("Двигатель заглушен");
            listener.onJournal("Глушение связи: Двигатель заглушен сотрудником " + operatorName
                    + " по обращению владельца договора [" + ownerName + "]." + suffix());
        } else {
            listener.onStatus("Машина на стоянке");
            listener.onJournal("Глушение связи: Доступ к запуску двигателя разрешен сотрудником " + operatorName
                    + " по обращению владельца договора [" + ownerName + "]." + suffix());
        }
    }

    /** Вызывается раз в секунду. */
    public void tick() {
        if (gbrSecondsLeft > 0) {
            gbrSecondsLeft--;
            listener.onGbrTimer(gbrSecondsLeft);
            if (gbrSecondsLeft == 0) {
                gbrSecondsLeft = NOT_CALLED;
                sensorCount = 0;
                listener.onJournal("ГБР: Группа Быстрого Реагирования прибыла [" + operatorName + "]." + suffix());
                listener.onStatus("ГБР прибыла на место происшествия");
            }
        }
        if (mchsSecondsLeft > 0) {
            mchsSecondsLeft--;
            if (mchsSecondsLeft == 0) {
                mchsSecondsLeft = NOT_CALLED;
                listener.onJournal("МЧС: МЧС прибыла на охраняемый объект [" + operatorName + "]." + suffix());
                listener.onStatus("МЧС прибыл на охраняемый объект");
            }
        }
    }

    public int sensorCount() {
        return sensorCount;
    }

    public boolean armed() {
        return armed;
    }

    public boolean ignitionBlocked() {
        return ignitionBlocked;
    }

    public int gbrSecondsLeft() {
        return gbrSecondsLeft;
    }

    public int mchsSecondsLeft() {
        return mchsSecondsLeft;
    }
}
