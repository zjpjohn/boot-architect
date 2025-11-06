package com.cloud.arch.mutex;


public class MutexEvent {

    public static final String ACQUIRED = "acquired";
    public static final String RELEASED = "released";

    private       String event;
    private       String ownerId;
    private final long   eventAt = System.currentTimeMillis();

    public void setEvent(String event) {
        this.event = event;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getEvent() {
        return event;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public long getEventAt() {
        return eventAt;
    }

}
