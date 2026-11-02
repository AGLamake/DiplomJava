package ru.vlsu.security.web;

import ru.vlsu.security.model.DispatchResult;

import java.io.Serializable;

/** Диалог завершения выезда ГБР: результат и примечание (бывший CompleteDispatchDialog). */
public class CompletionForm implements Serializable {

    private DispatchResult result;
    private String notes;

    public DispatchResult[] getResults() {
        return DispatchResult.values();
    }

    public DispatchResult getResult() {
        return result;
    }

    public void setResult(DispatchResult result) {
        this.result = result;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
