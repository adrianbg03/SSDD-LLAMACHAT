package es.um.sisdist.backend.dao.models;

import java.sql.Timestamp;

public class Conversation {
    private String dialogueId;
    private String userId;
    private String dname;
    private String status; // 'READY', 'BUSY', 'FINISHED'
    private String dialogue; // JSON o String según tu uso
    private Timestamp createdAt;

    public Conversation() {
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public void setDialogueId(String dialogueId) {
        this.dialogueId = dialogueId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDname() {
        return dname;
    }

    public void setDname(String dname) {
        this.dname = dname;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDialogue() {
        return dialogue;
    }

    public void setDialogue(String dialogue) {
        this.dialogue = dialogue;
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt.toString() : null;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}