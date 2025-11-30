package com.org.model;

public class Switch {
    private Integer id;
    private String hostname;
    private String ipv4;
    private String location;

    public Switch() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getIpv4() { return ipv4; }
    public void setIpv4(String ipv4) { this.ipv4 = ipv4; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @Override
    public String toString() {
        return "Switch{" +
                "id=" + id +
                ", hostname='" + hostname + '\'' +
                ", ipv4='" + ipv4 + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
