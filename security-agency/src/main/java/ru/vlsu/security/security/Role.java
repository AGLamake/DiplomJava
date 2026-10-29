package ru.vlsu.security.security;

/** Роли пользователей; code совпадает с roles.name в БД. */
public enum Role {
    ADMINISTRATOR("administrator", "Администратор"),
    MANAGER("manager", "Менеджер"),
    ENGINEER("engineer", "Инженер"),
    OPERATOR("operator", "Оператор"),
    RESPONSE_TEAM("response_team", "Сотрудник ГБР"),
    CLIENT("client", "Клиент");

    private final String code;
    private final String title;

    Role(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public static Role fromCode(String code) {
        for (Role r : values()) {
            if (r.code.equals(code)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Неизвестная роль: " + code);
    }

    @Override
    public String toString() {
        return title;
    }
}
