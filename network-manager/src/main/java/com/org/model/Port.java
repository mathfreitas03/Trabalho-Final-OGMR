package com.org.model;

public class Port {
    private Integer id;
    private Integer switchId;
    private Integer number;     // número físico ou porta
    private Integer ifIndex;    
    private String ipv4;
    private boolean status;     // true = ativa
    private boolean lockable;
    private String description;

    public Port() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getSwitchId() { return switchId; }
    public void setSwitchId(Integer switchId) { this.switchId = switchId; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    public Integer getIfIndex() { return ifIndex; }
    public void setIfIndex(Integer ifIndex) { this.ifIndex = ifIndex; }

    public String getIpv4() { return ipv4; }
    public void setIpv4(String ipv4) { this.ipv4 = ipv4; }

    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    public boolean isLockable() { return lockable; }
    public void setLockable(boolean lockable) { this.lockable = lockable; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "Port{" +
                "id=" + id +
                ", switchId=" + switchId +
                ", number=" + number +
                ", ifIndex=" + ifIndex +
                ", ipv4='" + ipv4 + '\'' +
                ", status=" + status +
                ", lockable=" + lockable +
                ", description='" + description + '\'' +
                '}';
    }
}
