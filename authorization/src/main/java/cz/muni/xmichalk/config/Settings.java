package cz.muni.xmichalk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "custom-settings")
public class Settings {
    private boolean accessGrantedByDefault;

    public boolean isAccessGrantedByDefault() {
        return  accessGrantedByDefault;
    }
    
    public void setAccessGrantedByDefault(boolean accessGrantedByDefault) {
        this.accessGrantedByDefault = accessGrantedByDefault;
    }
}