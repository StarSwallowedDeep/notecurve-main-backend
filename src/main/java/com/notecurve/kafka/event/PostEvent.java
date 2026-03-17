package com.notecurve.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostEvent {
    private String type;
    private Long postId;
    private String title;
    private String userName;
    private LocalDate createdAt;
}
