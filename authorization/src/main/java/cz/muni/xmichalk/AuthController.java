package cz.muni.xmichalk;

import cz.muni.xmichalk.dto.AccessRequestPayload;
import cz.muni.xmichalk.dto.AccessResponse;
import cz.muni.xmichalk.dto.OrgAccessRequestPayload;
import cz.muni.xmichalk.persistence.OrgBundleAccess;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthorizationService authorizationService;
    private final JwtDecoder jwtDecoder;

    public AuthController(AuthorizationService authService, JwtDecoder jwtDecoder) {
        this.authorizationService = authService;
        this.jwtDecoder = jwtDecoder;
    }

    @GetMapping("/helloWorld")
    public ResponseEntity<String> testHelloWorld() {
        return ResponseEntity.ok("HelloWorld");
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkAccess(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody AccessRequestPayload payload
    ) {
        System.out.println("Check request");
        String token = authHeader.replace("Bearer ", "");
        Jwt jwt = jwtDecoder.decode(token);
        String orgId = jwt.getClaimAsString("org_name");

        System.out.println("Org id: "+ orgId);
        
        OrgBundleAccess accessRights = authorizationService.getAccess(orgId, payload.bundleId());

        System.out.println("Sending check response");

        return ResponseEntity.ok(new AccessResponse(accessRights.hasTiAccess(), accessRights.hasDsiAccess()));
    }

    @PostMapping("/checkOrgAccess")
    public ResponseEntity<?> checkOrgAccess(@RequestBody OrgAccessRequestPayload payload
    ) {
        OrgBundleAccess accessRights = authorizationService.getAccess(payload.orgId(), payload.bundleId());

        return ResponseEntity.ok(new AccessResponse(accessRights.hasTiAccess(), accessRights.hasDsiAccess()));
    }

    
}


