package com.attuned.main;

import java.util.ArrayList;

public class Artist {
    String name;
    ArrayList<Album> albums = new ArrayList<Album>();
    public Artist (String name) {
        this.name = name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Artist) {
            if (((Artist) obj).name.equalsIgnoreCase(this.name)) {
                return true;
            }
        }
        return false;
    }
}
