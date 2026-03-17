package com.notecurve.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String type;
    private Long userId;
    private String loginId;
    private String name;
    private String role;
}
