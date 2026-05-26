package com.my.pharmacy.service;

import com.my.pharmacy.dto.DocumentDto;
import com.my.pharmacy.dto.KakaoApiResponseDto;
import com.my.pharmacy.dto.OutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KakaoCategorySearchService {
    private final RestTemplate restTemplate;

    @Value("${kakao.rest.api.key}")
    private String kakaoRestApiKey;

    private static final String KAKAO_CATEGORY_URL = "https://dapi.kakao.com/v2/local/search/category";
    // 카테고리 상수(약국)
//    private static final String CATEGORY = "PM9";
    // 카테고리 상수(카페)
    private static final String CATEGORY = "CE7";
    // 위도 : latitude, 경도 : longitude를 인자로 받아서 카테고리 검색
    public KakaoApiResponseDto resultCategorySearch(
            double latitude, double longitude) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(KAKAO_CATEGORY_URL);
        // 1. 카테고리
        uriBuilder.queryParam("category_group_code", CATEGORY);
        // 2. x 값, y 값
        uriBuilder.queryParam("x", longitude);
        uriBuilder.queryParam("y", latitude);
        // 3. 검색 반경
        uriBuilder.queryParam("radius", 1000);
        // 4. 검색 사이즈 - 나중에 처리
        // 5. 정렬처리
        uriBuilder.queryParam("sort", "distance");

        // url 에 포함된 한글을 UTF-8 인코딩 처리
        URI uri = uriBuilder.build().encode().toUri();
        // 헤더 작업
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey);
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

        // 카카오 api 호출
        return restTemplate
                .exchange(
                        uri,
                        HttpMethod.GET,
                        httpEntity,
                        KakaoApiResponseDto.class
                ).getBody();
    }

    // documentList를 받아서 OutputDto의 리스트로 변환
    // 각 documentList 안에있는 DocumentDto -> OutputDto 변환 후
    // 다시 OutputDto 리스트에 저장
    public List<OutputDto> makeOutputDto(
            List<DocumentDto> documentList
    ) {
        // 전체 15개의 리스트가 들어온다... 그 중 5개 출력
        return documentList
                .stream()
                .map(x -> convertToOutputDto(x))
                .limit(5)
                .toList();
    }


    // 각각의 DocumentDto를 꺼내서 OutputDto 변환
    private OutputDto convertToOutputDto(DocumentDto document){
        // 길찾기 URL을 변수
        String DIRECTION_URL = "https://map.kakao.com/link/map/";
        // 로드뷰 URL을 변수
        String ROAD_VIEW_URL = "https://map.kakao.com/link/roadview/";
        String params = String.join(",", document.getPlaceName(),
                String.valueOf(document.getLatitude()),
                String.valueOf(document.getLongitude())
                );
        String mapUrl = UriComponentsBuilder
                .fromUriString(DIRECTION_URL + params)
                .toUriString();

        String roadUrl = ROAD_VIEW_URL + document.getLatitude() + "," + document.getLongitude();
        return OutputDto.builder()
                .pharmacyName(document.getPlaceName())
                .pharmacyAddress(document.getAddressName())
                .pharmacyPhone(document.getPhone())
                .distance(document.getDistance())
                .directionURL(mapUrl)
                .roadViewURL(roadUrl)
                .build();
    }
}
