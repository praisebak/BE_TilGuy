package com.tilguys.matilda.common.dlq.domain;

public enum DLQEventStatus {
    FAILED,              // 처리 실패 (재시도 가능)
    RETRYING,            // 재시도 중
    RESOLVED,            // 해결됨 (재처리 성공)
    PERMANENTLY_FAILED   // 영구 실패 (재시도 불가)
}
