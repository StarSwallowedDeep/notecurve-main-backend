package com.notecurve.map.controller;

import com.notecurve.map.dto.FlowMapResponse;
import com.notecurve.map.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @PostMapping("/analyze")
    public ResponseEntity<FlowMapResponse> analyzeProject(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.ok(FlowMapResponse.builder()
                    .nodes(new ArrayList<>())
                    .edges(new ArrayList<>())
                    .build());
        }
        // 컨트롤러 측의 추가적인 데이터 가공 없이 서비스 레이어로 위임
        FlowMapResponse response = mapService.analyzeMultipartFiles(files);
        return ResponseEntity.ok(response);
    }
}
