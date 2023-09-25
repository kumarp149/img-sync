package com.sruteesh.imgSync.googledriveapi.entities;

import java.util.List;

public class GDriveEntity {
    private String mimeType;
    private String id;
    private String name;
    private String sha256Checksum;
    private List<String> parents;
    private String webContentLink;
    private long size;
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSha256Checksum() {
        return sha256Checksum;
    }
    public void setSha256Checksum(String sha256Checksum) {
        this.sha256Checksum = sha256Checksum;
    }
    public List<String> getParents() {
        return parents;
    }
    public void setParents(List<String> parents) {
        this.parents = parents;
    }
    public String getWebContentLink() {
        return webContentLink;
    }
    public void setWebContentLink(String webContentLink) {
        this.webContentLink = webContentLink;
    }
    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }

}
