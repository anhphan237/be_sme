package com.sme.be_sme.modules.identity.invite.infrastructure.entity;

import java.util.Date;

public class UserInviteTokenEntity {
    private String inviteTokenId;
    private String userId;
    private String tokenHash;
    private Date expiresAt;
    private Date createdAt;
    private Date usedAt;

    public String getInviteTokenId() {
        return inviteTokenId;
    }

    public void setInviteTokenId(String inviteTokenId) {
        this.inviteTokenId = inviteTokenId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Date usedAt) {
        this.usedAt = usedAt;
    }
}
