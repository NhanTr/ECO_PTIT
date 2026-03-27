package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.AuthenticationRequest;
import com.example.manage_activities.dto.request.IntrospectRequest;
import com.example.manage_activities.dto.response.AuthenticationResponse;
import com.example.manage_activities.dto.response.IntrospectResponse;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")  
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/token")
    APIResponse<AuthenticationResponse>  authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse result = authenticationService.authenticate(request); 
        return APIResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    APIResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        IntrospectResponse result = authenticationService.introspect(request); 
        return APIResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }
    
}
