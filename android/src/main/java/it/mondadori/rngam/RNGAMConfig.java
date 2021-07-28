package it.mondadori.rngam;

import java.util.HashMap;

public class RNGAMConfig {
    
    private HashMap GAM2Criteo;
    public HashMap getGAM2Criteo() {return GAM2Criteo;}
    public void setGAM2Criteo(HashMap GAM2Criteo) {this.GAM2Criteo = GAM2Criteo;}

    private static final RNGAMConfig config = new RNGAMConfig();
    public static RNGAMConfig getInstance() {return config;}
}