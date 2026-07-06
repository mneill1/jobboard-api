package com.jobboard.api.controller;

import com.jobboard.api.dto.ApplicationResponse;
import com.jobboard.api.dto.JobResponse;
import com.jobboard.api.dto.UpdateProfileRequest;
import com.jobboard.api.dto.UserProfileResponse;
import com.jobboard.api.entity.User;
import com.jobboard.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser));
    }

    @GetMapping("/me/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplications(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getApplications(currentUser));
    }

    @GetMapping("/me/jobs")
    public ResponseEntity<List<JobResponse>> getJobs(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getJobs(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(currentUser, req));
    }
}
