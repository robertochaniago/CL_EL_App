package com.el.cmr.nusaindah.ui.my_home.page.model;

public class MoreModel {

    private int mod_id;
    private String title, image_url, download_url, name;

    public MoreModel(int mod_id, String title, String image_url, String download_url, String name){
        this.mod_id = mod_id;
        this.title = title;
        this.image_url = image_url;
        this.download_url = download_url;
        this.name = name;
    }

    public int getMod_id() {
        return mod_id;
    }

    public void setMod_id(int mod_id) {
        this.mod_id = mod_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
