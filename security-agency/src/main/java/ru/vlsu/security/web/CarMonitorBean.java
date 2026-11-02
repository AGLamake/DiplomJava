package ru.vlsu.security.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import ru.vlsu.security.monitoring.ObjectKind;

@Named
@SessionScoped
public class CarMonitorBean extends MonitorBean {

    public CarMonitorBean() {
        super(ObjectKind.CAR);
    }
}
