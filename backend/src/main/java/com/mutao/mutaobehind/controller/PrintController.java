package com.mutao.mutaobehind.controller;

import com.mutao.mutaobehind.dto.PrintReportRequest;
import com.mutao.mutaobehind.service.PdfReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF 报告打印接口
 * 接收测评结果 JSON + 填写人信息，返回 PDF 文件流
 */
@RestController
public class PrintController {

    @Autowired
    private PdfReportService pdfReportService;

    @PostMapping("/api/print/report")
    public ResponseEntity<?> printReport(@RequestBody PrintReportRequest request) {
        try {
            byte[] pdfBytes = pdfReportService.generateReport(request);

            ContentDisposition disposition = ContentDisposition
                    .inline()
                    .filename("report.pdf", StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "PDF 生成失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
