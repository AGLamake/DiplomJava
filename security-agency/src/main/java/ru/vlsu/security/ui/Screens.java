package ru.vlsu.security.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import ru.vlsu.security.AppContext;
import ru.vlsu.security.model.Client;
import ru.vlsu.security.model.RealEstate;
import ru.vlsu.security.model.User;
import ru.vlsu.security.model.VehicleObject;
import ru.vlsu.security.security.Access;
import ru.vlsu.security.security.Permissions;
import ru.vlsu.security.security.Section;
import ru.vlsu.security.ui.forms.ClientForm;
import ru.vlsu.security.ui.forms.RealEstateForm;
import ru.vlsu.security.ui.forms.VehicleForm;
import ru.vlsu.security.ui.table.ColumnSpec;
import ru.vlsu.security.ui.table.TableSpec;
import ru.vlsu.security.ui.table.TableViewController;

import java.util.List;

/** Фабрика экранов разделов главного меню. */
public final class Screens {

    /** Обработчики жизненного цикла экрана; хранятся в properties узла. */
    public interface Lifecycle {
        default void onShown() {
        }

        default void dispose() {
        }
    }

    private static final String KEY = "screen.lifecycle";

    private Screens() {
    }

    public static Parent open(Section section, AppContext ctx, User user) {
        Access access = Permissions.access(user.role(), section);
        return switch (section) {
            case CLIENTS -> TableViewController.create(TableSpec.of("Клиенты", List.of(
                    new ColumnSpec<Client>("ID", Client::id, 60),
                    new ColumnSpec<>("Тип", Client::type, 110),
                    new ColumnSpec<>("Наименование / ФИО", Client::name, 280),
                    new ColumnSpec<>("Контактное лицо", Client::contactPerson, 220),
                    new ColumnSpec<>("Телефон", Client::phone, 150),
                    new ColumnSpec<>("E-mail", Client::email, 200)),
                    ctx.clients(), new ClientForm(), Client::id), access);
            case REAL_ESTATE -> TableViewController.create(TableSpec.of("Объекты недвижимости", List.of(
                    new ColumnSpec<RealEstate>("ID", RealEstate::id, 60),
                    new ColumnSpec<>("Клиент", RealEstate::clientName, 220),
                    new ColumnSpec<>("Название", RealEstate::name, 150),
                    new ColumnSpec<>("Адрес", RealEstate::address, 340),
                    new ColumnSpec<>("Статус", RealEstate::objectStatus, 120),
                    new ColumnSpec<>("Охрана", RealEstate::securityStatus, 120)),
                    ctx.realEstate(), new RealEstateForm(ctx.clients()), RealEstate::id), access);
            case VEHICLES -> TableViewController.create(TableSpec.of("Автомобили", List.of(
                    new ColumnSpec<VehicleObject>("ID", VehicleObject::objectId, 60),
                    new ColumnSpec<>("Клиент", VehicleObject::clientName, 220),
                    new ColumnSpec<>("Марка", VehicleObject::brand, 120),
                    new ColumnSpec<>("Модель", VehicleObject::model, 120),
                    new ColumnSpec<>("Гос. номер", VehicleObject::regNumber, 110),
                    new ColumnSpec<>("VIN", VehicleObject::vin, 180),
                    new ColumnSpec<>("Статус", VehicleObject::objectStatus, 110),
                    new ColumnSpec<>("Охрана", VehicleObject::securityStatus, 110)),
                    ctx.vehicles(), new VehicleForm(ctx.clients()), VehicleObject::objectId), access);
            default -> placeholder(section);
        };
    }

    public static <T extends Node> T withLifecycle(T node, Lifecycle lifecycle) {
        node.getProperties().put(KEY, lifecycle);
        return node;
    }

    static void onShown(Node node) {
        if (node.getProperties().get(KEY) instanceof Lifecycle l) {
            l.onShown();
        }
    }

    static void dispose(Node node) {
        if (node.getProperties().get(KEY) instanceof Lifecycle l) {
            l.dispose();
        }
    }

    private static Parent placeholder(Section section) {
        Label label = new Label(section.title());
        label.getStyleClass().add("screen-title");
        return new StackPane(label);
    }
}
