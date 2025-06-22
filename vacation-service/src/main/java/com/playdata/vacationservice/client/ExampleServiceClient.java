package com.playdata.vacationservice.client;


import org.springframework.cloud.openfeign.FeignClient;

//여기에 요청보낼 타 서비스 이름을 쓰세요. 쿠버내티스는 url 까지 써서 지정해야한데유
@FeignClient(name = "example-service")
public interface ExampleServiceClient {

    // 참고하게 남겨놔유
    // 아래는 클라이언트 요청보낼때의 다양한 유형들이유
//    // 상품 ID로 상품 정보를 조회하는 메서드
//    @GetMapping("/product/{prodId}")
//    CommonResDto<ProductResDto> findById(@PathVariable Long prodId);
//
//    // 상품 수량 업데이트
//    @PutMapping("/product/updateQuantity")
//    ResponseEntity<?> updateQuantity(@RequestBody ProductResDto productResDto);
//
//    // 여러 상품을 한 번에 조회하는 메서드
//    @PostMapping("/product/products")
//    CommonResDto<List<ProductResDto>> getProducts(@RequestBody List<Long> productIds);
//
//    // 상품 취소 처리
//    @PutMapping("/product/cancel")
//    ResponseEntity<?> cancelProduct(@RequestBody Map<Long, Integer> map);
//
//    // 상품 정보를 가져오는 메서드 (단일 상품 조회)
//    @GetMapping("/product/{productId}")
//    ProductResDto getProductById(@PathVariable Long productId);

}
