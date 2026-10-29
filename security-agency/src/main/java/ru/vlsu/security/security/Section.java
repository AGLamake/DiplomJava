package ru.vlsu.security.security;

/** Разделы главного меню в порядке отображения. */
public enum Section {
    CAR_MONITOR("Автомобиль"),
    NDV_MONITOR("Недвижимость"),
    ALARMS("Тревоги и выезды"),
    CLIENTS("Клиенты"),
    REAL_ESTATE("Объекты недвижимости"),
    VEHICLES("Автомобили"),
    CONTRACTS("Договоры"),
    TEAMS("ГБР"),
    EQUIPMENT("Оборудование"),
    INSTALL_REQUESTS("Заявки на монтаж"),
    MY_DISPATCHES("Мои выезды"),
    USERS("Пользователи"),
    AUDIT("Журнал действий");

    private final String title;

    Section(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
