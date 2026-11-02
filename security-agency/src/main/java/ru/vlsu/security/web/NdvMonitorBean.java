package ru.vlsu.security.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import ru.vlsu.security.monitoring.ObjectKind;

@Named
@SessionScoped
public class NdvMonitorBean extends MonitorBean {

    public NdvMonitorBean() {
        super(ObjectKind.NDV);
    }
}
