package com.ysd.iep.feign;

import com.ysd.iep.entity.dto.CourceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author 80795
 * @date 2018/11/12 8:55
 */
@FeignClient("IEP-TEACKER")
public interface TeacherFeign {
    /**
     * 根据课程id查询课程
     * @param courId
     * @return
     */
    @GetMapping("/course/queryCourByteaId")
    List<CourceDTO> findCourseById(@RequestParam("courId") String courId);

    /**
     * 课程分页
     * @param page
     * @param pageSize
     * @param courName
     * @return
     */
    @GetMapping("/course/getPaginate")
    Page<CourceDTO> getPaginate(@RequestParam("page") int page,
                                @RequestParam("pageSize") int pageSize,
                                @RequestParam("courName") String courName);
}
