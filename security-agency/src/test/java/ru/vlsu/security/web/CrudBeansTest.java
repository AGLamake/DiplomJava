package ru.vlsu.security.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.ClientType;
import ru.vlsu.security.model.Contract;
import ru.vlsu.security.model.User;
import ru.vlsu.security.security.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrudBeansTest {

    private AppContext ctx;
    private User admin;
    private WebTestSupport.RecordingUi ui;

    @BeforeEach
    void setUp() {
        ctx = WebTestSupport.freshContext(900);
        admin = WebTestSupport.user(ctx, "admin", "Админов А.А.", Role.ADMINISTRATOR);
        ui = new WebTestSupport.RecordingUi();
    }

    @Test
    void clients_addValidateEditDelete() {
        ClientsBean bean = WebTestSupport.wire(new ClientsBean(), ctx, admin, ui);
        int before = bean.getRows().size();

        bean.startAdd();
        assertEquals(true, ui.callbacks.get("open"));
        assertEquals("Новый клиент", bean.getFormTitle());
        bean.getForm().setType(ClientType.LEGAL_ENTITY);
        bean.getForm().setName("ООО Тест");
        bean.save();
        assertEquals(List.of("Поле «Телефон» обязательно для заполнения"), ui.warnings);
        assertNotNull(bean.getForm(), "при ошибке проверки форма остаётся открытой");
        assertFalse(ui.callbacks.containsKey("saved"));

        bean.getForm().setPhone("+7 900 000-00-00");
        bean.save();
        assertEquals(true, ui.callbacks.get("saved"));
        assertNull(bean.getForm());
        assertEquals(before + 1, bean.getRows().size());

        Client created = WebTestSupport.rowWhere(bean, c -> c.name().equals("ООО Тест"));
        bean.setSelected(created);
        bean.startEdit();
        assertEquals("Клиент", bean.getFormTitle());
        assertEquals(ClientType.LEGAL_ENTITY, bean.getForm().getType());
        bean.getForm().setName("ООО Тест-2");
        bean.save();
        assertEquals("ООО Тест-2", bean.getSelected().name(), "выбранная строка сохраняется после обновления");

        bean.delete();
        assertEquals(before, bean.getRows().size());
        assertNull(bean.getSelected());
    }

    @Test
    void search_filtersRows() {
        ClientsBean bean = WebTestSupport.wire(new ClientsBean(), ctx, admin, ui);
        String name = bean.getRows().get(0).name();
        bean.setQuery(" " + name.toUpperCase() + " ");
        bean.applyFilter();
        assertTrue(bean.getRows().stream().allMatch(c -> c.name().equals(name)));
        bean.setQuery("");
        bean.applyFilter();
        assertTrue(bean.getRows().size() > 1);
    }

    @Test
    void readOnlyAccess_blocksChanges() {
        User engineer = WebTestSupport.user(ctx, "eng", "Инженеров И.И.", Role.ENGINEER);
        ClientsBean bean = WebTestSupport.wire(new ClientsBean(), ctx, engineer, ui);
        assertFalse(bean.isCanWrite());
        bean.startAdd();
        assertNull(bean.getForm());
        int before = bean.getRows().size();
        bean.setSelected(bean.getRows().get(0));
        bean.delete();
        assertEquals(before, ctx.clients().findAll().size());
    }

    @Test
    void deleteReferencedClient_showsDbMessage() {
        ClientsBean bean = WebTestSupport.wire(new ClientsBean(), ctx, admin, ui);
        long withObjects = ctx.realEstate().findAll().get(0).clientId();
        bean.setSelected(WebTestSupport.rowWhere(bean, c -> c.id() == withObjects));
        bean.delete();
        assertEquals(List.of("Запись используется в других таблицах и не может быть удалена"), ui.errors);
    }

    @Test
    void contracts_objectsFollowClient() {
        ContractsBean bean = WebTestSupport.wire(new ContractsBean(), ctx, admin, ui);
        Contract existing = bean.getRows().stream().filter(c -> !c.objectIds().isEmpty()).findFirst().orElseThrow();
        bean.setSelected(existing);
        bean.startEdit();
        assertEquals(existing.objectIds(), bean.getForm().getObjectIds());
        assertTrue(bean.getClientObjects().stream().allMatch(o -> o.clientId() == existing.clientId()));

        long otherClient = bean.getClients().stream().filter(c -> c.id() != existing.clientId()).findFirst()
                .orElseThrow().id();
        bean.getForm().setClientId(otherClient);
        bean.clientChanged();
        assertTrue(bean.getForm().getObjectIds().isEmpty(), "отметки объектов прежнего клиента сняты");
    }

    @Test
    void users_cannotAddOrDelete_resetPasswordChecksRepeat() {
        UsersBean bean = WebTestSupport.wire(new UsersBean(), ctx, admin, ui);
        assertFalse(bean.isCanAdd());
        assertFalse(bean.isCanDelete());
        bean.setSelected(bean.getRows().get(0));
        bean.startResetPassword();
        bean.setNewPassword("abcdef");
        bean.setNewPasswordRepeat("abcdeX");
        bean.resetPassword();
        assertEquals(List.of("Пароли отличаются!"), ui.warnings);
        bean.setNewPassword("abcdef");
        bean.setNewPasswordRepeat("abcdef");
        bean.resetPassword();
        assertEquals(true, ui.callbacks.get("saved"));
    }

    @Test
    void users_adminCannotDemoteSelf() {
        UsersBean bean = WebTestSupport.wire(new UsersBean(), ctx, admin, ui);
        bean.setSelected(WebTestSupport.rowWhere(bean, u -> u.id() == admin.id()));
        bean.startEdit();
        bean.getForm().setRole(Role.OPERATOR);
        bean.save();
        assertEquals(List.of("Нельзя изменить собственную роль или заблокировать себя"), ui.warnings);
    }
}
