package com.notecurve.image.controller;

import java.io.IOException;
import java.util.logging.Logger;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.notecurve.image.service.ImageServeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageServeController {

    private static final Logger LOGGER =
            Logger.getLogger(ImageServeController.class.getName());

    private final ImageServeService imageServeService;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            // 이미지 파일 제공
            Resource resource = imageServeService.serveImage(filename);

            // MIME 타입 결정
            String contentType =
                    imageServeService.determineContentType(resource.getFile().toPath());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (IOException | IllegalArgumentException e) {
            LOGGER.severe("Error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/profiles/{filename:.+}")
    public ResponseEntity<Resource> serveProfileImage(@PathVariable String filename) {
        try {
            Resource resource = imageServeService.serveProfileImage(filename);
            String contentType = imageServeService.determineContentType(resource.getFile().toPath());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.severe("Error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
