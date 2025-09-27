package com.el.cmr.nusaindah.ui.my_home.page.model;

public class ImageModel {

    private int mod_id;
    private String image_url;

    public ImageModel(int mod_id, String image_url){
        this.mod_id = mod_id;
        this.image_url = image_url;
    }

    public int getMod_id() {
        return mod_id;
    }

    public void setMod_id(int mod_id) {
        this.mod_id = mod_id;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }
}
