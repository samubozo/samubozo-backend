package com.playdata.payrollservice.client;

import com.playdata.payrollservice.common.dto.CommonResDto;
import com.playdata.payrollservice.payroll.dto.UserResDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "hr-service")
public interface HrClient {

    @GetMapping("/hr/user/{id}")
    CommonResDto<UserResDto> getUserById(@PathVariable("id") Long employeeNo);

    @GetMapping("/hr/user/list")
    CommonResDto<PageWrapper<UserResDto>> getUserList(@RequestParam("page") int page,
                                                          @RequestParam("size") int size);


    @Getter
    @Setter
    public class PageWrapper<T> {
        private List<T> content;
        private int totalPages;
        private long totalElements;
        private int number;
    }

}
