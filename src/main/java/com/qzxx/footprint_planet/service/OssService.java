package com.qzxx.footprint_planet.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.qzxx.footprint_planet.config.OssProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class OssService {

    private final OssProperties props;

    public OssService(OssProperties props) {
        this.props = props;
    }

    public String upload(MultipartFile file) {
        String key = "uploads/"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + extractExtension(file.getOriginalFilename());

        OSS client = new OSSClientBuilder().build(
                props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
        try (InputStream in = file.getInputStream()) {
            client.putObject(props.getBucketName(), key, in);
        } catch (IOException e) {
            throw new RuntimeException("图片上传失败", e);
        } finally {
            client.shutdown();
        }
        return props.getUrlPrefix() + "/" + key;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".png";
        }
        String ext = filename.substring(filename.lastIndexOf('.'));
        if (!ext.matches("(?i)\\.(jpg|jpeg|png|gif|webp|bmp)")) {
            return ".png";
        }
        return ext;
    }
}
