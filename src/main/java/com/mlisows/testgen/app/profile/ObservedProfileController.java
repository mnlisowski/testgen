package com.mlisows.testgen.app.profile;

import com.mlisows.testgen.usecase.profile.StoredObservedProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

@RestController
public class ObservedProfileController {
    private final ObservedProfileService profileService;

    public ObservedProfileController(ObservedProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping(value = "/api/projects/{projectId}/profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ObservedProfileResponse> upload(
            @PathVariable String projectId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String text = decodeUtf8(file.getBytes());
        StoredObservedProfile profile = profileService.importProfile(projectId, text);
        return ResponseEntity.status(HttpStatus.CREATED).body(ObservedProfileResponse.from(profile));
    }

    private String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new InvalidObservedProfileException("Profile must be encoded as UTF-8");
        }
    }
}
