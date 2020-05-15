package eu.europa.ec.fisheries.uvms.incident.model.dto;

import java.io.Serializable;

public class StatusDto implements Serializable {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
