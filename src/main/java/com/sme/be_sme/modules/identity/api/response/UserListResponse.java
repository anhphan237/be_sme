package com.sme.be_sme.modules.identity.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserListResponse {
    private List<UserListItem> users;
}
