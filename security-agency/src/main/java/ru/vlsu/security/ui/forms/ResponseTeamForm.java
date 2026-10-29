package ru.vlsu.security.ui.forms;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.vlsu.security.dao.UserDao;
import ru.vlsu.security.model.ResponseTeam;
import ru.vlsu.security.model.TeamStatus;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.ui.table.EntityForm;
import ru.vlsu.security.ui.table.FormDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

public class ResponseTeamForm implements EntityForm<ResponseTeam> {

    private final UserDao users;

    public ResponseTeamForm(UserDao users) {
        this.users = users;
    }

    @Override
    public Optional<ResponseTeam> show(Window owner, ResponseTeam initial) {
        FormDialog<ResponseTeam> f = new FormDialog<>(owner, initial == null ? "Новая группа" : "Группа быстрого реагирования");
        TextField name = f.add("Название", new TextField());
        TextField phone = f.add("Телефон", new TextField());
        ComboBox<TeamStatus> status = f.add("Статус", Forms.combo(TeamStatus.values()));
        status.setValue(TeamStatus.AVAILABLE);
        VBox membersBox = f.add("Состав", new VBox(4));
        List<CheckBox> checks = new ArrayList<>();
        List<Long> current = initial == null ? List.of() : initial.memberUserIds();
        for (User u : users.findByRole(Role.RESPONSE_TEAM)) {
            CheckBox cb = new CheckBox(u.fullName());
            cb.setUserData(u.id());
            cb.setSelected(current.contains(u.id()));
            checks.add(cb);
            membersBox.getChildren().add(cb);
        }
        if (checks.isEmpty()) {
            membersBox.getChildren().add(new Label("Нет активных пользователей с ролью «Сотрудник ГБР»"));
        }
        long id = 0;
        if (initial != null) {
            id = initial.id();
            name.setText(initial.name());
            phone.setText(initial.phone());
            status.setValue(initial.status());
        }
        long teamId = id;
        return f.show(() -> new ResponseTeam(teamId, requireText(name.getText(), "Название"),
                optionalText(phone.getText()), requireChoice(status.getValue(), "Статус"),
                checks.stream().filter(CheckBox::isSelected).map(cb -> (Long) cb.getUserData()).toList(), null));
    }
}
