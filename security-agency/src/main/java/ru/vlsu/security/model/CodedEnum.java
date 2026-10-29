package ru.vlsu.security.model;

/** Перечисление, хранимое в БД строковым кодом и показываемое русским названием. */
public interface CodedEnum {

    String code();

    String title();

    static <E extends Enum<E> & CodedEnum> E fromCode(Class<E> type, String code) {
        for (E e : type.getEnumConstants()) {
            if (e.code().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Неизвестное значение " + type.getSimpleName() + ": " + code);
    }
}
