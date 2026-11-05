import http from 'k6/http';
import { check, sleep } from 'k6';

// 부하 테스트 설정
export const options = {
    stages: [
        { duration: '10s', target: 500 },  // 10초 동안 유저 500명 증가
        { duration: '20s', target: 1000 }, // 이후 20초 동안 유저 1000명까지 증가
        { duration: '10s', target: 0 },    // 마지막 10초 동안 유저 퇴장 및 안정화
    ],
};

// 가상 유저 시나리오 로직
export default function () {
    // k6: 고유 스레드 번호와 고유 유저 ID 발급
    const userId = __VU * 10000 + __ITER; 
    const url = `http://localhost:8080/api/queue/status?userId=${userId}`;

    let isAllowed = false;
    let retryCount = 0;

    // 대기열을 통과하거나 최대 15번 재시도할 때까지 루프 작동
    while (!isAllowed && retryCount < 15) {
        const response = http.get(url);

        // API 응답 구조 검증
        check(response, {
            '200': (r) => response.status === 200,
        });

        if (response.status === 200) {
            const body = JSON.parse(response.body);

            if (body.status === 'ALLOWED') {
                console.log(`[PASS] 유저 ${userId}`);
                isAllowed = true;
            } else if (body.status === 'WAITING') {
                console.log(`[WAIT] 유저 ${userId} 대기 중... 대기 순번: ${body.waitingCount}번`);
            }
        }

        // 1초 간격으로 폴링 조회
        sleep(1);
        retryCount++;
    }
}
