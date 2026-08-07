#!/usr/bin/env bash
set -euo pipefail

: "${GRAFANA_URL:?Set GRAFANA_URL, e.g. https://localhost:8443/grafana}"
: "${GRAFANA_USER:?Set GRAFANA_USER}"
: "${GRAFANA_PASSWORD:?Set GRAFANA_PASSWORD}"

PROM_UID="${PROM_UID:-cftz8vl4oa874e}"
LOKI_UID="${LOKI_UID:-aftzngll19a0wd}"

jq -n --arg prom "$PROM_UID" --arg loki "$LOKI_UID" '
  def thresholds($warn; $critical):
    {mode:"absolute",steps:[
      {color:"green",value:null},
      {color:"yellow",value:$warn},
      {color:"red",value:$critical}
    ]};
  def ts($id;$title;$desc;$expr;$unit;$warn;$critical;$x;$y;$w;$h):
    {
      id:$id,title:$title,description:$desc,type:"timeseries",
      datasource:{type:"prometheus",uid:$prom},
      gridPos:{x:$x,y:$y,w:$w,h:$h},
      fieldConfig:{defaults:{unit:$unit,thresholds:thresholds($warn;$critical),custom:{drawStyle:"line",lineWidth:2,fillOpacity:12,showPoints:"never"}},overrides:[]},
      options:{legend:{displayMode:"table",placement:"bottom",calcs:["lastNotNull","max","mean"]},tooltip:{mode:"multi",sort:"desc"}},
      targets:[{refId:"A",expr:$expr,legendFormat:"{{method}} {{uri}} {{job}} {{pool}} {{stream}} {{group}}",range:true}]
    };
  def stat($id;$title;$desc;$expr;$unit;$warn;$critical;$x;$y;$w):
    {
      id:$id,title:$title,description:$desc,type:"stat",
      datasource:{type:"prometheus",uid:$prom},gridPos:{x:$x,y:$y,w:$w,h:5},
      fieldConfig:{defaults:{unit:$unit,thresholds:thresholds($warn;$critical),color:{mode:"thresholds"}},overrides:[]},
      options:{colorMode:"background",graphMode:"area",justifyMode:"auto",reduceOptions:{calcs:["lastNotNull"],values:false}},
      targets:[{refId:"A",expr:$expr,legendFormat:"{{job}}",range:true}]
    };
  def row($id;$title;$y): {id:$id,title:$title,type:"row",collapsed:false,gridPos:{x:0,y:$y,w:24,h:1},panels:[]};

  {
    dashboard:{
      id:null,uid:"modera-perf",title:"Modera 성능 테스트",tags:["modera","performance","presentation"],
      description:"부하·과부하 테스트 판정용 대시보드. 각 패널 설명의 지속시간과 임계값을 함께 적용한다. 순간 스파이크만으로 병목을 단정하지 않는다.",
      timezone:"browser",schemaVersion:42,version:1,refresh:"5s",time:{from:"now-30m",to:"now"},
      timepicker:{refresh_intervals:["5s","10s","30s","1m","5m"]},
      panels:[
        row(100;"1. 합격 여부와 호스트 자원";0),
        stat(1;"서비스 UP";"모든 대상이 1이어야 한다. 0 또는 No data면 해당 서비스/수집기가 중단된 상태.";"min(up)";"short";0.99;0.5;0;1;6),
        stat(2;"호스트 CPU";"5분 이상 70% 이상이면 경고, 85% 이상이면 CPU 병목. 순간 스파이크는 병목으로 판정하지 않는다.";"100 - (avg(rate(node_cpu_seconds_total{mode=\"idle\"}[1m])) * 100)";"percent";70;85;6;1;6),
        stat(3;"호스트 메모리";"75% 이상 경고, 90% 이상 OOM 위험. 부하 제거 후에도 계속 상승하면 누수 의심.";"100-(node_memory_MemAvailable_bytes/node_memory_MemTotal_bytes*100)";"percent";75;90;12;1;6),
        stat(4;"루트 디스크";"80% 이상 정리 필요, 90% 이상 위험. 이미지 영구 보관 정책 때문에 장기 추세도 확인.";"100-(node_filesystem_avail_bytes{mountpoint=\"/\",fstype=\"ext4\"}/node_filesystem_size_bytes{mountpoint=\"/\",fstype=\"ext4\"}*100)";"percent";80;90;18;1;6),

        row(101;"2. API 처리량·지연·오류";6),
        ts(10;"API별 RPS";"요청이 실제 목표 RPS까지 올라가는지 확인. k6 목표보다 낮으면 부하 발생기 VU 부족 또는 서버 포화 가능.";"sum by(method,uri,status)(rate(http_server_requests_seconds_count{job=\"modera-api\",uri!~\"/actuator.*\"}[1m]))";"reqps";1000;2000;0;7;12;9),
        ts(11;"API별 p95";"핵심 합격 기준: 각 API p95 300ms 이하. 300~500ms 경고, 500ms 초과 병목. 최소 3분 유지 구간으로 판정.";"histogram_quantile(0.95,sum by(le,method,uri)(rate(http_server_requests_seconds_bucket{job=\"modera-api\",uri!~\"/actuator.*\"}[1m])))";"s";0.3;0.5;12;7;12;9),
        ts(12;"API별 p99";"꼬리 지연 확인. 1초 초과가 반복되면 락·GC·DB 대기·외부 호출을 함께 확인.";"histogram_quantile(0.99,sum by(le,method,uri)(rate(http_server_requests_seconds_bucket{job=\"modera-api\",uri!~\"/actuator.*\"}[1m])))";"s";0.5;1;0;16;12;9),
        ts(13;"API 오류율";"API별 오류 비율. 1% 미만 정상, 1~5% 경고, 5% 초과 실패. 409 polling 등 기대 오류는 테스트 결과와 분리해서 해석.";"100*sum by(method,uri)(rate(http_server_requests_seconds_count{job=\"modera-api\",outcome!=\"SUCCESS\",uri!~\"/actuator.*\"}[1m]))/clamp_min(sum by(method,uri)(rate(http_server_requests_seconds_count{job=\"modera-api\",uri!~\"/actuator.*\"}[1m])),0.001)";"percent";1;5;12;16;12;9),
        ts(14;"API 동시 처리 요청";"RPS 증가와 함께 상승 후 안정되면 정상. 계속 증가하면서 지연도 상승하면 처리율보다 유입률이 커진 포화 상태.";"sum by(method,uri)(http_server_requests_active_seconds_count{job=\"modera-api\",uri!~\"/actuator.*\"})";"short";50;100;0;25;12;8),
        ts(15;"5XX 비율";"정상은 0%. 한 건이라도 원인 로그를 확인하며, 1% 이상이면 해당 부하 단계 실패.";"100*sum(rate(http_server_requests_seconds_count{job=\"modera-api\",outcome=\"SERVER_ERROR\"}[1m]))/clamp_min(sum(rate(http_server_requests_seconds_count{job=\"modera-api\"}[1m])),0.001)";"percent";0.01;1;12;25;12;8),

        row(102;"3. DB 커넥션 풀";33),
        ts(20;"Hikari 풀 사용률";"80% 이상 경고, 95% 이상 포화. p95 상승과 동시에 포화되면 DB/풀 병목.";"100*max by(pool)(hikaricp_connections_active{job=\"modera-api\"})/clamp_min(max by(pool)(hikaricp_connections_max{job=\"modera-api\"}),1)";"percent";80;95;0;34;8;8),
        ts(21;"Hikari 대기 요청";"정상은 0. 1 이상이 지속되면 커넥션을 기다리는 중이며 API 지연의 직접 원인.";"max by(pool)(hikaricp_connections_pending{job=\"modera-api\"})";"short";1;5;8;34;8;8),
        ts(22;"커넥션 획득 최악시간";"50ms 이상 경고, 200ms 이상 병목. 풀 사용률·DB CPU·슬로우 쿼리와 함께 판단.";"max by(pool)(hikaricp_connections_acquire_seconds_max{job=\"modera-api\"})";"s";0.05;0.2;16;34;8;8),
        ts(23;"커넥션 타임아웃 증가율";"항상 0이어야 한다. 0 초과 시 풀 고갈로 해당 부하 단계 실패.";"sum by(pool)(rate(hikaricp_connections_timeout_total{job=\"modera-api\"}[1m]))";"ops";0.001;0.01;0;42;24;7),

        row(103;"4. JVM·GC·프로세스";49),
        ts(30;"JVM Heap 사용률";"70% 이상 경고, 85% 이상 위험. GC 후에도 기준선으로 돌아오지 않고 반복 상승하면 누수 의심.";"100*sum by(job)(jvm_memory_used_bytes{area=\"heap\"})/clamp_min(sum by(job)(jvm_memory_max_bytes{area=\"heap\"}),1)";"percent";70;85;0;50;8;8),
        ts(31;"GC pause 최댓값";"100ms 이상 경고, 500ms 이상 사용자 지연에 직접 영향. API p99 스파이크 시점과 대조.";"max by(job)(jvm_gc_pause_seconds_max)";"s";0.1;0.5;8;50;8;8),
        ts(32;"프로세스 CPU";"API/Worker별 CPU 비율. 70% 이상 지속 시 CPU 병목 후보. 호스트 CPU와 함께 판정.";"100*process_cpu_usage{job=~\"modera-api|modera-worker\"}";"percent";70;90;16;50;8;8),

        row(104;"5. Redis 비동기 파이프라인";58),
        ts(40;"Consumer lag";"정상은 0 근처. 지속 상승하면 Worker/API 소비 속도가 생산 속도보다 느린 실제 큐 병목.";"redis_stream_group_lag";"short";10;100;0;59;12;8),
        ts(41;"PEL pending";"소비됐지만 ACK되지 않은 메시지. 0~1 정상, 10 이상 지속 시 처리 실패·정지·재처리 병목.";"redis_stream_group_messages_pending";"short";1;10;12;59;12;8),
        ts(42;"Stream 유입률";"스트림별 초당 이벤트 생성량. lag와 함께 보면 생산/소비 불균형을 판단할 수 있다.";"sum by(stream)(rate(redis_stream_entries_added_total[1m]))";"ops";20;100;0;67;12;8),
        ts(43;"AI 관측 가능 상태";"현재 AI /metrics가 404이면 0이다. 1로 복구되기 전까지 AI 내부 병목은 Worker 계측과 로그로 판정한다.";"up{job=\"ai-service\"}";"short";0.99;0.5;12;67;12;8),
        ts(44;"분석 파이프라인 p95";"Worker 큐 등록부터 콜백 저장까지. 30초 이상 경고, 90초 이상 외부 AI timeout/재시도 병목. status=EMPTY/FAILED 비율도 함께 확인.";"histogram_quantile(0.95,sum by(le,status)(rate(modera_analysis_pipeline_duration_seconds_bucket[5m])))";"s";30;90;0;75;12;8),
        ts(45;"AI 단계 p95";"Worker가 AI 요청을 보낸 뒤 콜백을 저장할 때까지. 전체 파이프라인과 거의 같으면 AI/외부 모델 병목, 차이가 크면 Redis/Worker 대기 병목.";"histogram_quantile(0.95,sum by(le,status)(rate(modera_analysis_ai_duration_seconds_bucket[5m])))";"s";20;90;12;75;12;8),

        row(105;"6. 오류·병목 로그";83),
        {id:50,title:"ERROR/WARNING 로그",description:"성능 테스트 시간대의 timeout, retry, OOM, connection pool, DNS, GMS 오류를 확인. WARN도 포함해 외부 AI 지연을 놓치지 않는다.",type:"logs",datasource:{type:"loki",uid:$loki},gridPos:{x:0,y:84,w:24,h:14},options:{showTime:true,wrapLogMessage:true,sortOrder:"Descending",dedupStrategy:"none"},targets:[{refId:"A",expr:"{container=~\"modera-.*|ai-ai-service-1\"} |~ \"(?i)(error|warn|exception|timeout|retry|oom|connection)\"",queryType:"range",direction:"backward"}]}
      ]
    },
    overwrite:true,message:"Create presentation-ready performance dashboard"
  }
' > /tmp/modera-performance-dashboard.json

curl -ksS -u "$GRAFANA_USER:$GRAFANA_PASSWORD" \
  -H 'Content-Type: application/json' \
  -X POST "$GRAFANA_URL/api/dashboards/db" \
  --data-binary @/tmp/modera-performance-dashboard.json
