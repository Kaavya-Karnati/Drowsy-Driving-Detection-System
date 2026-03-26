package com.example.drowsydrivingdetection;

public class Alert {
    private String type;
    private long time;

    // Declare Alert object so it saves with it's type (audio or visual) and the time (for stats)
    // - Anthony
    public Alert(String type, long time){
        this.type = type;
        this.time = time;
    }


}
