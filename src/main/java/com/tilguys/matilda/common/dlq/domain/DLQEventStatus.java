package com.tilguys.matilda.common.dlq.domain;

public enum DLQEventStatus {
    FAILED,              // 처리 실패 (격리됨)
    RESOLVED,            // 해결됨 (수동 처리 완료)
    PERMANENTLY_FAILED   // 영구 실패 (재시도 불가)
}
