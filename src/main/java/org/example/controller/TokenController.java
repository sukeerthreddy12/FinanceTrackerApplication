package org.example.controller;

import org.example.entities.RefreshToken;
import org.example.repository.RefreshTokenRepository;
import org.example.request.AuthRequestDTO;
import org.example.request.RefreshTokenRequestDTO;
import org.example.response.JwtResponseDTO;
import org.example.service.JwtService;
import org.example.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @PostMapping("auth/v1/login")
    public ResponseEntity<JwtResponseDTO> AuthenticateAndGetToken(@RequestBody AuthRequestDTO authRequestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequestDTO.getUsername(), authRequestDTO.getPassword()));

        if (authentication.isAuthenticated()) {
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authRequestDTO.getUsername());
            String accessToken = jwtService.generateToken(authRequestDTO.getUsername());
            refreshTokenRepository.save(refreshToken);

            return new ResponseEntity<>(JwtResponseDTO
                    .builder()
                    .username(authRequestDTO.getUsername())
                    .accessToken(accessToken)
                    .token(refreshToken.getToken())
                    .build(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(JwtResponseDTO.builder().build(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("auth/v1/refreshToken")
    public ResponseEntity<JwtResponseDTO> refreshToken(@RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        return refreshTokenService.findByToken(refreshTokenRequestDTO.getToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserInfo)
                .map(userInfo -> JwtResponseDTO.builder()
                        .accessToken(jwtService.generateToken(userInfo.getUsername()))
                        .token(refreshTokenRequestDTO.getToken())
                        .build()
                ).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JwtResponseDTO.builder()
                                .accessToken("")
                                .token("")
                                .build()));
    }

    @PostMapping("auth/v1/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        try {
            if (refreshTokenRequestDTO.getToken() == null || refreshTokenRequestDTO.getToken().isEmpty()) {
                return new ResponseEntity<>("Refresh token is required", HttpStatus.BAD_REQUEST);
            }
            refreshTokenService.deleteByToken(refreshTokenRequestDTO.getToken());
            return ResponseEntity.ok("Successfully logged out");
        } catch (Exception ex) {
            return new ResponseEntity<>("Error during logout", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
