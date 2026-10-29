package ru.vlsu.security.monitoring;

/** События сессии мониторинга для экрана и сервисов БД. */
public interface MonitoringListener {

    void onJournal(String message);

    void onStatus(String status);

    /** Сработал датчик — событие нужно сохранить в alarm_events. */
    void onAlarm(SensorType sensor, String message);

    /** Нужно создать выезд ГБР; false — если свободной группы нет. */
    boolean onDispatchRequested();

    /** Осталось секунд до прибытия ГБР; 0 — группа прибыла. */
    void onGbrTimer(int secondsLeft);
}
