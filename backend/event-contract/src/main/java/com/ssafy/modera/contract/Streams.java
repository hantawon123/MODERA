package com.ssafy.modera.contract;

/**
 * Redis Streams 이름 및 Consumer Group 이름 상수.
 * api-server, analysis-worker가 공유하는 유일한 접점.
 */
public final class Streams {

    public static final String IMAGE_ANALYSIS = "image-analysis";
    public static final String ANALYSIS_RESULT = "analysis-result";

    public static final String GROUP_ANALYSIS_WORKERS = "analysis-workers";
    public static final String GROUP_API_CONSUMERS = "api-consumers";

    private Streams() {
    }
}
