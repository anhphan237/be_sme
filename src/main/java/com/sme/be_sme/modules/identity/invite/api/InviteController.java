package com.sme.be_sme.modules.identity.invite.api;

import com.sme.be_sme.modules.identity.invite.InviteSetPasswordService;
import com.sme.be_sme.shared.api.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints for invite flow (no JWT required).
 */
@RestController
@RequestMapping("/api/v1/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteSetPasswordService inviteSetPasswordService;

    @PostMapping("/set-password")
    public BaseResponse<Void> setPassword(@Valid @RequestBody InviteSetPasswordRequest request) {
        inviteSetPasswordService.setPassword(request.getToken(), request.getPassword());
        return BaseResponse.success(null, null);
    }
}
