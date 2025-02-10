package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);
        try {
            String originalfilename = file.getOriginalFilename();
            String extension = originalfilename.substring(originalfilename.lastIndexOf("."));
            String filepath = aliOssUtil.upload(file.getBytes(), UUID.randomUUID().toString() + extension);
            return Result.success(filepath);
        } catch (IOException e) {
            log.error("文件上传失败:{}", e.getMessage());
        }
        return Result.error("文件上传失败");
    }

}
