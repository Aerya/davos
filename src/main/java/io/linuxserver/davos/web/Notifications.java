package io.linuxserver.davos.web;

import java.util.ArrayList;
import java.util.List;

public class Notifications {

    private List<Pushbullet> pushbullet = new ArrayList<Pushbullet>();
    private List<SNS> sns = new ArrayList<SNS>();
    private List<Discord> discord = new ArrayList<Discord>();
    private List<Apprise> apprise = new ArrayList<Apprise>();

    public List<Pushbullet> getPushbullet() {
        return pushbullet;
    }

    public List<SNS> getSns() {
        return sns;
    }

    public List<Discord> getDiscord() {
        return discord;
    }

    public List<Apprise> getApprise() {
        return apprise;
    }

    public void setPushbullet(List<Pushbullet> pushbullet) {
        this.pushbullet = pushbullet;
    }

    public void setSns(List<SNS> sns) {
        this.sns = sns;
    }

    public void setDiscord(List<Discord> discord) {
        this.discord = discord;
    }

    public void setApprise(List<Apprise> apprise) {
        this.apprise = apprise;
    }
}
