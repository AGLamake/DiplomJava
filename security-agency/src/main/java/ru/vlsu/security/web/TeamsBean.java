package ru.vlsu.security.web;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ru.vlsu.security.dao.CrudDao;
import ru.vlsu.security.model.ResponseTeam;
import ru.vlsu.security.model.TeamStatus;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;
import ru.vlsu.security.security.Section;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static ru.vlsu.security.validation.FormParsers.optionalText;
import static ru.vlsu.security.validation.FormParsers.requireChoice;
import static ru.vlsu.security.validation.FormParsers.requireText;

@Named
@ViewScoped
public class TeamsBean extends CrudBean<ResponseTeam, TeamsBean.Form> {

    private List<User> members = List.of();

    @Override
    public String getTitle() {
        return "Группы быстрого реагирования";
    }

    @Override
    protected Section section() {
        return Section.TEAMS;
    }

    @Override
    protected List<ColumnSpec<ResponseTeam>> columns() {
        return List.of(
                new ColumnSpec<>("ID", ResponseTeam::id, 60),
                new ColumnSpec<>("Название", ResponseTeam::name, 140),
                new ColumnSpec<>("Телефон", ResponseTeam::phone, 150),
                new ColumnSpec<>("Статус", ResponseTeam::status, 120),
                new ColumnSpec<>("Состав", ResponseTeam::membersText, 500));
    }

    @Override
    protected List<ResponseTeam> load() {
        return ctx().teams().findAll();
    }

    @Override
    protected long key(ResponseTeam row) {
        return row.id();
    }

    @Override
    protected CrudDao<ResponseTeam> dao() {
        return ctx().teams();
    }

    @Override
    protected String formTitle(boolean creating) {
        return creating ? "Новая группа" : "Группа быстрого реагирования";
    }

    @Override
    protected Form newForm(ResponseTeam t) {
        members = ctx().users().findByRole(Role.RESPONSE_TEAM);
        Form f = new Form();
        if (t != null) {
            f.id = t.id();
            f.name = t.name();
            f.phone = t.phone();
            f.status = t.status();
            f.memberIds = new ArrayList<>(t.memberUserIds());
        }
        return f;
    }

    @Override
    protected ResponseTeam build(Form f) {
        return new ResponseTeam(f.id, requireText(f.name, "Название"), optionalText(f.phone),
                requireChoice(f.status, "Статус"), List.copyOf(f.memberIds), null);
    }

    /** Активные пользователи с ролью «Сотрудник ГБР». */
    public List<User> getMembers() {
        return members;
    }

    public TeamStatus[] getStatuses() {
        return TeamStatus.values();
    }

    public static class Form implements Serializable {
        private long id;
        private String name;
        private String phone;
        private TeamStatus status = TeamStatus.AVAILABLE;
        private List<Long> memberIds = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public TeamStatus getStatus() {
            return status;
        }

        public void setStatus(TeamStatus status) {
            this.status = status;
        }

        public List<Long> getMemberIds() {
            return memberIds;
        }

        public void setMemberIds(List<Long> memberIds) {
            this.memberIds = memberIds == null ? new ArrayList<>() : new ArrayList<>(memberIds);
        }
    }
}
