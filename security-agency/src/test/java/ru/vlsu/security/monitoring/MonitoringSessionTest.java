package ru.vlsu.security.monitoring;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringSessionTest {

    static final String CAR = "[4] Ford Fusion (М117РН)";
    static final String CAR_SUFFIX = " Автомобиль [" + CAR + "].";
    static final String FLAT = "[1] г. Владимир, ул. Школьная, д. 7, кв. 104";

    static class RecordingListener implements MonitoringListener {
        final List<String> journal = new ArrayList<>();
        final List<String> statuses = new ArrayList<>();
        final List<SensorType> alarms = new ArrayList<>();
        final List<Integer> gbrTimer = new ArrayList<>();
        boolean dispatchOk = true;
        int dispatchRequests;

        @Override
        public void onJournal(String message) {
            journal.add(message);
        }

        @Override
        public void onStatus(String status) {
            statuses.add(status);
        }

        @Override
        public void onAlarm(SensorType sensor, String message) {
            alarms.add(sensor);
        }

        @Override
        public boolean onDispatchRequested() {
            dispatchRequests++;
            return dispatchOk;
        }

        @Override
        public void onGbrTimer(int secondsLeft) {
            gbrTimer.add(secondsLeft);
        }

        String last() {
            return journal.get(journal.size() - 1);
        }
    }

    final RecordingListener l = new RecordingListener();

    MonitoringSession car(boolean armed) {
        return new MonitoringSession(ObjectKind.CAR, CAR, "Operator", "Каприянов А.Ф.", armed, 900, 900, l);
    }

    MonitoringSession flat(int mchsEta) {
        return new MonitoringSession(ObjectKind.NDV, FLAT, "Operator", "Смирнова О.П.", true, 900, mchsEta, l);
    }

    @Test
    void trigger_disarmed_ignored() {
        assertFalse(car(false).trigger(SensorType.HOOD));
        assertTrue(l.journal.isEmpty());
        assertTrue(l.alarms.isEmpty());
    }

    @Test
    void trigger_wrongKind_ignored() {
        assertFalse(car(true).trigger(SensorType.MOTION));
        assertTrue(l.journal.isEmpty());
    }

    @Test
    void trigger_firstSensor_journalAndAlarm() {
        MonitoringSession s = car(true);
        assertTrue(s.trigger(SensorType.HOOD));
        assertEquals(List.of("Подкапотное пространство: Срабатывание датчика взлома №1." + CAR_SUFFIX), l.journal);
        assertEquals(List.of(SensorType.HOOD), l.alarms);
        assertEquals(1, s.sensorCount());
    }

    @Test
    void thirdSensor_autoDispatch() {
        MonitoringSession s = car(true);
        s.trigger(SensorType.HOOD);
        s.trigger(SensorType.FRONT_DOOR);
        s.trigger(SensorType.REAR_DOOR);
        assertTrue(l.journal.contains("Множественное срабатывание датчиков: [3]." + CAR_SUFFIX));
        assertEquals("ГБР: Группа Быстрого Реагирования отправлена [Operator]." + CAR_SUFFIX, l.last());
        assertEquals(900, s.gbrSecondsLeft());
        assertEquals(List.of(900), l.gbrTimer);
    }

    @Test
    void fourthSensor_whileEnRoute_noSecondDispatch() {
        MonitoringSession s = car(true);
        for (int i = 0; i < 4; i++) {
            s.trigger(SensorType.TRUNK);
        }
        assertEquals(1, l.dispatchRequests);
        assertEquals(4, s.sensorCount());
    }

    @Test
    void panic_dispatchesImmediately() {
        MonitoringSession s = flat(900);
        s.trigger(SensorType.PANIC);
        assertEquals(900, s.gbrSecondsLeft());
        assertEquals("КТС: Срабатывание тревожной кнопки №1. Объект [" + FLAT + "].", l.journal.get(0));
    }

    @Test
    void requestGbr_twice_alreadyEnRoute() {
        MonitoringSession s = car(true);
        assertTrue(s.requestGbr());
        assertFalse(s.requestGbr());
        assertEquals("Группа быстрого реагирования уже в пути. Время до прибытия [15 минут]", l.last());
    }

    @Test
    void cancelGbr_allowsNewRequest() {
        MonitoringSession s = car(true);
        l.dispatchOk = false;
        assertFalse(s.requestGbr());
        assertEquals("Нет свободных групп быстрого реагирования", l.last());
        assertEquals(-1, s.gbrSecondsLeft());
        l.dispatchOk = true;
        assertTrue(s.requestGbr());
        assertEquals(900, s.gbrSecondsLeft());
    }

    @Test
    void tick_900_gbrArrives() {
        MonitoringSession s = car(true);
        s.trigger(SensorType.HOOD);
        s.requestGbr();
        for (int i = 0; i < 900; i++) {
            s.tick();
        }
        assertEquals("ГБР прибыла на место происшествия", l.statuses.get(l.statuses.size() - 1));
        assertEquals("ГБР: Группа Быстрого Реагирования прибыла [Operator]." + CAR_SUFFIX, l.last());
        assertEquals(-1, s.gbrSecondsLeft());
        assertEquals(0, s.sensorCount());
        assertEquals(0, l.gbrTimer.get(l.gbrTimer.size() - 1));
        s.tick();
        assertEquals(-1, s.gbrSecondsLeft());
    }

    @Test
    void ignition_running_statusAndMessage() {
        car(true).trigger(SensorType.IGNITION);
        assertEquals("Зажигание: Двигатель заведен на охране №1." + CAR_SUFFIX, l.last());
        assertEquals("Машина заведена", l.statuses.get(l.statuses.size() - 1));
    }

    @Test
    void ignition_blocked_attemptMessage() {
        MonitoringSession s = car(true);
        s.setIgnitionBlocked(true);
        assertEquals("Глушение связи: Двигатель заглушен сотрудником Operator по обращению владельца договора "
                + "[Каприянов А.Ф.]." + CAR_SUFFIX, l.last());
        assertEquals("Двигатель заглушен", l.statuses.get(l.statuses.size() - 1));
        s.trigger(SensorType.IGNITION);
        assertEquals("Зажигание: Попытка включения зажигания №1." + CAR_SUFFIX, l.last());
        assertEquals("Двигатель заглушен", l.statuses.get(l.statuses.size() - 1));
        s.setIgnitionBlocked(false);
        assertEquals("Машина на стоянке", l.statuses.get(l.statuses.size() - 1));
    }

    @Test
    void battery_hasNoNumber() {
        car(true).trigger(SensorType.BATTERY);
        assertEquals("АКБ: Аккумулятор разряжен/отключен." + CAR_SUFFIX, l.last());
    }

    @Test
    void mchs_car_returnsFalse() {
        assertFalse(car(true).callMchs());
        assertTrue(l.journal.isEmpty());
    }

    @Test
    void mchs_ndv_arrivesAfterEta() {
        MonitoringSession s = flat(5);
        assertTrue(s.callMchs());
        assertEquals("МЧС: МЧС вызвана на охраняемый объект [Operator]. Объект [" + FLAT + "].", l.last());
        assertFalse(s.callMchs());
        assertEquals("МЧС уже в пути. Время до прибытия [1 минут]", l.last());
        for (int i = 0; i < 5; i++) {
            s.tick();
        }
        assertEquals("МЧС: МЧС прибыла на охраняемый объект [Operator]. Объект [" + FLAT + "].", l.last());
        assertEquals("МЧС прибыл на охраняемый объект", l.statuses.get(l.statuses.size() - 1));
        assertEquals(-1, s.mchsSecondsLeft());
    }

    @Test
    void disarm_resetsCounter_andBlocksSensors() {
        MonitoringSession s = flat(900);
        s.trigger(SensorType.MOTION);
        s.setArmed(false);
        assertEquals("Охрана: Охрана снята сотрудником Operator по обращению владельца договора [Смирнова О.П.]. "
                + "Объект [" + FLAT + "].", l.last());
        assertEquals("Объект снят с охраны", l.statuses.get(l.statuses.size() - 1));
        assertEquals(0, s.sensorCount());
        assertFalse(s.trigger(SensorType.MOTION));
        s.setArmed(true);
        assertEquals("Охрана: Объект поставлен на охрану сотрудником Operator по обращению владельца договора "
                + "[Смирнова О.П.]. Объект [" + FLAT + "].", l.last());
    }

    @Test
    void armCar_messages() {
        MonitoringSession s = car(false);
        s.setArmed(true);
        assertEquals("Охрана: Машина поставлена на охрану сотрудником Operator по обращению владельца договора "
                + "[Каприянов А.Ф.]." + CAR_SUFFIX, l.last());
        assertEquals("Машина поставлена на охрану", l.statuses.get(l.statuses.size() - 1));
    }

    @Test
    void setArmed_sameValue_noJournal() {
        car(true).setArmed(true);
        assertTrue(l.journal.isEmpty());
    }

    @Test
    void journalLine_format() {
        assertEquals("11.04.2023 13:58:02 : x", JournalLine.format(LocalDateTime.of(2023, 4, 11, 13, 58, 2), "x"));
    }
}
