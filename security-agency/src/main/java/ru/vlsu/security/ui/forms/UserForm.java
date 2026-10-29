package ru.vlsu.security.ui.forms;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.ui.table.EntityForm;
import ru.vlsu.security.ui.table.FormDialog;

import java.util.Arrays;
import java.util.Optional;

import static ru.vlsu.security.ui.table.FormParsers.optionalText;
import static ru.vlsu.security.ui.table.FormParsers.requireChoice;
import static ru.vlsu.security.ui.table.FormParsers.requireText;

/** Редактирование учётной записи администратором (создаются они только регистрацией). */
public class UserForm implements EntityForm<User> {

    @Override
    public Optional<User> show(Window owner, User initial) {
        FormDialog<User> f = new FormDialog<>(owner, "Пользователь");
        f.add("Логин", new Label(initial.login()));
        TextField fullName = f.add("ФИО", new TextField(initial.fullName()));
        TextField phone = f.add("Телефон", new TextField(initial.phone()));
        TextField email = f.add("E-mail", new TextField(initial.email()));
        ComboBox<Role> role = f.add("Роль", Forms.combo(Role.values()));
        role.setValue(initial.role());
        CheckBox active = f.add("Активен", new CheckBox());
        active.setSelected(initial.active());
        return f.show(() -> new User(initial.id(), initial.login(), requireText(fullName.getText(), "ФИО"),
                requireChoice(role.getValue(), "Роль"), active.isSelected(),
                optionalText(phone.getText()), optionalText(email.getText())));
    }

    /** Диалог нового пароля; возвращает пароль, если оба поля совпали. */
    public static Optional<char[]> askNewPassword(Window owner, User user) {
        FormDialog<char[]> f = new FormDialog<>(owner, "Сброс пароля: " + user.login());
        PasswordField p1 = f.add("Новый пароль", new PasswordField());
        PasswordField p2 = f.add("Повторите пароль", new PasswordField());
        return f.show(() -> {
            char[] a = p1.getText().toCharArray();
            if (!Arrays.equals(a, p2.getText().toCharArray())) {
                throw new IllegalArgumentException("Пароли отличаются!");
            }
            return a;
        });
    }
}
